package dev.steamrailway.registry;

import dev.steamrailway.SteamRailwayMod;
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
	public static final Item TRACK_ANCHOR = registerBlockItem("track_anchor", SRBlocks.TRACK_ANCHOR, settings("track_anchor"));

	private SRItems() {
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> entries.add(TRACK_ANCHOR));
	}

	private static Item registerBlockItem(String path, Block block, Item.Settings settings) {
		return Registry.register(Registries.ITEM, SteamRailwayMod.id(path), new BlockItem(block, settings));
	}

	private static Item.Settings settings(String path) {
		Identifier id = SteamRailwayMod.id(path);
		return new Item.Settings()
			.useBlockPrefixedTranslationKey()
			.registryKey(RegistryKey.of(RegistryKeys.ITEM, id));
	}
}
