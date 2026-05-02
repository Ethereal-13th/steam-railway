package dev.steamrailway.rail;

import dev.steamrailway.registry.SRBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

public final class RailPlacementTargetResolver {
	private RailPlacementTargetResolver() {
	}

	public static Optional<RailPlacementTarget> resolveTarget(World world, BlockPos clickedPos, Direction side, Direction playerFacing) {
		BlockState clickedState = world.getBlockState(clickedPos);
		if (clickedState.isOf(SRBlocks.STANDARD_RAIL)) {
			return Optional.of(new RailPlacementTarget(clickedPos, clickedState.get(StandardRailBlock.FACING), true));
		}

		if (side != Direction.UP) {
			return Optional.empty();
		}

		BlockPos targetPos = clickedPos.up();
		BlockState targetState = world.getBlockState(targetPos);
		BlockState railState = SRBlocks.STANDARD_RAIL.getDefaultState().with(StandardRailBlock.FACING, playerFacing);
		if (!targetState.isReplaceable() || !railState.canPlaceAt(world, targetPos)) {
			return Optional.empty();
		}

		return Optional.of(new RailPlacementTarget(targetPos, playerFacing, false));
	}

	public static Vec3d worldPosition(BlockPos pos) {
		return RailGeometry.worldPosition(pos);
	}

	public static Direction resolveStraightDirection(
		BlockPos startPos,
		Direction startFacing,
		boolean startFacingLocked,
		BlockPos endPos,
		Direction endFacing,
		boolean endFacingLocked
	) {
		if (startPos.getY() != endPos.getY()) {
			return null;
		}

		int deltaX = endPos.getX() - startPos.getX();
		int deltaZ = endPos.getZ() - startPos.getZ();
		if ((deltaX == 0 && deltaZ == 0) || (deltaX != 0 && deltaZ != 0)) {
			return null;
		}

		Direction direction = deltaX > 0
			? Direction.EAST
			: deltaX < 0
				? Direction.WEST
				: deltaZ > 0
					? Direction.SOUTH
					: Direction.NORTH;

		if (startFacingLocked && direction.getAxis() != startFacing.getAxis()) {
			return null;
		}
		if (endFacingLocked && direction.getAxis() != endFacing.getAxis()) {
			return null;
		}

		return direction;
	}

	public record RailPlacementTarget(BlockPos pos, Direction facing, boolean lockedFacing) {
	}
}
