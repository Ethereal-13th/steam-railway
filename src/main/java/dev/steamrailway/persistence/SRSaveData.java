package dev.steamrailway.persistence;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.steamrailway.rail.RailNetwork;
import dev.steamrailway.train.TrainState;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public final class SRSaveData extends PersistentState {
	private static final String SAVE_ID = "steam_railway_data";
	private static final Codec<SRSaveData> STRUCTURED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		RailNetwork.CODEC.optionalFieldOf("rail_network", new RailNetwork()).forGetter(SRSaveData::railNetwork),
		Codec.unboundedMap(Codec.STRING, TrainState.CODEC).optionalFieldOf("trains", Map.of()).forGetter(SRSaveData::trains)
	).apply(instance, SRSaveData::new));
	private static final Codec<SRSaveData> CODEC = Codec.of(
		STRUCTURED_CODEC,
		new Decoder<>() {
			@Override
			public <T> DataResult<Pair<SRSaveData, T>> decode(DynamicOps<T> ops, T input) {
				if (hasField(ops, input, "rail_network") || hasField(ops, input, "trains")) {
					return STRUCTURED_CODEC.decode(ops, input);
				}

				return RailNetwork.CODEC.decode(ops, input).map(pair -> pair.mapFirst(SRSaveData::new));
			}
		});
	private static final PersistentStateType<SRSaveData> TYPE =
		new PersistentStateType<>(SAVE_ID, SRSaveData::new, CODEC, DataFixTypes.LEVEL);

	private final RailNetwork railNetwork;
	private final LinkedHashMap<String, TrainState> trains;

	public SRSaveData() {
		this(new RailNetwork(), Map.of());
	}

	private SRSaveData(RailNetwork railNetwork) {
		this(railNetwork, Map.of());
	}

	private SRSaveData(RailNetwork railNetwork, Map<String, TrainState> trains) {
		this.railNetwork = railNetwork;
		this.trains = new LinkedHashMap<>(trains);
	}

	public static SRSaveData get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
	}

	public RailNetwork railNetwork() {
		return railNetwork;
	}

	public Map<String, TrainState> trains() {
		return Collections.unmodifiableMap(trains);
	}

	public TrainState getTrain(String trainId) {
		return trains.get(trainId);
	}

	public void putTrain(TrainState trainState) {
		trains.put(trainState.trainId(), trainState);
	}

	public String nextTrainId() {
		return String.format(Locale.ROOT, "train_%04d", trains.size() + 1);
	}

	private static <T> boolean hasField(DynamicOps<T> ops, T input, String fieldName) {
		return ops.getMap(input)
			.result()
			.map(map -> map.get(fieldName) != null)
			.orElse(false);
	}
}
