package dev.steamrailway.rail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class RailNetwork {
	public static final Codec<RailNetwork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.LONG.optionalFieldOf("version", 0L).forGetter(RailNetwork::version),
		Codec.unboundedMap(Codec.STRING, RailNode.CODEC).optionalFieldOf("nodes", Map.of()).forGetter(RailNetwork::nodes),
		Codec.unboundedMap(Codec.STRING, RailEdge.CODEC).optionalFieldOf("edges", Map.of()).forGetter(RailNetwork::edges)
	).apply(instance, RailNetwork::new));

	private long version;
	private final LinkedHashMap<String, RailNode> nodes;
	private final LinkedHashMap<String, RailEdge> edges;

	public RailNetwork() {
		this(0L, Map.of(), Map.of());
	}

	private RailNetwork(long version, Map<String, RailNode> nodes, Map<String, RailEdge> edges) {
		this.version = version;
		this.nodes = new LinkedHashMap<>(nodes);
		this.edges = new LinkedHashMap<>(edges);
	}

	public long version() {
		return version;
	}

	public Map<String, RailNode> nodes() {
		return Collections.unmodifiableMap(nodes);
	}

	public Map<String, RailEdge> edges() {
		return Collections.unmodifiableMap(edges);
	}

	public RailNode bindNode(String requestedName, ServerWorld world, BlockPos pos) {
		String displayName = requestedName.trim();
		if (displayName.isBlank()) {
			throw new IllegalArgumentException("Anchor name cannot be blank.");
		}

		String key = normalizeNodeKey(displayName);
		if (nodes.containsKey(key)) {
			throw new IllegalArgumentException("Anchor name already exists: " + displayName);
		}

		Vec3d worldPos = Vec3d.ofBottomCenter(pos).add(0.0D, 1.0D, 0.0D);
		RailNode node = new RailNode(
			key,
			displayName,
			world.getRegistryKey().getValue().toString(),
			pos.getX(),
			pos.getY(),
			pos.getZ(),
			worldPos.x,
			worldPos.y,
			worldPos.z);

		nodes.put(key, node);
		version++;
		return node;
	}

	public RailNode getNode(String requestedName) {
		return nodes.get(normalizeNodeKey(requestedName));
	}

	public RailNode getNodeById(String nodeId) {
		return nodes.get(nodeId);
	}

	public RailEdge getEdge(String edgeId) {
		return edges.get(edgeId);
	}

	public RailNode createPlacedNode(ServerWorld world, BlockPos pos) {
		String nodeId = nextPlacedNodeId();
		Vec3d worldPos = RailGeometry.worldPosition(pos);
		RailNode node = new RailNode(
			nodeId,
			nodeId,
			world.getRegistryKey().getValue().toString(),
			pos.getX(),
			pos.getY(),
			pos.getZ(),
			worldPos.x,
			worldPos.y,
			worldPos.z);

		nodes.put(nodeId, node);
		version++;
		return node;
	}

	public void putEdge(RailEdge edge) {
		edges.put(edge.id(), edge);
		version++;
	}

	public RailEdge removeEdge(String edgeId) {
		RailEdge removed = edges.remove(edgeId);
		if (removed != null) {
			version++;
		}
		return removed;
	}

	public String nextEdgeId() {
		return String.format(Locale.ROOT, "path_%04d", edges.size() + 1);
	}

	public String latestEdgeId() {
		String last = null;
		for (String edgeId : edges.keySet()) {
			last = edgeId;
		}
		return last;
	}

	private String nextPlacedNodeId() {
		int index = 1;
		while (true) {
			String candidate = String.format(Locale.ROOT, "placed_node_%04d", index);
			if (!nodes.containsKey(candidate)) {
				return candidate;
			}
			index++;
		}
	}

	private static String normalizeNodeKey(String requestedName) {
		return requestedName.trim().toLowerCase(Locale.ROOT);
	}
}
