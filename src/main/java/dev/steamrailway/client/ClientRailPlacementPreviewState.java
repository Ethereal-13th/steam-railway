package dev.steamrailway.client;

import dev.steamrailway.net.RailPlacementModePayload;
import dev.steamrailway.rail.RailPlacementClientHooks;
import dev.steamrailway.rail.RailPlacementMode;
import dev.steamrailway.rail.RailPlacementTargetResolver;
import dev.steamrailway.registry.SRItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public final class ClientRailPlacementPreviewState {
	private static PreviewSession currentSession;

	private ClientRailPlacementPreviewState() {
	}

	public static void register() {
		RailPlacementClientHooks.register(ClientRailPlacementPreviewState::handleUse);
		ClientTickEvents.END_CLIENT_TICK.register(ClientRailPlacementPreviewState::tick);
	}

	public static Optional<PreviewSession> getCurrentSession() {
		return Optional.ofNullable(currentSession);
	}

	public static void clear() {
		currentSession = null;
	}

	private static void handleUse(ItemUsageContext context) {
		if (context.getPlayer() == null || !context.getWorld().isClient()) {
			return;
		}
		if (context.getPlayer().isSneaking()) {
			clear();
			return;
		}

		if (currentSession != null) {
			ClientPlayNetworking.send(new RailPlacementModePayload(currentSession.mode().gentle()));
			RailPlacementTargetResolver.resolveTarget(
				context.getWorld(),
				context.getBlockPos(),
				context.getSide(),
				context.getHorizontalPlayerFacing()
			).ifPresent(target -> clear());
			return;
		}

		RailPlacementTargetResolver.resolveTarget(
			context.getWorld(),
			context.getBlockPos(),
			context.getSide(),
			context.getHorizontalPlayerFacing()
		).ifPresent(target -> currentSession = new PreviewSession(
			context.getWorld().getRegistryKey().getValue().toString(),
			target.pos().toImmutable(),
			target.facing(),
			target.lockedFacing(),
			RailPlacementMode.NORMAL));
	}

	private static void tick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			clear();
			return;
		}
		if (!isHoldingPlacementTool(client)) {
			clear();
			return;
		}
		if (currentSession == null) {
			return;
		}
		if (!currentSession.dimension().equals(client.world.getRegistryKey().getValue().toString())) {
			clear();
			return;
		}

		RailPlacementMode mode = isGentlePreviewHeld(client) ? RailPlacementMode.GENTLE : RailPlacementMode.NORMAL;
		if (currentSession.mode() != mode) {
			currentSession = new PreviewSession(
				currentSession.dimension(),
				currentSession.startPos(),
				currentSession.startFacing(),
				currentSession.startFacingLocked(),
				mode);
		}

		if (client.player.age % 2 == 0) {
			RailPlacementPreviewRenderer.spawnParticlePreview(client, currentSession);
		}
	}

	private static boolean isHoldingPlacementTool(MinecraftClient client) {
		return client.player.getMainHandStack().isOf(SRItems.RAIL_PLACEMENT_TOOL)
			|| client.player.getOffHandStack().isOf(SRItems.RAIL_PLACEMENT_TOOL);
	}

	private static boolean isGentlePreviewHeld(MinecraftClient client) {
		return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	public record PreviewSession(String dimension, BlockPos startPos, Direction startFacing, boolean startFacingLocked, RailPlacementMode mode) {
	}
}
