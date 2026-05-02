package dev.steamrailway.registry;

import dev.steamrailway.SteamRailwayMod;
import dev.steamrailway.rail.CurveRailSupportBlock;
import dev.steamrailway.rail.StandardRailBlock;
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
	public static final Block STANDARD_RAIL = register("standard_rail",
		new StandardRailBlock(settings("standard_rail").strength(1.0F).nonOpaque()));
	public static final Block CURVE_RAIL_SUPPORT = register("curve_rail_support",
		new CurveRailSupportBlock(settings("curve_rail_support").strength(1.0F).nonOpaque().dropsNothing()));
	public static final Block CURVE_RAIL_RAIL_RENDER = register("curve_rail_rail_render",
		new Block(settings("curve_rail_rail_render").nonOpaque().dropsNothing()));
	public static final Block CURVE_RAIL_TIE_RENDER = register("curve_rail_tie_render",
		new Block(settings("curve_rail_tie_render").nonOpaque().dropsNothing()));

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
