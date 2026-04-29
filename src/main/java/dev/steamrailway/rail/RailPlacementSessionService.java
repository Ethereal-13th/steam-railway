package dev.steamrailway.rail;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RailPlacementSessionService {
	private static final Map<UUID, RailPlacementSession> SESSIONS = new ConcurrentHashMap<>();

	private RailPlacementSessionService() {
	}

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player.getUuid()));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> clear(player.getUuid()));
	}

	public static void setStart(ServerPlayerEntity player, BlockPos startPos) {
		SESSIONS.put(player.getUuid(), new RailPlacementSession(
			player.getUuid(),
			player.getEntityWorld().getRegistryKey().getValue().toString(),
			startPos.toImmutable()));
	}

	public static Optional<RailPlacementSession> get(ServerPlayerEntity player) {
		return Optional.ofNullable(SESSIONS.get(player.getUuid()));
	}

	public static void clear(UUID playerId) {
		SESSIONS.remove(playerId);
	}

	public static void clearAll(MinecraftServer server) {
		SESSIONS.clear();
	}

	public record RailPlacementSession(UUID playerUuid, String dimension, BlockPos startPos) {
	}
}
