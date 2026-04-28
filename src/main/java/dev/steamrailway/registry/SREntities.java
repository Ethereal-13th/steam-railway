package dev.steamrailway.registry;

import dev.steamrailway.SteamRailwayMod;
import dev.steamrailway.train.entity.TrainEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class SREntities {
	public static final EntityType<TrainEntity> TRAIN = Registry.register(
		Registries.ENTITY_TYPE,
		SteamRailwayMod.id("train"),
		FabricEntityTypeBuilder.create(SpawnGroup.MISC, TrainEntity::new)
			.dimensions(EntityDimensions.fixed(0.98F, 0.7F))
			.trackRangeBlocks(64)
			.trackedUpdateRate(1)
			.forceTrackedVelocityUpdates(true)
			.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, SteamRailwayMod.id("train"))));

	private SREntities() {
	}

	public static void register() {
	}
}
