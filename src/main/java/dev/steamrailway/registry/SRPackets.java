package dev.steamrailway.registry;

import dev.steamrailway.net.RailPlacementModePayload;
import dev.steamrailway.rail.RailPlacementMode;
import dev.steamrailway.rail.RailPlacementSessionService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class SRPackets {
	private SRPackets() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(RailPlacementModePayload.ID, RailPlacementModePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RailPlacementModePayload.ID, (payload, context) ->
			context.server().execute(() ->
				RailPlacementSessionService.updateMode(context.player(), RailPlacementMode.fromGentle(payload.gentle()))));
	}
}
