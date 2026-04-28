package dev.steamrailway.train.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

public final class TrainEntity extends MinecartEntity {
	private String logicalTrainId = "";

	public TrainEntity(EntityType<? extends TrainEntity> entityType, World world) {
		super(entityType, world);
		setNoGravity(true);
		setCustomBlockState(Optional.of(Blocks.REDSTONE_BLOCK.getDefaultState()));
	}

	@Override
	public void tick() {
		if (getEntityWorld().isClient()) {
			super.tick();
			return;
		}

		super.baseTick();
		setVelocity(Vec3d.ZERO);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	protected void writeCustomData(WriteView writeView) {
		writeView.putString("logical_train_id", logicalTrainId);
	}

	@Override
	protected void readCustomData(ReadView readView) {
		logicalTrainId = readView.getString("logical_train_id", "");
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.MINECART);
	}

	public void setLogicalTrainId(String logicalTrainId) {
		this.logicalTrainId = logicalTrainId;
	}

	public String getLogicalTrainId() {
		return logicalTrainId;
	}
}
