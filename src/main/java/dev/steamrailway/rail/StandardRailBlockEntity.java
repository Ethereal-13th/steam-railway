package dev.steamrailway.rail;

import dev.steamrailway.registry.SRBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StandardRailBlockEntity extends BlockEntity {
	private static final String CONNECTIONS_KEY = "connections";

	private final LinkedHashMap<BlockPos, CurveRailConnection> connections = new LinkedHashMap<>();

	public StandardRailBlockEntity(BlockPos pos, BlockState state) {
		super(SRBlockEntities.STANDARD_RAIL, pos, state);
	}

	public Collection<CurveRailConnection> getConnections() {
		return Collections.unmodifiableCollection(connections.values());
	}

	public Map<BlockPos, CurveRailConnection> getConnectionsByTarget() {
		return Collections.unmodifiableMap(connections);
	}

	public CurveRailConnection getConnection(BlockPos targetPos) {
		return connections.get(targetPos);
	}

	public boolean hasPrimaryConnections() {
		for (CurveRailConnection connection : connections.values()) {
			if (connection.primary()) {
				return true;
			}
		}
		return false;
	}

	public void putConnection(CurveRailConnection connection) {
		connections.put(connection.targetPos(), connection);
		if (world != null && world.isClient()) {
			ClientCurveRenderRegistry.put(connection);
		}
		markDirtyAndSync();
	}

	public CurveRailConnection removeConnection(BlockPos targetPos) {
		CurveRailConnection removed = connections.remove(targetPos);
		if (removed != null) {
			if (world != null && world.isClient()) {
				ClientCurveRenderRegistry.remove(removed);
			}
			markDirtyAndSync();
		}
		return removed;
	}

	private void markDirtyAndSync() {
		markDirty();
		if (world instanceof ServerWorld serverWorld) {
			serverWorld.getChunkManager().markForUpdate(pos);
		}
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		WriteView.ListView list = view.getList(CONNECTIONS_KEY);
		for (CurveRailConnection connection : connections.values()) {
			connection.writeData(list.add());
		}
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		if (world != null && world.isClient()) {
			for (CurveRailConnection connection : connections.values()) {
				ClientCurveRenderRegistry.remove(connection);
			}
		}
		connections.clear();
		view.getListReadView(CONNECTIONS_KEY).stream().forEach(connectionView -> {
			CurveRailConnection connection = CurveRailConnection.fromReadView(connectionView);
			connections.put(connection.targetPos(), connection);
			if (world != null && world.isClient()) {
				ClientCurveRenderRegistry.put(connection);
			}
		});
	}

	@Override
	public void markRemoved() {
		if (world != null && world.isClient()) {
			for (CurveRailConnection connection : connections.values()) {
				ClientCurveRenderRegistry.remove(connection);
			}
		}
		super.markRemoved();
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
