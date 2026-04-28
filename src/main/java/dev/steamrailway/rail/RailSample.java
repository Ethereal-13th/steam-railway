package dev.steamrailway.rail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.Vec3d;

public record RailSample(
	double x,
	double y,
	double z,
	double tangentX,
	double tangentY,
	double tangentZ,
	double distance
) {
	public static final Codec<RailSample> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.DOUBLE.fieldOf("x").forGetter(RailSample::x),
		Codec.DOUBLE.fieldOf("y").forGetter(RailSample::y),
		Codec.DOUBLE.fieldOf("z").forGetter(RailSample::z),
		Codec.DOUBLE.fieldOf("tangent_x").forGetter(RailSample::tangentX),
		Codec.DOUBLE.fieldOf("tangent_y").forGetter(RailSample::tangentY),
		Codec.DOUBLE.fieldOf("tangent_z").forGetter(RailSample::tangentZ),
		Codec.DOUBLE.fieldOf("distance").forGetter(RailSample::distance)
	).apply(instance, RailSample::new));

	public Vec3d position() {
		return new Vec3d(x, y, z);
	}

	public Vec3d tangent() {
		return new Vec3d(tangentX, tangentY, tangentZ);
	}
}
