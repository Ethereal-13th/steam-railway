package dev.steamrailway.rail;

import dev.steamrailway.registry.SRBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

public final class CurveRailSupportBlockEntity extends BlockEntity {
	private static final String EDGE_ID_KEY = "edge_id";
	private static final String START_POS_KEY = "start_pos";
	private static final String END_POS_KEY = "end_pos";

	private String edgeId = "";
	private BlockPos startPos = BlockPos.ORIGIN;
	private BlockPos endPos = BlockPos.ORIGIN;

	public CurveRailSupportBlockEntity(BlockPos pos, BlockState state) {
		super(SRBlockEntities.CURVE_RAIL_SUPPORT, pos, state);
	}

	public String edgeId() {
		return edgeId;
	}

	public BlockPos startPos() {
		return startPos;
	}

	public BlockPos endPos() {
		return endPos;
	}

	public void setCurveData(String edgeId, BlockPos startPos, BlockPos endPos) {
		this.edgeId = edgeId;
		this.startPos = startPos;
		this.endPos = endPos;
		markDirty();
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putString(EDGE_ID_KEY, edgeId);
		view.putLong(START_POS_KEY, startPos.asLong());
		view.putLong(END_POS_KEY, endPos.asLong());
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		edgeId = view.getString(EDGE_ID_KEY, "");
		startPos = BlockPos.fromLong(view.getLong(START_POS_KEY, BlockPos.ORIGIN.asLong()));
		endPos = BlockPos.fromLong(view.getLong(END_POS_KEY, BlockPos.ORIGIN.asLong()));
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createNbt(registries);
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}
}
