package dev.steamrailway.rail;

import net.minecraft.util.math.Vec3d;

public final class RailDisplayHelper {
	private static final double LEGACY_SAMPLE_Y_FRACTION = 0.5D;
	private static final double LEGACY_SAMPLE_Y_OFFSET = 0.5D;
	private static final double LEGACY_EPSILON = 1.0E-6D;

	private RailDisplayHelper() {
	}

	public static Vec3d visiblePosition(RailEdge edge, RailSample sample) {
		return sample.position().add(0.0D, visualYOffset(edge), 0.0D);
	}

	private static double visualYOffset(RailEdge edge) {
		if (edge.samples().isEmpty()) {
			return 0.0D;
		}

		RailSample first = edge.samples().getFirst();
		RailSample last = edge.samples().getLast();
		if (isLegacyCenteredSample(first.y()) && isLegacyCenteredSample(last.y())) {
			return LEGACY_SAMPLE_Y_OFFSET;
		}

		return 0.0D;
	}

	private static boolean isLegacyCenteredSample(double y) {
		double fraction = y - Math.floor(y);
		return Math.abs(fraction - LEGACY_SAMPLE_Y_FRACTION) < LEGACY_EPSILON;
	}
}
