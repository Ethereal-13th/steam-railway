package dev.steamrailway.rail;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public record RailPlacementPlan(
	BlockPos startPos,
	BlockPos endPos,
	double directDistance,
	RailPathGenerator.ToolPath toolPath,
	List<RailPlannedPlacement> railPlacements,
	List<BlockPos> supportPositions,
	List<BlockPos> affectedPositions
) {
	public RailPlacementPlan {
		railPlacements = List.copyOf(railPlacements);
		supportPositions = List.copyOf(supportPositions);
		affectedPositions = List.copyOf(affectedPositions);
	}

	public boolean curve() {
		return toolPath.kind() == RailPathGenerator.ToolPathKind.CURVE;
	}

	public BlockPos curveStartPos() {
		return startPos.offset(toolPath.startFacing(), toolPath.startExtent());
	}

	public BlockPos curveEndPos() {
		return endPos.offset(toolPath.endFacing().getOpposite(), toolPath.endExtent());
	}

	public int placedRailCount() {
		return railPlacements.size();
	}
}
