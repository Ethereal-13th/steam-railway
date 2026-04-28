package dev.steamrailway;

import dev.steamrailway.debug.DebugPathRenderer;
import dev.steamrailway.registry.SRBlocks;
import dev.steamrailway.registry.SRCommands;
import dev.steamrailway.registry.SREntities;
import dev.steamrailway.registry.SRItems;
import dev.steamrailway.train.TrainRuntimeService;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SteamRailwayMod implements ModInitializer {
	public static final String MOD_ID = "steam_railway";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		SRBlocks.register();
		SREntities.register();
		SRItems.register();
		SRCommands.register();
		DebugPathRenderer.register();
		TrainRuntimeService.register();
		LOGGER.info("steam_railway initialized");
	}
}
