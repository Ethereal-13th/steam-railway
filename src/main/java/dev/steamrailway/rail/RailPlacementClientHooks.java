package dev.steamrailway.rail;

import net.minecraft.item.ItemUsageContext;

public final class RailPlacementClientHooks {
	private static ClientHandler handler = context -> {
	};

	private RailPlacementClientHooks() {
	}

	public static void register(ClientHandler handler) {
		RailPlacementClientHooks.handler = handler;
	}

	public static void handleUse(ItemUsageContext context) {
		handler.handleUse(context);
	}

	@FunctionalInterface
	public interface ClientHandler {
		void handleUse(ItemUsageContext context);
	}
}
