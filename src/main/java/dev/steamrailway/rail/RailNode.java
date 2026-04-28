package dev.steamrailway.rail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record RailNode(
	String id,
	String name,
	String dimension,
	int blockX,
	int blockY,
	int blockZ,
	double worldX,
	double worldY,
	double worldZ
) {
	private static final double LEGACY_EPSILON = 1.0E-6D;

	public static final Codec<RailNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("id").forGetter(RailNode::id),
		Codec.STRING.fieldOf("name").forGetter(RailNode::name),
		Codec.STRING.fieldOf("dimension").forGetter(RailNode::dimension),
		Codec.INT.fieldOf("block_x").forGetter(RailNode::blockX),
		Codec.INT.fieldOf("block_y").forGetter(RailNode::blockY),
		Codec.INT.fieldOf("block_z").forGetter(RailNode::blockZ),
		Codec.DOUBLE.fieldOf("world_x").forGetter(RailNode::worldX),
		Codec.DOUBLE.fieldOf("world_y").forGetter(RailNode::worldY),
		Codec.DOUBLE.fieldOf("world_z").forGetter(RailNode::worldZ)
	).apply(instance, RailNode::new));

	public BlockPos blockPos() {
		return new BlockPos(blockX, blockY, blockZ);
	}

	public Vec3d worldPos() {
		if (isLegacyCenteredPosition()) {
			return new Vec3d(blockX + 0.5D, blockY + 1.0D, blockZ + 0.5D);
		}

		return new Vec3d(worldX, worldY, worldZ);
	}

	private boolean isLegacyCenteredPosition() {
		return Math.abs(worldX - (blockX + 0.5D)) < LEGACY_EPSILON
			&& Math.abs(worldY - (blockY + 0.5D)) < LEGACY_EPSILON
			&& Math.abs(worldZ - (blockZ + 0.5D)) < LEGACY_EPSILON;
	}
}
