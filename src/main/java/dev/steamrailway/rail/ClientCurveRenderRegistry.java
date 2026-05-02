package dev.steamrailway.rail;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientCurveRenderRegistry {
	private static final LinkedHashMap<String, CurveRailConnection> CONNECTIONS = new LinkedHashMap<>();

	private ClientCurveRenderRegistry() {
	}

	public static void put(CurveRailConnection connection) {
		if (connection.primary()) {
			CONNECTIONS.put(connection.edgeId(), connection);
		}
	}

	public static void remove(CurveRailConnection connection) {
		if (connection.primary()) {
			CONNECTIONS.remove(connection.edgeId());
		}
	}

	public static void removeByEdgeId(String edgeId) {
		CONNECTIONS.remove(edgeId);
	}

	public static Collection<CurveRailConnection> all() {
		return Collections.unmodifiableCollection(CONNECTIONS.values());
	}

	public static void clear() {
		CONNECTIONS.clear();
	}
}
