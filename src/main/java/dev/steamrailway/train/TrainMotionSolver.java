package dev.steamrailway.train;

import dev.steamrailway.rail.RailEdge;

public final class TrainMotionSolver {
	private TrainMotionSolver() {
	}

	public static TrainState tick(TrainState trainState, RailEdge railEdge, double tickDelta) {
		if (trainState.status() != TrainStatus.MOVING) {
			return trainState;
		}

		double nextDistance = trainState.distanceOnEdge() + trainState.speed() * tickDelta;
		if (nextDistance >= railEdge.length()) {
			return trainState.stoppedAt(railEdge.length());
		}

		return trainState.withDistanceOnEdge(nextDistance);
	}
}
