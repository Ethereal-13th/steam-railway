package dev.steamrailway;

import dev.steamrailway.client.ClientRailPlacementPreviewState;
import dev.steamrailway.client.PlacedRailCurveRenderer;
import dev.steamrailway.client.RailPlacementPreviewRenderer;
import dev.steamrailway.registry.SREntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.MinecartEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;

public final class SteamRailwayClientMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SteamRailwayMod.LOGGER.info("steam_railway client initialized");
		EntityRendererRegistry.register(SREntities.TRAIN, context -> new MinecartEntityRenderer(context, EntityModelLayers.MINECART));
		ClientRailPlacementPreviewState.register();
		RailPlacementPreviewRenderer.register();
		PlacedRailCurveRenderer.register();
	}
}
