package dev.steamrailway.registry;

import dev.steamrailway.SteamRailwayMod;
import dev.steamrailway.rail.RailPlacementToolItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class SRItems {
	public static final Item TRACK_ANCHOR = registerBlockItem("track_anchor", SRBlocks.TRACK_ANCHOR, blockItemSettings("track_anchor"));
	public static final Item STANDARD_RAIL = registerBlockItem("standard_rail", SRBlocks.STANDARD_RAIL, blockItemSettings("standard_rail"));
	public static final Item RAIL_PLACEMENT_TOOL = register("rail_placement_tool", new RailPlacementToolItem(itemSettings("rail_placement_tool").maxCount(1)));

	private SRItems() {
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> entries.add(TRACK_ANCHOR));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(STANDARD_RAIL));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(RAIL_PLACEMENT_TOOL));
	}

	private static Item registerBlockItem(String path, Block block, Item.Settings settings) {
		return Registry.register(Registries.ITEM, SteamRailwayMod.id(path), new BlockItem(block, settings));
	}

	private static Item register(String path, Item item) {
		return Registry.register(Registries.ITEM, SteamRailwayMod.id(path), item);
	}

	private static Item.Settings blockItemSettings(String path) {
		Identifier id = SteamRailwayMod.id(path);
		return new Item.Settings()
			.useBlockPrefixedTranslationKey()
			.registryKey(RegistryKey.of(RegistryKeys.ITEM, id));
	}

	private static Item.Settings itemSettings(String path) {
		Identifier id = SteamRailwayMod.id(path);
		return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id));
	}
}
