package dev.steamrailway.rail;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class RailGeometry {
	private static final double PATH_EPSILON = 1.0E-6D;
	private static final double STANDARD_RAIL_Y_OFFSET = 0.25D;

	private RailGeometry() {
	}

	public static Vec3d worldPosition(BlockPos pos) {
		return Vec3d.ofBottomCenter(pos).add(0.0D, STANDARD_RAIL_Y_OFFSET, 0.0D);
	}

	public static RailSample sampleAtDistance(List<RailSample> samples, double targetDistance) {
		RailSample previous = samples.getFirst();
		for (int index = 1; index < samples.size(); index++) {
			RailSample current = samples.get(index);
			if (targetDistance <= current.distance()) {
				double span = current.distance() - previous.distance();
				double progress = span <= PATH_EPSILON ? 0.0D : (targetDistance - previous.distance()) / span;
				Vec3d position = previous.position().lerp(current.position(), progress);
				Vec3d tangent = previous.tangent().lerp(current.tangent(), progress).normalize();
				return new RailSample(
					position.x,
					position.y,
					position.z,
					tangent.x,
					tangent.y,
					tangent.z,
					targetDistance);
			}
			previous = current;
		}
		return samples.getLast();
	}

	public static Direction cardinalFacing(Vec3d tangent, Direction fallback) {
		Vec3d flat = new Vec3d(tangent.x, 0.0D, tangent.z);
		if (flat.lengthSquared() <= PATH_EPSILON) {
			return fallback;
		}
		if (Math.abs(flat.x) >= Math.abs(flat.z)) {
			return flat.x >= 0.0D ? Direction.EAST : Direction.WEST;
		}
		return flat.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
	}

	public static Vec3d lateralVector(Vec3d tangent) {
		Vec3d flat = new Vec3d(tangent.x, 0.0D, tangent.z);
		if (flat.lengthSquared() <= PATH_EPSILON) {
			return new Vec3d(1.0D, 0.0D, 0.0D);
		}
		return new Vec3d(-flat.z, 0.0D, flat.x).normalize();
	}

	public static BlockPos sampleToBlockPos(Vec3d position) {
		return BlockPos.ofFloored(position.x, position.y, position.z);
	}
}
