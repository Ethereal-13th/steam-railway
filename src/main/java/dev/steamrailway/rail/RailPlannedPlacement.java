package dev.steamrailway.rail;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

record RailPlannedPlacement(BlockPos pos, BlockState state) {
	RailPlannedPlacement withFacing(Direction facing) {
		return new RailPlannedPlacement(pos, state.with(StandardRailBlock.FACING, facing));
	}
}
