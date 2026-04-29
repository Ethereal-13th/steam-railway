package dev.steamrailway.rail;

import dev.steamrailway.registry.SRBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class RailPlacementToolItem extends Item {
	public RailPlacementToolItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (context.getPlayer() == null) {
			return ActionResult.PASS;
		}
		if (context.getWorld().isClient()) {
			return ActionResult.SUCCESS;
		}

		ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
		if (player.isSneaking()) {
			RailPlacementSessionService.clear(player.getUuid());
			player.sendMessage(Text.translatable("item.steam_railway.rail_placement_tool.selection_cleared"), true);
			return ActionResult.SUCCESS;
		}

		BlockPos startPos = resolveStartPos(context);
		if (startPos == null) {
			player.sendMessage(Text.translatable("item.steam_railway.rail_placement_tool.invalid_start"), true);
			return ActionResult.FAIL;
		}

		RailPlacementSessionService.setStart(player, startPos);
		player.sendMessage(Text.translatable("item.steam_railway.rail_placement_tool.start_set", startPos.toShortString()), true);
		return ActionResult.SUCCESS;
	}

	private BlockPos resolveStartPos(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos clickedPos = context.getBlockPos();
		BlockState clickedState = world.getBlockState(clickedPos);

		if (clickedState.isOf(SRBlocks.STANDARD_RAIL)) {
			return clickedPos;
		}

		if (context.getSide() != Direction.UP) {
			return null;
		}

		BlockPos targetPos = clickedPos.up();
		BlockState targetState = world.getBlockState(targetPos);
		BlockState railState = SRBlocks.STANDARD_RAIL.getDefaultState().with(StandardRailBlock.FACING, context.getHorizontalPlayerFacing());
		if (!targetState.isReplaceable() || !railState.canPlaceAt(world, targetPos)) {
			return null;
		}

		return targetPos;
	}
}
