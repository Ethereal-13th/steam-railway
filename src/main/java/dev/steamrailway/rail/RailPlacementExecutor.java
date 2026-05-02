package dev.steamrailway.rail;

import dev.steamrailway.persistence.SRSaveData;
import dev.steamrailway.registry.SRBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RailPlacementExecutor {
	private RailPlacementExecutor() {
	}

	public static ExecutionResult execute(ServerWorld world, RailPlacementPlan plan) {
		Map<BlockPos, BlockState> previousStates = capturePreviousStates(world, plan);
		List<BlockPos> changedPositions = new ArrayList<>();

		for (RailPlannedPlacement placement : plan.railPlacements()) {
			if (!applyState(world, placement.pos(), placement.state(), previousStates, changedPositions)) {
				rollback(world, previousStates, changedPositions);
				return ExecutionResult.failure("item.steam_railway.rail_placement_tool.place_failed", placement.pos().toShortString());
			}
		}
		for (BlockPos supportPos : plan.supportPositions()) {
			if (!applyState(world, supportPos, SRBlocks.CURVE_RAIL_SUPPORT.getDefaultState(), previousStates, changedPositions)) {
				rollback(world, previousStates, changedPositions);
				return ExecutionResult.failure("item.steam_railway.rail_placement_tool.place_failed", supportPos.toShortString());
			}
		}

		StandardRailBlockEntity startEntity = null;
		StandardRailBlockEntity endEntity = null;
		if (plan.curve()) {
			startEntity = ensureRailBlockEntity(world, plan.curveStartPos());
			endEntity = ensureRailBlockEntity(world, plan.curveEndPos());
			if (startEntity == null || endEntity == null) {
				rollback(world, previousStates, changedPositions);
				return ExecutionResult.failure("item.steam_railway.rail_placement_tool.place_failed");
			}
		}

		CurveRailRemovalService.removeEdgesTouchingPositions(world, plan.affectedPositions());

		SRSaveData saveData = SRSaveData.get(world.getServer());
		RailNetwork network = saveData.railNetwork();
		if (plan.curve()) {
			removeExistingEdge(network, startEntity.getConnection(plan.curveEndPos()));
			removeExistingEdge(network, endEntity.getConnection(plan.curveStartPos()));
		}

		RailNode fromNode = network.createPlacedNode(world, plan.startPos());
		RailNode toNode = network.createPlacedNode(world, plan.endPos());
		String edgeId = network.nextEdgeId();
		RailEdge edge = RailPathGenerator.createEdgeFromSamples(edgeId, fromNode, toNode, plan.toolPath().samples());
		network.putEdge(edge);

		if (plan.curve()) {
			boolean startIsPrimary = compareBlockPos(plan.curveStartPos(), plan.curveEndPos()) < 0;
			CurveRailConnection startConnection = new CurveRailConnection(edgeId, plan.curveEndPos(), plan.toolPath().renderSamples(), startIsPrimary);
			CurveRailConnection endConnection = startConnection.reversed(plan.curveStartPos(), !startIsPrimary);
			startEntity.putConnection(startConnection);
			endEntity.putConnection(endConnection);
			for (BlockPos supportPos : plan.supportPositions()) {
				if (world.getBlockEntity(supportPos) instanceof CurveRailSupportBlockEntity supportBlockEntity) {
					supportBlockEntity.setCurveData(edgeId, plan.curveStartPos(), plan.curveEndPos());
				}
			}
		}

		saveData.markDirty();
		return ExecutionResult.success(plan.placedRailCount(), edge.id());
	}

	private static Map<BlockPos, BlockState> capturePreviousStates(ServerWorld world, RailPlacementPlan plan) {
		Map<BlockPos, BlockState> previousStates = new LinkedHashMap<>();
		for (RailPlannedPlacement placement : plan.railPlacements()) {
			BlockState existing = world.getBlockState(placement.pos());
			if (!existing.equals(placement.state()) || existing.isOf(SRBlocks.STANDARD_RAIL) || existing.isOf(SRBlocks.CURVE_RAIL_SUPPORT)) {
				previousStates.put(placement.pos(), existing);
			}
		}
		for (BlockPos supportPos : plan.supportPositions()) {
			BlockState existing = world.getBlockState(supportPos);
			if (!existing.equals(SRBlocks.CURVE_RAIL_SUPPORT.getDefaultState())) {
				previousStates.put(supportPos, existing);
			}
		}
		return previousStates;
	}

	private static boolean applyState(
		ServerWorld world,
		BlockPos pos,
		BlockState desiredState,
		Map<BlockPos, BlockState> previousStates,
		List<BlockPos> changedPositions
	) {
		if (!previousStates.containsKey(pos)) {
			return true;
		}
		if (!world.setBlockState(pos, desiredState, Block.NOTIFY_ALL)) {
			return false;
		}
		changedPositions.add(pos);
		return true;
	}

	private static void rollback(ServerWorld world, Map<BlockPos, BlockState> previousStates, List<BlockPos> changedPositions) {
		for (int index = changedPositions.size() - 1; index >= 0; index--) {
			BlockPos pos = changedPositions.get(index);
			world.setBlockState(pos, previousStates.get(pos), Block.NOTIFY_ALL);
		}
	}

	private static StandardRailBlockEntity ensureRailBlockEntity(ServerWorld world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof StandardRailBlockEntity railBlockEntity) {
			return railBlockEntity;
		}

		BlockState state = world.getBlockState(pos);
		if (!state.isOf(SRBlocks.STANDARD_RAIL)) {
			return null;
		}

		world.setBlockState(pos, state, Block.NOTIFY_ALL);
		return world.getBlockEntity(pos) instanceof StandardRailBlockEntity railBlockEntity
			? railBlockEntity
			: null;
	}

	private static void removeExistingEdge(RailNetwork network, CurveRailConnection existingConnection) {
		if (existingConnection == null || existingConnection.edgeId().isBlank()) {
			return;
		}
		network.removeEdge(existingConnection.edgeId());
	}

	private static int compareBlockPos(BlockPos first, BlockPos second) {
		if (first.getY() != second.getY()) {
			return Integer.compare(first.getY(), second.getY());
		}
		if (first.getX() != second.getX()) {
			return Integer.compare(first.getX(), second.getX());
		}
		return Integer.compare(first.getZ(), second.getZ());
	}

	public record ExecutionResult(boolean success, String messageKey, Object[] messageArgs) {
		public static ExecutionResult success(Object... messageArgs) {
			return new ExecutionResult(true, "item.steam_railway.rail_placement_tool.place_success", messageArgs);
		}

		public static ExecutionResult failure(String messageKey, Object... messageArgs) {
			return new ExecutionResult(false, messageKey, messageArgs);
		}
	}
}
