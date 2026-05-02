package dev.steamrailway.rail;

import dev.steamrailway.registry.SRBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RailPlacementPlanner {
	private RailPlacementPlanner() {
	}

	public static PlanningResult plan(
		RailPlacementSessionService.RailPlacementSession session,
		RailPlacementTargetResolver.RailPlacementTarget endTarget
	) {
		RailPathGenerator.ToolPath toolPath = RailPathGenerator.createToolPath(
			session.startPos(),
			session.startFacing(),
			session.startFacingLocked(),
			endTarget.pos(),
			endTarget.facing(),
			endTarget.lockedFacing(),
			session.mode().gentle());
		if (toolPath.kind() == RailPathGenerator.ToolPathKind.NONE || toolPath.samples().size() < 2) {
			return PlanningResult.failure("item.steam_railway.rail_placement_tool.invalid_path");
		}
		if (!toolPath.valid()) {
			return PlanningResult.failure("item.steam_railway.rail_placement_tool.curve_too_tight");
		}

		BlockPos startPos = session.startPos();
		BlockPos endPos = endTarget.pos();
		double directDistance = RailGeometry.worldPosition(startPos).distanceTo(RailGeometry.worldPosition(endPos));

		List<RailPlannedPlacement> railPlacements = buildRailPlacements(startPos, endPos, toolPath);
		if (railPlacements.isEmpty()) {
			return PlanningResult.failure("item.steam_railway.rail_placement_tool.invalid_path");
		}

		List<BlockPos> supportPositions = List.of();
		if (toolPath.kind() == RailPathGenerator.ToolPathKind.CURVE) {
			BlockPos curveStartPos = startPos.offset(toolPath.startFacing(), toolPath.startExtent());
			BlockPos curveEndPos = endPos.offset(toolPath.endFacing().getOpposite(), toolPath.endExtent());
			supportPositions = collectCurveSupportPositions(curveStartPos, curveEndPos, toolPath.renderSamples());
		}

		LinkedHashSet<BlockPos> affectedPositions = new LinkedHashSet<>();
		for (RailPlannedPlacement placement : railPlacements) {
			affectedPositions.add(placement.pos());
		}
		affectedPositions.addAll(supportPositions);

		return PlanningResult.success(new RailPlacementPlan(
			startPos,
			endPos,
			directDistance,
			toolPath,
			railPlacements,
			supportPositions,
			List.copyOf(affectedPositions)));
	}

	private static List<RailPlannedPlacement> buildRailPlacements(BlockPos startPos, BlockPos endPos, RailPathGenerator.ToolPath toolPath) {
		if (toolPath.kind() == RailPathGenerator.ToolPathKind.STRAIGHT) {
			return buildStraightPlacements(startPos, endPos, toolPath.startFacing());
		}

		BlockPos curveStartPos = startPos.offset(toolPath.startFacing(), toolPath.startExtent());
		Direction endLeadFacing = toolPath.endFacing().getOpposite();
		BlockPos curveEndPos = endPos.offset(endLeadFacing, toolPath.endExtent());
		List<RailPlannedPlacement> railPlacements = new ArrayList<>();
		addPlacements(railPlacements, buildStraightPlacements(startPos, curveStartPos, toolPath.startFacing()));
		addPlacements(railPlacements, buildStraightPlacements(endPos, curveEndPos, endLeadFacing));
		return railPlacements;
	}

	private static List<RailPlannedPlacement> buildStraightPlacements(BlockPos startPos, BlockPos endPos, Direction facing) {
		int steps = Math.abs(switch (facing.getAxis()) {
			case X -> endPos.getX() - startPos.getX();
			case Z -> endPos.getZ() - startPos.getZ();
			default -> 0;
		});

		List<RailPlannedPlacement> placements = new ArrayList<>(steps + 1);
		BlockPos.Mutable cursor = startPos.mutableCopy();
		for (int step = 0; step <= steps; step++) {
			appendPlacementIfAbsent(placements, new RailPlannedPlacement(cursor.toImmutable(), stateFor(facing)));
			cursor.move(facing);
		}
		return placements;
	}

	private static void addPlacements(List<RailPlannedPlacement> target, List<RailPlannedPlacement> additions) {
		for (RailPlannedPlacement placement : additions) {
			appendPlacementIfAbsent(target, placement);
		}
	}

	private static void appendPlacementIfAbsent(List<RailPlannedPlacement> placements, RailPlannedPlacement placement) {
		if (!placements.isEmpty() && placements.getLast().pos().equals(placement.pos())) {
			placements.set(placements.size() - 1, placement);
			return;
		}
		for (int index = 0; index < placements.size(); index++) {
			if (placements.get(index).pos().equals(placement.pos())) {
				placements.set(index, placement);
				return;
			}
		}
		placements.add(placement);
	}

	private static List<BlockPos> collectCurveSupportPositions(BlockPos startPos, BlockPos endPos, List<RailSample> samples) {
		List<BlockPos> positions = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		for (RailSample sample : samples) {
			BlockPos pos = RailGeometry.sampleToBlockPos(sample.position());
			if (!seen.add(pos) || pos.equals(startPos) || pos.equals(endPos)) {
				continue;
			}
			positions.add(pos);
		}
		return positions;
	}

	private static BlockState stateFor(Direction facing) {
		return SRBlocks.STANDARD_RAIL.getDefaultState().with(StandardRailBlock.FACING, facing);
	}

	public record PlanningResult(boolean success, RailPlacementPlan plan, String messageKey, Object[] messageArgs) {
		public static PlanningResult success(RailPlacementPlan plan) {
			return new PlanningResult(true, plan, "", new Object[0]);
		}

		public static PlanningResult failure(String messageKey, Object... messageArgs) {
			return new PlanningResult(false, null, messageKey, messageArgs);
		}
	}
}
