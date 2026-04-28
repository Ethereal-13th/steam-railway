package dev.steamrailway.train;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record TrainState(
	String trainId,
	String dimension,
	String edgeId,
	double distanceOnEdge,
	double speed,
	double targetSpeed,
	TrainStatus status,
	Optional<String> visibleEntityId
) {
	public static final Codec<TrainState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("train_id").forGetter(TrainState::trainId),
		Codec.STRING.fieldOf("dimension").forGetter(TrainState::dimension),
		Codec.STRING.fieldOf("edge_id").forGetter(TrainState::edgeId),
		Codec.DOUBLE.fieldOf("distance_on_edge").forGetter(TrainState::distanceOnEdge),
		Codec.DOUBLE.fieldOf("speed").forGetter(TrainState::speed),
		Codec.DOUBLE.fieldOf("target_speed").forGetter(TrainState::targetSpeed),
		TrainStatus.CODEC.fieldOf("status").forGetter(TrainState::status),
		Codec.STRING.optionalFieldOf("visible_entity_id").forGetter(TrainState::visibleEntityId)
	).apply(instance, TrainState::new));

	public static TrainState createStopped(String trainId, String dimension, String edgeId) {
		return new TrainState(trainId, dimension, edgeId, 0.0D, 0.0D, 0.0D, TrainStatus.STOPPED, Optional.empty());
	}

	public TrainState withDistanceOnEdge(double distanceOnEdge) {
		return new TrainState(trainId, dimension, edgeId, distanceOnEdge, speed, targetSpeed, status, visibleEntityId);
	}

	public TrainState movingAt(double targetSpeed) {
		return new TrainState(trainId, dimension, edgeId, distanceOnEdge, targetSpeed, targetSpeed, TrainStatus.MOVING, visibleEntityId);
	}

	public TrainState stoppedAt(double distanceOnEdge) {
		return new TrainState(trainId, dimension, edgeId, distanceOnEdge, 0.0D, 0.0D, TrainStatus.STOPPED, visibleEntityId);
	}

	public TrainState stopForMissingEdge() {
		return new TrainState(trainId, dimension, edgeId, distanceOnEdge, 0.0D, 0.0D, TrainStatus.STOPPED, visibleEntityId);
	}

	public TrainState withVisibleEntityId(Optional<String> visibleEntityId) {
		return new TrainState(trainId, dimension, edgeId, distanceOnEdge, speed, targetSpeed, status, visibleEntityId);
	}
}
