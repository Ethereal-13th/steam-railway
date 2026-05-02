package dev.steamrailway.rail;

import dev.steamrailway.registry.SRBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class RailPlacementValidator {
	private RailPlacementValidator() {
	}

	public static ValidationError validate(ServerWorld world, RailPlacementPlan plan) {
		if (plan.directDistance() < RailPathGenerator.MIN_PATH_DISTANCE) {
			return new ValidationError("item.steam_railway.rail_placement_tool.too_close", null);
		}
		if (plan.directDistance() > RailPathGenerator.MAX_PATH_DISTANCE) {
			return new ValidationError("item.steam_railway.rail_placement_tool.too_far", null);
		}

		ValidationError placementError = validatePlacements(world, plan.railPlacements());
		if (placementError != null) {
			return placementError;
		}

		if (plan.curve()) {
			return validateCurveFootprint(world, plan.supportPositions());
		}

		return null;
	}

	private static ValidationError validatePlacements(ServerWorld world, List<RailPlannedPlacement> placements) {
		for (RailPlannedPlacement placement : placements) {
			BlockState desiredState = placement.state();
			BlockPos pos = placement.pos();
			if (!desiredState.canPlaceAt(world, pos)) {
				return new ValidationError("item.steam_railway.rail_placement_tool.no_support", pos);
			}

			BlockState existing = world.getBlockState(pos);
			if (existing.isOf(SRBlocks.STANDARD_RAIL)) {
				if (existing.get(StandardRailBlock.FACING).getAxis() != desiredState.get(StandardRailBlock.FACING).getAxis()) {
					return new ValidationError("item.steam_railway.rail_placement_tool.blocked", pos);
				}
				continue;
			}
			if (existing.isOf(SRBlocks.CURVE_RAIL_SUPPORT)) {
				continue;
			}
			if (!existing.isReplaceable()) {
				return new ValidationError("item.steam_railway.rail_placement_tool.blocked", pos);
			}
		}

		return null;
	}

	private static ValidationError validateCurveFootprint(ServerWorld world, List<BlockPos> supportPositions) {
		for (BlockPos pos : supportPositions) {
			if (!SRBlocks.CURVE_RAIL_SUPPORT.getDefaultState().canPlaceAt(world, pos)) {
				return new ValidationError("item.steam_railway.rail_placement_tool.no_support", pos);
			}
			BlockState state = world.getBlockState(pos);
			if (state.isOf(SRBlocks.CURVE_RAIL_SUPPORT)) {
				continue;
			}
			if (!state.isReplaceable()) {
				return new ValidationError("item.steam_railway.rail_placement_tool.blocked", pos);
			}
		}
		return null;
	}

	public record ValidationError(String messageKey, BlockPos pos) {
	}
}
