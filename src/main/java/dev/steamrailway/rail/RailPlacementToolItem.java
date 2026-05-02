package dev.steamrailway.rail;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

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
			RailPlacementClientHooks.handleUse(context);
			return ActionResult.SUCCESS;
		}

		ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
		if (player.isSneaking()) {
			RailPlacementSessionService.clear(player.getUuid());
			player.sendMessage(Text.translatable("item.steam_railway.rail_placement_tool.selection_cleared"), true);
			return ActionResult.SUCCESS;
		}

		Optional<RailPlacementSessionService.RailPlacementSession> session = RailPlacementSessionService.get(player);

		RailPlacementTargetResolver.RailPlacementTarget target = RailPlacementTargetResolver.resolveTarget(
			context.getWorld(),
			context.getBlockPos(),
			context.getSide(),
			context.getHorizontalPlayerFacing()
		).orElse(null);
		if (target == null) {
			String messageKey = session.isPresent()
				? "item.steam_railway.rail_placement_tool.invalid_end"
				: "item.steam_railway.rail_placement_tool.invalid_start";
			player.sendMessage(Text.translatable(messageKey), true);
			return ActionResult.FAIL;
		}

		if (session.isPresent()) {
			RailPlacementService.PlacementResult result = RailPlacementService.tryPlace(player, session.get(), target);
			if (result.clearSelection()) {
				RailPlacementSessionService.clear(player.getUuid());
			}
			player.sendMessage(Text.translatable(result.messageKey(), result.messageArgs()), true);
			return result.success() ? ActionResult.SUCCESS : ActionResult.FAIL;
		}

		BlockPos startPos = target.pos();
		RailPlacementSessionService.setStart(player, startPos, target.facing(), target.lockedFacing());
		player.sendMessage(Text.translatable("item.steam_railway.rail_placement_tool.start_set", startPos.toShortString()), true);
		return ActionResult.SUCCESS;
	}
}
