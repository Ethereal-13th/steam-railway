package dev.steamrailway.rail;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class RailPlacementService {
	private RailPlacementService() {
	}

	public static PlacementResult tryPlace(
		ServerPlayerEntity player,
		RailPlacementSessionService.RailPlacementSession session,
		RailPlacementTargetResolver.RailPlacementTarget endTarget
	) {
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		String worldDimension = world.getRegistryKey().getValue().toString();
		if (!session.dimension().equals(worldDimension)) {
			return PlacementResult.failure(
				"item.steam_railway.rail_placement_tool.dimension_mismatch",
				true);
		}

		RailPlacementPlanner.PlanningResult planning = RailPlacementPlanner.plan(session, endTarget);
		if (!planning.success()) {
			return PlacementResult.failure(planning.messageKey(), false, planning.messageArgs());
		}

		RailPlacementValidator.ValidationError validationError = RailPlacementValidator.validate(world, planning.plan());
		if (validationError != null) {
			if (validationError.pos() == null) {
				return PlacementResult.failure(validationError.messageKey(), false);
			}
			return PlacementResult.failure(validationError.messageKey(), false, validationError.pos().toShortString());
		}

		RailPlacementExecutor.ExecutionResult execution = RailPlacementExecutor.execute(world, planning.plan());
		return execution.success()
			? PlacementResult.success(execution.messageKey(), true, execution.messageArgs())
			: PlacementResult.failure(execution.messageKey(), false, execution.messageArgs());
	}

	public record PlacementResult(boolean success, boolean clearSelection, String messageKey, Object[] messageArgs) {
		public static PlacementResult success(String messageKey, boolean clearSelection, Object... messageArgs) {
			return new PlacementResult(true, clearSelection, messageKey, messageArgs);
		}

		public static PlacementResult failure(String messageKey, boolean clearSelection, Object... messageArgs) {
			return new PlacementResult(false, clearSelection, messageKey, messageArgs);
		}
	}
}
