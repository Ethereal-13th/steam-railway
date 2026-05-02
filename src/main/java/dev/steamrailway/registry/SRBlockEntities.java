package dev.steamrailway.registry;

import dev.steamrailway.SteamRailwayMod;
import dev.steamrailway.rail.CurveRailSupportBlockEntity;
import dev.steamrailway.rail.StandardRailBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class SRBlockEntities {
	public static final BlockEntityType<StandardRailBlockEntity> STANDARD_RAIL = Registry.register(
		Registries.BLOCK_ENTITY_TYPE,
		SteamRailwayMod.id("standard_rail"),
		FabricBlockEntityTypeBuilder.create(StandardRailBlockEntity::new, SRBlocks.STANDARD_RAIL).build());
	public static final BlockEntityType<CurveRailSupportBlockEntity> CURVE_RAIL_SUPPORT = Registry.register(
		Registries.BLOCK_ENTITY_TYPE,
		SteamRailwayMod.id("curve_rail_support"),
		FabricBlockEntityTypeBuilder.create(CurveRailSupportBlockEntity::new, SRBlocks.CURVE_RAIL_SUPPORT).build());

	private SRBlockEntities() {
	}

	public static void register() {
	}
}
