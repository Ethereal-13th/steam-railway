package dev.steamrailway.train;

import dev.steamrailway.SteamRailwayMod;
import dev.steamrailway.rail.RailDisplayHelper;
import dev.steamrailway.rail.RailEdge;
import dev.steamrailway.rail.RailSample;
import dev.steamrailway.registry.SREntities;
import dev.steamrailway.train.entity.TrainEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.UUID;

public final class TrainSpawnManager {
	private TrainSpawnManager() {
	}

	public static TrainState syncVisibleEntity(MinecraftServer server, RailEdge railEdge, TrainState trainState) {
		ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(trainState.dimension())));
		if (world == null) {
			return trainState;
		}

		TrainEntity entity = resolveTrackedEntity(world, trainState);
		if (entity == null) {
			entity = spawnEntity(world, trainState);
			if (entity == null) {
				return trainState;
			}
		}

		pruneDuplicates(world, trainState.trainId(), entity.getUuid());
		syncEntityTransform(entity, railEdge, trainState.distanceOnEdge());

		String visibleEntityId = entity.getUuidAsString();
		if (trainState.visibleEntityId().filter(visibleEntityId::equals).isPresent()) {
			return trainState;
		}

		return trainState.withVisibleEntityId(Optional.of(visibleEntityId));
	}

	public static Optional<TrainEntity> findVisibleEntity(MinecraftServer server, TrainState trainState) {
		ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(trainState.dimension())));
		if (world == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(resolveTrackedEntity(world, trainState));
	}

	public static TrainState despawnVisibleEntity(MinecraftServer server, TrainState trainState) {
		ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(trainState.dimension())));
		if (world != null) {
			discardTrackedEntities(world, trainState);
		}

		if (trainState.visibleEntityId().isEmpty()) {
			return trainState;
		}

		return trainState.withVisibleEntityId(Optional.empty());
	}

	private static TrainEntity resolveTrackedEntity(ServerWorld world, TrainState trainState) {
		if (trainState.visibleEntityId().isPresent()) {
			try {
				UUID uuid = UUID.fromString(trainState.visibleEntityId().get());
				Entity entity = world.getEntityAnyDimension(uuid);
				if (entity instanceof TrainEntity trainEntity && trainState.trainId().equals(trainEntity.getLogicalTrainId())) {
					return trainEntity;
				}
			} catch (IllegalArgumentException ignored) {
				// fall through to lookup by logical id
			}
		}

		for (Entity entity : world.iterateEntities()) {
			if (entity instanceof TrainEntity trainEntity && trainState.trainId().equals(trainEntity.getLogicalTrainId())) {
				return trainEntity;
			}
		}

		return null;
	}

	private static void discardTrackedEntities(ServerWorld world, TrainState trainState) {
		if (trainState.visibleEntityId().isPresent()) {
			try {
				UUID uuid = UUID.fromString(trainState.visibleEntityId().get());
				Entity entity = world.getEntityAnyDimension(uuid);
				if (entity instanceof TrainEntity trainEntity && trainState.trainId().equals(trainEntity.getLogicalTrainId())) {
					trainEntity.discard();
				}
			} catch (IllegalArgumentException ignored) {
				// fall through to logical id sweep
			}
		}

		for (Entity entity : world.iterateEntities()) {
			if (entity instanceof TrainEntity trainEntity && trainState.trainId().equals(trainEntity.getLogicalTrainId())) {
				trainEntity.discard();
			}
		}
	}

	private static TrainEntity spawnEntity(ServerWorld world, TrainState trainState) {
		TrainEntity entity = new TrainEntity(SREntities.TRAIN, world);
		entity.setLogicalTrainId(trainState.trainId());
		entity.setCustomName(Text.literal(trainState.trainId()));
		entity.setCustomNameVisible(true);
		entity.setGlowing(true);
		boolean spawned = world.spawnEntity(entity);
		if (!spawned) {
			SteamRailwayMod.LOGGER.warn("Failed to spawn visible entity for train {}", trainState.trainId());
			return null;
		}
		return entity;
	}

	private static void pruneDuplicates(ServerWorld world, String trainId, UUID keepUuid) {
		for (Entity entity : world.iterateEntities()) {
			if (!(entity instanceof TrainEntity trainEntity)) {
				continue;
			}
			if (!trainId.equals(trainEntity.getLogicalTrainId())) {
				continue;
			}
			if (keepUuid.equals(trainEntity.getUuid())) {
				continue;
			}
			trainEntity.discard();
		}
	}

	private static void syncEntityTransform(TrainEntity entity, RailEdge railEdge, double distanceOnEdge) {
		RailSample sample = findSampleAtDistance(railEdge, distanceOnEdge);
		Vec3d position = RailDisplayHelper.visiblePosition(railEdge, sample);
		float yaw = toYawDegrees(sample.tangent());

		entity.setCustomNameVisible(true);
		entity.setGlowing(true);
		entity.refreshPositionAndAngles(position, yaw, 0.0F);
		entity.setVelocity(Vec3d.ZERO);
		entity.setYaw(yaw);
		entity.setHeadYaw(yaw);
		entity.setBodyYaw(yaw);
		entity.updateTrackedPositionAndAngles(position, yaw, 0.0F);
	}

	private static RailSample findSampleAtDistance(RailEdge railEdge, double distanceOnEdge) {
		RailSample closest = railEdge.samples().getFirst();
		double bestDelta = Math.abs(distanceOnEdge - closest.distance());

		for (RailSample sample : railEdge.samples()) {
			double delta = Math.abs(distanceOnEdge - sample.distance());
			if (delta < bestDelta) {
				closest = sample;
				bestDelta = delta;
			}
		}

		return closest;
	}

	private static float toYawDegrees(Vec3d tangent) {
		return (float) (Math.toDegrees(Math.atan2(tangent.z, tangent.x)) - 90.0D);
	}
}
