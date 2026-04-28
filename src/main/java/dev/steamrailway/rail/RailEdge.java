package dev.steamrailway.rail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record RailEdge(
	String id,
	String fromNodeId,
	String toNodeId,
	List<RailSample> samples,
	double length
) {
	public static final Codec<RailEdge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("id").forGetter(RailEdge::id),
		Codec.STRING.fieldOf("from_node_id").forGetter(RailEdge::fromNodeId),
		Codec.STRING.fieldOf("to_node_id").forGetter(RailEdge::toNodeId),
		RailSample.CODEC.listOf().fieldOf("samples").forGetter(RailEdge::samples),
		Codec.DOUBLE.fieldOf("length").forGetter(RailEdge::length)
	).apply(instance, RailEdge::new));
}
