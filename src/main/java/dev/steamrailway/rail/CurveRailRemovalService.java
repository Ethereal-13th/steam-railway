package dev.steamrailway.rail;

import dev.steamrailway.persistence.SRSaveData;
import dev.steamrailway.registry.SRBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CurveRailRemovalService {
	private static final ThreadLocal<Set<String>> REMOVING_EDGES = ThreadLocal.withInitial(HashSet::new);

	private CurveRailRemovalService() {
	}

	public static boolean isRemoving(String edgeId) {
		return REMOVING_EDGES.get().contains(edgeId);
	}

	public static void removeCurve(ServerWorld world, String edgeId, BlockPos startPos, BlockPos endPos) {
		if (edgeId == null || edgeId.isBlank()) {
			return;
		}

		Set<String> removingEdges = REMOVING_EDGES.get();
		if (!removingEdges.add(edgeId)) {
			return;
		}

		try {
			removeStoredEdge(world, edgeId);
			removeConnection(world, startPos, endPos);
			removeConnection(world, endPos, startPos);
		} finally {
			removingEdges.remove(edgeId);
		}
	}

	public static void removeEdge(ServerWorld world, String edgeId) {
		if (edgeId == null || edgeId.isBlank()) {
			return;
		}

		Set<String> removingEdges = REMOVING_EDGES.get();
		if (!removingEdges.add(edgeId)) {
			return;
		}

		try {
			removeStoredEdge(world, edgeId);
		} finally {
			removingEdges.remove(edgeId);
		}
	}

	public static void removeEdgesTouchingPositions(ServerWorld world, Iterable<BlockPos> positions) {
		LinkedHashSet<BlockPos> targetPositions = new LinkedHashSet<>();
		for (BlockPos pos : positions) {
			targetPositions.add(pos.toImmutable());
		}
		if (targetPositions.isEmpty()) {
			return;
		}

		SRSaveData saveData = SRSaveData.get(world.getServer());
		RailNetwork network = saveData.railNetwork();
		LinkedHashSet<String> affectedEdgeIds = new LinkedHashSet<>();
		for (RailEdge edge : network.edges().values()) {
			if (edgeTouchesAny(edge, targetPositions)) {
				affectedEdgeIds.add(edge.id());
			}
		}

		for (String edgeId : affectedEdgeIds) {
			removeEdge(world, edgeId);
		}
	}

	public static void removeConnectionsForRail(ServerWorld world, StandardRailBlockEntity railBlockEntity) {
		for (CurveRailConnection connection : List.copyOf(railBlockEntity.getConnections())) {
			CurveRailRemovalService.removeCurve(world, connection.edgeId(), railBlockEntity.getPos(), connection.targetPos());
		}
	}

	private static void removeStoredEdge(ServerWorld world, String edgeId) {
		SRSaveData saveData = SRSaveData.get(world.getServer());
		RailNetwork network = saveData.railNetwork();
		RailEdge edge = network.getEdge(edgeId);
		if (edge == null) {
			return;
		}

		clearSupportBlocks(world, edge.samples(), edgeId);
		removeConnectionsForEdge(world, edge.samples(), edgeId);
		network.removeEdge(edgeId);
		saveData.markDirty();
	}

	private static boolean edgeTouchesAny(RailEdge edge, Set<BlockPos> targetPositions) {
		Set<BlockPos> seen = new HashSet<>();
		for (RailSample sample : edge.samples()) {
			BlockPos samplePos = BlockPos.ofFloored(sample.position());
			if (!seen.add(samplePos)) {
				continue;
			}
			if (targetPositions.contains(samplePos)) {
				return true;
			}
		}
		return false;
	}

	private static void removeConnectionsForEdge(ServerWorld world, List<RailSample> samples, String edgeId) {
		Set<BlockPos> seen = new HashSet<>();
		for (RailSample sample : samples) {
			BlockPos pos = BlockPos.ofFloored(sample.position());
			if (!seen.add(pos)) {
				continue;
			}
			if (!(world.getBlockEntity(pos) instanceof StandardRailBlockEntity railBlockEntity)) {
				continue;
			}
			for (CurveRailConnection connection : List.copyOf(railBlockEntity.getConnections())) {
				if (edgeId.equals(connection.edgeId())) {
					railBlockEntity.removeConnection(connection.targetPos());
				}
			}
		}
	}

	private static void clearSupportBlocks(ServerWorld world, List<RailSample> samples, String edgeId) {
		Set<BlockPos> seen = new HashSet<>();
		for (RailSample sample : samples) {
			BlockPos pos = BlockPos.ofFloored(sample.position());
			if (!seen.add(pos)) {
				continue;
			}

			if (!world.getBlockState(pos).isOf(SRBlocks.CURVE_RAIL_SUPPORT)) {
				continue;
			}
			if (world.getBlockEntity(pos) instanceof CurveRailSupportBlockEntity support && edgeId.equals(support.edgeId())) {
				world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
	}

	private static void removeConnection(ServerWorld world, BlockPos fromPos, BlockPos toPos) {
		if (world.getBlockEntity(fromPos) instanceof StandardRailBlockEntity railBlockEntity) {
			railBlockEntity.removeConnection(toPos);
		}
	}
}
