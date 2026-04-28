package dev.steamrailway.debug;

import dev.steamrailway.persistence.SRSaveData;
import dev.steamrailway.rail.RailDisplayHelper;
import dev.steamrailway.rail.RailEdge;
import dev.steamrailway.rail.RailNetwork;
import dev.steamrailway.rail.RailNode;
import dev.steamrailway.rail.RailSample;
import dev.steamrailway.train.TrainState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DebugPathRenderer {
	private static final Map<UUID, String> ACTIVE_PATHS = new ConcurrentHashMap<>();
	private static final Map<UUID, String> ACTIVE_TRAINS = new ConcurrentHashMap<>();
	private static int tickCounter;

	private DebugPathRenderer() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(DebugPathRenderer::tick);
	}

	public static void showPath(UUID playerId, String pathId) {
		ACTIVE_PATHS.put(playerId, pathId);
	}

	public static void clearPath(UUID playerId) {
		ACTIVE_PATHS.remove(playerId);
	}

	public static void showTrain(UUID playerId, String trainId) {
		ACTIVE_TRAINS.put(playerId, trainId);
	}

	public static Optional<String> getSelectedTrainId(UUID playerId) {
		return Optional.ofNullable(ACTIVE_TRAINS.get(playerId));
	}

	public static Optional<String> getSelectedPathId(UUID playerId) {
		return Optional.ofNullable(ACTIVE_PATHS.get(playerId));
	}

	public static void renderPath(ServerWorld world, RailNetwork network, ServerPlayerEntity player, RailEdge edge) {
		RailNode fromNode = network.getNodeById(edge.fromNodeId());
		if (fromNode == null) {
			return;
		}
		if (!player.getEntityWorld().getRegistryKey().getValue().equals(Identifier.of(fromNode.dimension()))) {
			return;
		}

		spawnPathParticles(world, edge);
	}

	private static void tick(MinecraftServer server) {
		tickCounter++;
		if (tickCounter % 10 != 0) {
			return;
		}

		RailNetwork network = SRSaveData.get(server).railNetwork();
		SRSaveData saveData = SRSaveData.get(server);
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			String pathId = ACTIVE_PATHS.get(player.getUuid());
			if (pathId == null) {
				// no-op
			} else {
				RailEdge edge = network.getEdge(pathId);
				if (edge == null) {
					ACTIVE_PATHS.remove(player.getUuid());
				} else {
					RailNode fromNode = network.getNodeById(edge.fromNodeId());
					if (fromNode != null && player.getEntityWorld().getRegistryKey().getValue().equals(Identifier.of(fromNode.dimension()))) {
						spawnPathParticles(player.getEntityWorld(), edge);
					}
				}
			}

			String trainId = ACTIVE_TRAINS.get(player.getUuid());
			if (trainId == null) {
				continue;
			}

			TrainState trainState = saveData.getTrain(trainId);
			if (trainState == null) {
				ACTIVE_TRAINS.remove(player.getUuid());
				continue;
			}

			if (!player.getEntityWorld().getRegistryKey().getValue().equals(Identifier.of(trainState.dimension()))) {
				continue;
			}

			RailEdge edge = network.getEdge(trainState.edgeId());
			if (edge == null) {
				continue;
			}

			spawnTrainParticle(player.getEntityWorld(), edge, trainState.distanceOnEdge());
		}
	}

	private static void spawnPathParticles(ServerWorld world, RailEdge edge) {
		if (edge.samples().isEmpty()) {
			return;
		}

		RailSample first = edge.samples().getFirst();
		RailSample last = edge.samples().getLast();
		spawnMarker(world, RailDisplayHelper.visiblePosition(edge, first), ParticleTypes.HAPPY_VILLAGER);
		spawnMarker(world, RailDisplayHelper.visiblePosition(edge, last), ParticleTypes.FLAME);

		for (int index = 0; index < edge.samples().size(); index += 4) {
			RailSample sample = edge.samples().get(index);
			Vec3d point = RailDisplayHelper.visiblePosition(edge, sample);
			world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	private static void spawnMarker(ServerWorld world, Vec3d point, ParticleEffect particle) {
		world.spawnParticles(particle, point.x, point.y, point.z, 6, 0.1D, 0.1D, 0.1D, 0.0D);
	}

	public static void renderTrainPosition(ServerWorld world, RailEdge edge, double distanceOnEdge) {
		spawnTrainParticle(world, edge, distanceOnEdge);
	}

	private static void spawnTrainParticle(ServerWorld world, RailEdge edge, double distanceOnEdge) {
		RailSample sample = findSampleAtDistance(edge, distanceOnEdge);
		Vec3d point = RailDisplayHelper.visiblePosition(edge, sample);
		world.spawnParticles(ParticleTypes.CRIT, point.x, point.y + 0.1D, point.z, 8, 0.12D, 0.12D, 0.12D, 0.0D);
	}

	private static RailSample findSampleAtDistance(RailEdge edge, double distanceOnEdge) {
		RailSample closest = edge.samples().getFirst();
		double bestDelta = Math.abs(distanceOnEdge - closest.distance());

		for (RailSample sample : edge.samples()) {
			double delta = Math.abs(distanceOnEdge - sample.distance());
			if (delta < bestDelta) {
				closest = sample;
				bestDelta = delta;
			}
		}

		return closest;
	}
}
