package dev.steamrailway.rail;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class RailPathGenerator {
	public static final double MIN_PATH_DISTANCE = 4.0D;
	public static final double MAX_PATH_DISTANCE = 128.0D;
	private static final double HANDLE_SCALE = 0.35D;
	private static final double MIN_HANDLE_LENGTH = 2.0D;
	private static final double MAX_HANDLE_LENGTH = 16.0D;
	private static final double SAMPLE_STEP = 0.5D;

	private RailPathGenerator() {
	}

	public static RailEdge createEdge(String edgeId, RailNode fromNode, Direction fromFacing, RailNode toNode, Direction toFacing) {
		Vec3d start = fromNode.worldPos();
		Vec3d end = toNode.worldPos();
		double chordLength = start.distanceTo(end);
		double handleLength = Math.clamp(chordLength * HANDLE_SCALE, MIN_HANDLE_LENGTH, MAX_HANDLE_LENGTH);

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

		return new RailEdge(edgeId, fromNode.id(), toNode.id(), List.copyOf(samples), traveled);
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

	private static Vec3d unitVector(Direction direction) {
		return new Vec3d(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ()).normalize();
	}
}
