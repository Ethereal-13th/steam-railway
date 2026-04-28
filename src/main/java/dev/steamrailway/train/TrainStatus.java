package dev.steamrailway.train;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum TrainStatus {
	STOPPED,
	MOVING;

	public static final Codec<TrainStatus> CODEC = Codec.STRING.xmap(TrainStatus::fromSerialized, TrainStatus::serializedName);

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	private static TrainStatus fromSerialized(String value) {
		return valueOf(value.toUpperCase(Locale.ROOT));
	}
}
