package dev.steamrailway.train;

import dev.steamrailway.SteamRailwayMod;
import dev.steamrailway.persistence.SRSaveData;
import dev.steamrailway.rail.RailEdge;
import dev.steamrailway.rail.RailNetwork;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;

public final class TrainRuntimeService {
	private static final double TICK_DELTA = 1.0D;

	private TrainRuntimeService() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TrainRuntimeService::tick);
	}

	private static void tick(MinecraftServer server) {
		SRSaveData saveData = SRSaveData.get(server);
		RailNetwork railNetwork = saveData.railNetwork();
		boolean changed = false;

		for (TrainState trainState : new ArrayList<>(saveData.trains().values())) {
			RailEdge railEdge = railNetwork.getEdge(trainState.edgeId());
			if (railEdge == null) {
				TrainState stopped = TrainSpawnManager.despawnVisibleEntity(server, trainState.stopForMissingEdge());
				if (!stopped.equals(trainState)) {
					saveData.putTrain(stopped);
					saveData.markDirty();
					changed = true;
					SteamRailwayMod.LOGGER.warn("Train {} stopped and cleared its visible entity because path {} is missing", trainState.trainId(), trainState.edgeId());
				}
				continue;
			}

			TrainState nextState = TrainMotionSolver.tick(trainState, railEdge, TICK_DELTA);
			nextState = TrainSpawnManager.syncVisibleEntity(server, railEdge, nextState);
			if (!nextState.equals(trainState)) {
				saveData.putTrain(nextState);
				changed = true;
			}
		}

		if (changed) {
			saveData.markDirty();
		}
	}
}
