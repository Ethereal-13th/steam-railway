package dev.steamrailway.registry;

import dev.steamrailway.SteamRailwayMod;
import dev.steamrailway.rail.TrackAnchorBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class SRBlocks {
	public static final Block TRACK_ANCHOR = register("track_anchor",
		new TrackAnchorBlock(settings("track_anchor").strength(1.5F)));

	private SRBlocks() {
	}

	public static void register() {
	}

	private static Block register(String path, Block block) {
		return Registry.register(Registries.BLOCK, SteamRailwayMod.id(path), block);
	}

	private static AbstractBlock.Settings settings(String path) {
		Identifier id = SteamRailwayMod.id(path);
		return AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id));
	}
}
