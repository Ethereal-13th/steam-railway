package dev.steamrailway.rail;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class RailPathGenerator {
	public static final double MIN_PATH_DISTANCE = 4.0D;
	public static final double MAX_PATH_DISTANCE = 128.0D;
	private static final double PATH_EPSILON = 1.0E-6D;
	private static final double HANDLE_SCALE = 0.35D;
	private static final double MIN_HANDLE_LENGTH = 2.0D;
	private static final double MAX_HANDLE_LENGTH = 16.0D;
	private static final double GENTLE_HANDLE_SCALE = 0.6D;
	private static final double GENTLE_MAX_HANDLE_LENGTH = 24.0D;
	private static final double CREATE_NINETY_MIN_TURN_SIZE = 7.0D;
	private static final double CREATE_DIAGONAL_MIN_TURN_SIZE = 3.25D;
	private static final double SHORT_DIAGONAL_ALLOWANCE = 6.0D;
	private static final double SAMPLE_STEP = 0.5D;

	private RailPathGenerator() {
	}

	public static RailEdge createEdge(String edgeId, RailNode fromNode, Direction fromFacing, RailNode toNode, Direction toFacing) {
		List<RailSample> samples = generateSamples(fromNode.worldPos(), fromFacing, toNode.worldPos(), toFacing, HANDLE_SCALE, MAX_HANDLE_LENGTH);
		double traveled = samples.isEmpty() ? 0.0D : samples.getLast().distance();
		return new RailEdge(edgeId, fromNode.id(), toNode.id(), List.copyOf(samples), traveled);
	}

	public static RailEdge createStraightEdge(String edgeId, RailNode fromNode, RailNode toNode, Direction facing) {
		List<RailSample> samples = sampleStraightLine(fromNode.worldPos(), toNode.worldPos(), facing);
		double traveled = samples.isEmpty() ? 0.0D : samples.getLast().distance();
		return new RailEdge(edgeId, fromNode.id(), toNode.id(), List.copyOf(samples), traveled);
	}

	public static RailEdge createEdgeFromSamples(String edgeId, RailNode fromNode, RailNode toNode, List<RailSample> samples) {
		double traveled = samples.isEmpty() ? 0.0D : samples.getLast().distance();
		return new RailEdge(edgeId, fromNode.id(), toNode.id(), List.copyOf(samples), traveled);
	}

	public static ToolPath createToolPath(
		BlockPos startPos,
		Direction startFacing,
		boolean startFacingLocked,
		BlockPos endPos,
		Direction endFacing,
		boolean endFacingLocked,
		boolean gentle
	) {
		Vec3d start = RailGeometry.worldPosition(startPos);
		Vec3d end = RailGeometry.worldPosition(endPos);
		Direction straightDirection = RailPlacementTargetResolver.resolveStraightDirection(
			startPos,
			startFacing,
			startFacingLocked,
			endPos,
			endFacing,
			endFacingLocked);
		if (straightDirection != null) {
			List<RailSample> samples = List.copyOf(sampleStraightLine(start, end, straightDirection));
			return new ToolPath(samples, List.of(), true, ToolPathKind.STRAIGHT, straightDirection, straightDirection, 0, 0);
		}

		List<Direction> startCandidates = axisCandidates(startFacing);
		List<Direction> endCandidates = endFacingLocked ? axisCandidates(endFacing) : new ArrayList<>();
		if (!endFacingLocked) {
			for (Direction startCandidate : startCandidates) {
				appendUnique(endCandidates, previewCandidates(startCandidate));
			}
		}

		return selectBestToolPath(start, startCandidates, end, endCandidates, gentle, startFacing, endFacing);
	}

	private static ToolPath selectBestToolPath(
		Vec3d start,
		List<Direction> startCandidates,
		Vec3d end,
		List<Direction> endCandidates,
		boolean gentle,
		Direction fallbackStartFacing,
		Direction fallbackEndFacing
	) {
		ToolPathCandidate bestValid = null;
		ToolPathCandidate bestInvalid = null;

		for (Direction startCandidate : startCandidates) {
			for (Direction endCandidate : endCandidates) {
				ToolPathCandidate candidate = buildToolPathCandidate(
					start,
					startCandidate,
					end,
					endCandidate,
					gentle,
					fallbackStartFacing,
					fallbackEndFacing);
				if (candidate == null) {
					continue;
				}

				if (candidate.path().valid()) {
					if (bestValid == null || candidate.score() < bestValid.score()) {
						bestValid = candidate;
					}
				} else if (bestInvalid == null || candidate.score() < bestInvalid.score()) {
					bestInvalid = candidate;
				}
			}
		}

		if (bestValid != null) {
			return bestValid.path();
		}
		if (bestInvalid != null) {
			return bestInvalid.path();
		}
		return new ToolPath(List.of(), List.of(), false, ToolPathKind.NONE, fallbackStartFacing, fallbackEndFacing, 0, 0);
	}

	private static ToolPathCandidate buildToolPathCandidate(
		Vec3d start,
		Direction fromFacing,
		Vec3d end,
		Direction toFacing,
		boolean gentle,
		Direction fallbackStartFacing,
		Direction fallbackEndFacing
	) {
		if (!fromFacing.getAxis().isHorizontal() || !toFacing.getAxis().isHorizontal()) {
			return null;
		}

		PathCandidateSolution solution = resolveCandidateSolution(start, fromFacing, end, toFacing, gentle);
		if (solution.samples().size() < 2) {
			return null;
		}

		Direction resolvedStartFacing = RailGeometry.cardinalFacing(solution.samples().getFirst().tangent(), fallbackStartFacing);
		Direction resolvedEndFacing = RailGeometry.cardinalFacing(solution.samples().getLast().tangent(), fallbackEndFacing);
		boolean valid = isCreateLikeValid(start, fromFacing, end, toFacing, gentle);
		List<RailSample> renderSamples = solution.straight()
			? List.of()
			: extractRenderableCurveSamples(solution.samples(), solution.startExtent(), solution.endExtent());
		ToolPathKind kind = solution.straight() || renderSamples.size() < 2 ? ToolPathKind.STRAIGHT : ToolPathKind.CURVE;
		ToolPath path = new ToolPath(
			List.copyOf(solution.samples()),
			List.copyOf(renderSamples),
			valid,
			kind,
			resolvedStartFacing,
			resolvedEndFacing,
			solution.startExtent(),
			solution.endExtent());
		return new ToolPathCandidate(path, solution.score());
	}

	private static PathCandidateSolution resolveCandidateSolution(
		Vec3d start,
		Direction fromFacing,
		Vec3d end,
		Direction toFacing,
		boolean gentle
	) {
		PreviewConnectionSolution solution = solveCreateLikePreview(start, fromFacing, end, toFacing, gentle);
		if (solution != null) {
			return new PathCandidateSolution(
				solution.samples(),
				prioritizedCurveScore(solution),
				solution.startExtent(),
				solution.endExtent(),
				solution.straight());
		}

		double handleScale = gentle ? GENTLE_HANDLE_SCALE : HANDLE_SCALE;
		double maxHandleLength = gentle ? GENTLE_MAX_HANDLE_LENGTH : MAX_HANDLE_LENGTH;
		List<RailSample> samples = generateSamples(start, fromFacing, end, toFacing, handleScale, maxHandleLength);
		double score = samples.isEmpty() ? Double.MAX_VALUE : samples.getLast().distance();
		return new PathCandidateSolution(samples, score, 0, 0, false);
	}

	private static boolean isCreateLikeValid(Vec3d start, Direction fromFacing, Vec3d end, Direction toFacing, boolean gentle) {
		if (!fromFacing.getAxis().isHorizontal() || !toFacing.getAxis().isHorizontal()) {
			return false;
		}

		Vec3d startDir = unitVector(fromFacing);
		Vec3d endDir = unitVector(toFacing);
		Vec3d delta = end.subtract(start);
		double dot = horizontalDot(startDir, endDir);
		if (dot > 0.999D) {
			double lateralOffset = Math.abs(horizontalCross(delta, startDir));
			double forwardDistance = horizontalDot(delta, startDir);
			if (forwardDistance < 0.75D) {
				return false;
			}
			if (lateralOffset <= 0.25D) {
				return forwardDistance >= MIN_PATH_DISTANCE;
			}

			double directDistance = start.distanceTo(end);
			if (directDistance <= SHORT_DIAGONAL_ALLOWANCE) {
				return true;
			}

			return forwardDistance >= minimumParallelForwardDistance(lateralOffset);
		}

		if (dot < -0.999D) {
			return false;
		}

		double directDistance = start.distanceTo(end);
		if (directDistance <= SHORT_DIAGONAL_ALLOWANCE) {
			return allowsShortDiagonal(delta, startDir, endDir);
		}

		double[] intersection = intersectXZ(start, end, startDir, endDir.multiply(-1.0D));
		if (intersection == null) {
			return false;
		}

		double startLead = intersection[0];
		double endLead = intersection[1];
		double absAngle = Math.abs(horizontalAngleDegrees(startDir, endDir));
		boolean ninety = (absAngle + 0.25D) % 90.0D < 1.0D;
		double minTurnSize = ninety ? CREATE_NINETY_MIN_TURN_SIZE : CREATE_DIAGONAL_MIN_TURN_SIZE;
		return startLead >= minTurnSize && endLead >= minTurnSize;
	}

	private static PreviewConnectionSolution solveCreateLikePreview(Vec3d start, Direction fromFacing, Vec3d end, Direction toFacing, boolean gentle) {
		if (!fromFacing.getAxis().isHorizontal() || !toFacing.getAxis().isHorizontal()) {
			return null;
		}

		Vec3d axis1 = unitVector(fromFacing);
		Vec3d axis2 = unitVector(toFacing);
		Vec3d delta = end.subtract(start);

		if (horizontalDot(axis1, delta) < 0.0D) {
			axis1 = axis1.multiply(-1.0D);
		}

		double[] intersect = intersectXZ(start, end, axis1, axis2);
		boolean parallel = intersect == null;
		if ((parallel && horizontalDot(axis1, axis2) > 0.0D) || (!parallel && (intersect[0] < 0.0D || intersect[1] < 0.0D))) {
			axis2 = axis2.multiply(-1.0D);
		}

		Vec3d cross2 = rightNormal(axis2);
		double a1 = Math.atan2(axis2.z, axis2.x);
		double a2 = Math.atan2(axis1.z, axis1.x);
		double angle = a1 - a2;
		double end1Extent = 0.0D;
		double end2Extent = 0.0D;

		if (parallel) {
			double[] sTest = intersectXZ(start, end, axis1, cross2);
			if (sTest == null) {
				return null;
			}

			double t = Math.abs(sTest[0]);
			double u = Math.abs(sTest[1]);
			boolean straight = u <= PATH_EPSILON;

			if (!straight && sTest[0] < 0.0D) {
				return null;
			}

			if (straight) {
				List<RailSample> straightSamples = sampleLineWithDistance(start, end);
				return straightSamples.size() < 2
					? null
					: new PreviewConnectionSolution(List.copyOf(straightSamples), straightSamples.getLast().distance(), 0.0D, 0, 0, true);
			}

			double targetT = minimumParallelForwardDistance(u);
			if (t < targetT) {
				return null;
			}

			if (!gentle && t > targetT) {
				int correction = (int) ((t - targetT) / axis1.length());
				end1Extent = correction / 2.0D + (correction % 2);
				end2Extent = correction / 2.0D;
			}
		} else {
			double absAngle = Math.abs(Math.toDegrees(angle));
			if (absAngle < 60.0D || absAngle > 300.0D) {
				return null;
			}

			intersect = intersectXZ(start, end, axis1, axis2);
			if (intersect == null || intersect[0] < 0.0D || intersect[1] < 0.0D) {
				return null;
			}

			double dist1 = Math.abs(intersect[0]);
			double dist2 = Math.abs(intersect[1]);
			double ex1 = 0.0D;
			double ex2 = 0.0D;

			if (dist1 > dist2) {
				ex1 = (dist1 - dist2) / axis1.length();
			}
			if (dist2 > dist1) {
				ex2 = (dist2 - dist1) / axis2.length();
			}

			double turnSize = Math.min(dist1, dist2) - 0.1D;
			boolean ninety = (absAngle + 0.25D) % 90.0D < 1.0D;
			double minTurnSize = ninety ? CREATE_NINETY_MIN_TURN_SIZE : CREATE_DIAGONAL_MIN_TURN_SIZE;
			if (turnSize < minTurnSize) {
				return null;
			}

			if (!gentle) {
				ex1 += (turnSize - minTurnSize) / axis1.length();
				ex2 += (turnSize - minTurnSize) / axis2.length();
			}

			end1Extent = Math.floor(ex1);
			end2Extent = Math.floor(ex2);
		}

		Vec3d bezierStart = start.add(axis1.multiply(end1Extent));
		Vec3d bezierEnd = end.add(axis2.multiply(end2Extent));
		double handleLength = determineCreateHandleLength(bezierStart, bezierEnd, axis1, axis2);
		List<RailSample> samples = sampleCreateLikePath(start, bezierStart, axis1, bezierEnd, axis2, end, handleLength);
		if (samples.size() < 2) {
			return null;
		}

		return new PreviewConnectionSolution(
			List.copyOf(samples),
			samples.getLast().distance(),
			Math.max(0.0D, samples.getLast().distance() - end1Extent - end2Extent),
			Math.max(0, (int) Math.round(end1Extent)),
			Math.max(0, (int) Math.round(end2Extent)),
			false);
	}

	private static List<RailSample> extractRenderableCurveSamples(List<RailSample> samples, int startExtent, int endExtent) {
		if (samples.size() < 2) {
			return List.of();
		}
		if (startExtent <= 0 && endExtent <= 0) {
			return List.copyOf(samples);
		}

		double startDistance = Math.max(0.0D, startExtent);
		double endDistance = Math.max(startDistance, samples.getLast().distance() - Math.max(0.0D, endExtent));
		if (endDistance - startDistance <= PATH_EPSILON) {
			return List.of();
		}

		ArrayList<RailSample> extracted = new ArrayList<>();
		extracted.add(RailGeometry.sampleAtDistance(samples, startDistance));
		for (RailSample sample : samples) {
			if (sample.distance() > startDistance + PATH_EPSILON && sample.distance() < endDistance - PATH_EPSILON) {
				extracted.add(sample);
			}
		}
		extracted.add(RailGeometry.sampleAtDistance(samples, endDistance));
		return extracted.size() < 2 ? List.of() : List.copyOf(extracted);
	}

	private static List<RailSample> sampleCreateLikePath(
		Vec3d start,
		Vec3d bezierStart,
		Vec3d axis1,
		Vec3d bezierEnd,
		Vec3d axis2,
		Vec3d end,
		double handleLength
	) {
		ArrayList<RailSample> samples = new ArrayList<>();
		appendLinearSamples(samples, start, bezierStart);
		appendCreateBezierSamples(samples, bezierStart, bezierEnd, axis1, axis2, handleLength);
		appendLinearSamples(samples, bezierEnd, end);
		return samples;
	}

	private static List<RailSample> generateSamples(Vec3d start, Direction fromFacing, Vec3d end, Direction toFacing, double handleScale, double maxHandleLength) {
		double chordLength = start.distanceTo(end);
		double handleLength = Math.clamp(chordLength * handleScale, MIN_HANDLE_LENGTH, maxHandleLength);

		Vec3d fromTangent = unitVector(fromFacing);
		Vec3d toTangent = unitVector(toFacing);
		Vec3d control1 = start.add(fromTangent.multiply(handleLength));
		Vec3d control2 = end.subtract(toTangent.multiply(handleLength));

		int segments = Math.max(8, (int) Math.ceil(chordLength / SAMPLE_STEP));
		List<RailSample> samples = new ArrayList<>(segments + 1);
		double traveled = 0.0D;
		Vec3d previousPoint = null;
		Vec3d fallbackDirection = end.subtract(start).normalize();

		for (int index = 0; index <= segments; index++) {
			double t = index / (double) segments;
			Vec3d point = cubicBezier(start, control1, control2, end, t);
			Vec3d derivative = cubicBezierDerivative(start, control1, control2, end, t);
			Vec3d tangent = derivative.lengthSquared() > 1.0E-6D ? derivative.normalize() : fallbackDirection;

			if (previousPoint != null) {
				traveled += point.distanceTo(previousPoint);
			}

			samples.add(new RailSample(point.x, point.y, point.z, tangent.x, tangent.y, tangent.z, traveled));
			previousPoint = point;
		}

		return samples;
	}

	private static void appendCreateBezierSamples(List<RailSample> samples, Vec3d start, Vec3d end, Vec3d axis1, Vec3d axis2, double handleLength) {
		Vec3d control1 = start.add(axis1.multiply(handleLength));
		Vec3d control2 = end.add(axis2.multiply(handleLength));
		double chordLength = start.distanceTo(end);
		int segments = Math.max(8, (int) Math.ceil(chordLength / SAMPLE_STEP));

		if (samples.isEmpty()) {
			Vec3d initialTangent = cubicBezierDerivative(start, control1, control2, end, 0.0D);
			appendSample(samples, start, initialTangent);
		}

		for (int index = 1; index <= segments; index++) {
			double t = index / (double) segments;
			Vec3d point = cubicBezier(start, control1, control2, end, t);
			Vec3d derivative = cubicBezierDerivative(start, control1, control2, end, t);
			appendSample(samples, point, derivative);
		}
	}

	private static void appendLinearSamples(List<RailSample> samples, Vec3d from, Vec3d to) {
		double length = from.distanceTo(to);
		if (length <= PATH_EPSILON) {
			if (samples.isEmpty()) {
				appendSample(samples, from, to.subtract(from));
			}
			return;
		}

		Vec3d direction = to.subtract(from);
		if (samples.isEmpty()) {
			appendSample(samples, from, direction);
		}

		int segments = Math.max(1, (int) Math.ceil(length / SAMPLE_STEP));
		for (int index = 1; index <= segments; index++) {
			double t = index / (double) segments;
			appendSample(samples, from.lerp(to, t), direction);
		}
	}

	private static List<RailSample> sampleLineWithDistance(Vec3d start, Vec3d end) {
		ArrayList<RailSample> samples = new ArrayList<>();
		appendLinearSamples(samples, start, end);
		return samples;
	}

	private static List<RailSample> sampleStraightLine(Vec3d start, Vec3d end, Direction facing) {
		ArrayList<RailSample> samples = new ArrayList<>();
		Vec3d tangent = unitVector(facing);
		double length = start.distanceTo(end);

		samples.add(new RailSample(start.x, start.y, start.z, tangent.x, tangent.y, tangent.z, 0.0D));
		if (length <= PATH_EPSILON) {
			return samples;
		}

		int segments = Math.max(1, (int) Math.ceil(length / SAMPLE_STEP));
		for (int index = 1; index <= segments; index++) {
			double t = index / (double) segments;
			Vec3d point = start.lerp(end, t);
			double traveled = length * t;
			samples.add(new RailSample(point.x, point.y, point.z, tangent.x, tangent.y, tangent.z, traveled));
		}

		return samples;
	}

	private static double determineCreateHandleLength(Vec3d end1, Vec3d end2, Vec3d axis1, Vec3d axis2) {
		Vec3d cross1 = rightNormal(axis1);
		Vec3d cross2 = rightNormal(axis2);

		double a1 = Math.atan2(-axis2.z, -axis2.x);
		double a2 = Math.atan2(axis1.z, axis1.x);
		double angle = a1 - a2;
		double circle = Math.PI * 2.0D;
		angle = (angle + circle) % circle;
		if (Math.abs(circle - angle) < Math.abs(angle)) {
			angle = circle - angle;
		}

		if (Math.abs(angle) <= PATH_EPSILON) {
			double[] intersect = intersectXZ(end1, end2, axis1, cross2);
			if (intersect != null) {
				double t = Math.abs(intersect[0]);
				double u = Math.abs(intersect[1]);
				double min = Math.min(t, u);
				double max = Math.max(t, u);
				if (min > 1.2D && max / min > 1.0D && max / min < 3.0D) {
					return max - min;
				}
			}

			return Math.max(1.0D, end2.distanceTo(end1) / 3.0D);
		}

		double n = circle / angle;
		double factor = 4.0D / 3.0D * Math.tan(Math.PI / (2.0D * n));
		double[] intersect = intersectXZ(end1, end2, cross1, cross2);
		if (intersect == null) {
			return Math.max(1.0D, end2.distanceTo(end1) / 3.0D);
		}

		double radius = Math.abs(intersect[1]);
		double handleLength = radius * factor;
		return Math.max(1.0D, handleLength);
	}

	private static Vec3d cubicBezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
		double oneMinusT = 1.0D - t;
		double a = oneMinusT * oneMinusT * oneMinusT;
		double b = 3.0D * oneMinusT * oneMinusT * t;
		double c = 3.0D * oneMinusT * t * t;
		double d = t * t * t;
		return p0.multiply(a)
			.add(p1.multiply(b))
			.add(p2.multiply(c))
			.add(p3.multiply(d));
	}

	private static Vec3d cubicBezierDerivative(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
		double oneMinusT = 1.0D - t;
		Vec3d a = p1.subtract(p0).multiply(3.0D * oneMinusT * oneMinusT);
		Vec3d b = p2.subtract(p1).multiply(6.0D * oneMinusT * t);
		Vec3d c = p3.subtract(p2).multiply(3.0D * t * t);
		return a.add(b).add(c);
	}

	private static void appendSample(List<RailSample> samples, Vec3d point, Vec3d tangent) {
		Vec3d normalizedTangent = horizontalDirection(tangent);
		double traveled = 0.0D;
		if (!samples.isEmpty()) {
			RailSample last = samples.getLast();
			Vec3d previous = last.position();
			if (previous.squaredDistanceTo(point) <= PATH_EPSILON * PATH_EPSILON) {
				return;
			}
			traveled = last.distance() + previous.distanceTo(point);
		}

		samples.add(new RailSample(point.x, point.y, point.z, normalizedTangent.x, normalizedTangent.y, normalizedTangent.z, traveled));
	}

	private static List<Direction> axisCandidates(Direction direction) {
		if (!direction.getAxis().isHorizontal()) {
			return List.of(direction);
		}
		return List.of(direction, direction.getOpposite());
	}

	private static void appendUnique(List<Direction> directions, List<Direction> candidates) {
		for (Direction candidate : candidates) {
			if (!directions.contains(candidate)) {
				directions.add(candidate);
			}
		}
	}

	private static List<Direction> previewCandidates(Direction fromFacing) {
		return switch (fromFacing) {
			case NORTH -> List.of(Direction.NORTH, Direction.WEST, Direction.EAST);
			case SOUTH -> List.of(Direction.SOUTH, Direction.EAST, Direction.WEST);
			case WEST -> List.of(Direction.WEST, Direction.SOUTH, Direction.NORTH);
			case EAST -> List.of(Direction.EAST, Direction.NORTH, Direction.SOUTH);
			default -> List.of(fromFacing);
		};
	}

	private static double[] intersectXZ(Vec3d p1, Vec3d p2, Vec3d d1, Vec3d d2) {
		double det = horizontalCross(d1, d2);
		if (Math.abs(det) <= PATH_EPSILON) {
			return null;
		}

		Vec3d delta = p2.subtract(p1);
		double t = horizontalCross(delta, d2) / det;
		double u = horizontalCross(delta, d1) / det;
		return new double[] {t, u};
	}

	private static double horizontalAngleDegrees(Vec3d axis1, Vec3d axis2) {
		double dot = Math.clamp(horizontalDot(axis1, axis2), -1.0D, 1.0D);
		return Math.toDegrees(Math.acos(dot));
	}

	private static double prioritizedCurveScore(PreviewConnectionSolution solution) {
		double straightExtentWeight = 1000.0D;
		return solution.curveLengthScore() - (solution.startExtent() + solution.endExtent()) * straightExtentWeight;
	}

	private static boolean allowsShortDiagonal(Vec3d delta, Vec3d startDir, Vec3d endDir) {
		return horizontalDot(delta, startDir) > 0.75D || horizontalDot(delta, endDir.multiply(-1.0D)) > 0.75D;
	}

	private static double minimumParallelForwardDistance(double lateralOffset) {
		return lateralOffset <= 1.0D ? 3.0D : lateralOffset * 2.0D;
	}

	private static double horizontalDot(Vec3d a, Vec3d b) {
		return a.x * b.x + a.z * b.z;
	}

	private static double horizontalCross(Vec3d a, Vec3d b) {
		return a.x * b.z - a.z * b.x;
	}

	private static double horizontalLength(Vec3d vector) {
		return Math.sqrt(vector.x * vector.x + vector.z * vector.z);
	}

	private static Vec3d horizontalDirection(Vec3d vector) {
		double length = horizontalLength(vector);
		if (length <= PATH_EPSILON) {
			return new Vec3d(1.0D, 0.0D, 0.0D);
		}
		return new Vec3d(vector.x / length, 0.0D, vector.z / length);
	}

	private static Vec3d rightNormal(Vec3d vector) {
		return horizontalDirection(new Vec3d(vector.z, 0.0D, -vector.x));
	}

	private static Vec3d unitVector(Direction direction) {
		return new Vec3d(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ()).normalize();
	}

	private record PreviewConnectionSolution(
		List<RailSample> samples,
		double score,
		double curveLengthScore,
		int startExtent,
		int endExtent,
		boolean straight
	) {
	}

	private record PathCandidateSolution(
		List<RailSample> samples,
		double score,
		int startExtent,
		int endExtent,
		boolean straight
	) {
	}

	private record ToolPathCandidate(ToolPath path, double score) {
	}

	public record ToolPath(
		List<RailSample> samples,
		List<RailSample> renderSamples,
		boolean valid,
		ToolPathKind kind,
		Direction startFacing,
		Direction endFacing,
		int startExtent,
		int endExtent
	) {
	}

	public enum ToolPathKind {
		NONE,
		STRAIGHT,
		CURVE
	}
}
