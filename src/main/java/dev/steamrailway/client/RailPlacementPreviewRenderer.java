package dev.steamrailway.client;

import dev.steamrailway.client.ClientRailPlacementPreviewState.PreviewSession;
import dev.steamrailway.rail.RailPlacementMode;
import dev.steamrailway.rail.RailGeometry;
import dev.steamrailway.rail.RailPathGenerator;
import dev.steamrailway.rail.RailPlacementTargetResolver;
import dev.steamrailway.rail.RailSample;
import dev.steamrailway.registry.SRItems;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;

public final class RailPlacementPreviewRenderer {
	private static final double TRACK_HALF_WIDTH = 0.6D;
	private static final double TIE_STEP = 1.0D;
	private static final int TIE_PEGS = 5;
	private static final VoxelShape RAIL_SHAPE = centeredShape(0.26D, 0.22D, 0.26D);
	private static final VoxelShape TIE_SHAPE = centeredShape(0.20D, 0.14D, 0.20D);
	private static final VoxelShape END_MARKER_SHAPE = centeredShape(0.42D, 0.42D, 0.42D);
	private static final int VALID_RAIL_COLOR = rgb(0.36F, 0.95F, 0.53F);
	private static final int INVALID_RAIL_COLOR = rgb(0.95F, 0.28F, 0.28F);
	private static final int VALID_TIE_COLOR = rgb(0.82F, 0.73F, 0.45F);
	private static final int INVALID_TIE_COLOR = rgb(0.80F, 0.42F, 0.38F);
	private static final int END_MARKER_COLOR = rgb(0.96F, 0.96F, 0.98F);
	private static final float ALPHA = 0.95F;
	private static final DustParticleEffect VALID_RAIL_PARTICLE = new DustParticleEffect(VALID_RAIL_COLOR, 1.25F);
	private static final DustParticleEffect INVALID_RAIL_PARTICLE = new DustParticleEffect(INVALID_RAIL_COLOR, 1.25F);
	private static final DustParticleEffect VALID_TIE_PARTICLE = new DustParticleEffect(VALID_TIE_COLOR, 0.85F);
	private static final DustParticleEffect INVALID_TIE_PARTICLE = new DustParticleEffect(INVALID_TIE_COLOR, 0.85F);

	private RailPlacementPreviewRenderer() {
	}

	public static void register() {
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(RailPlacementPreviewRenderer::render);
	}

	private static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.crosshairTarget == null) {
			return;
		}
		if (!player.getMainHandStack().isOf(SRItems.RAIL_PLACEMENT_TOOL) && !player.getOffHandStack().isOf(SRItems.RAIL_PLACEMENT_TOOL)) {
			return;
		}
		if (!(client.crosshairTarget instanceof BlockHitResult blockHit) || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
			return;
		}

		PreviewSession session = ClientRailPlacementPreviewState.getCurrentSession().orElse(null);
		if (session == null || !session.dimension().equals(client.world.getRegistryKey().getValue().toString())) {
			return;
		}

		ResolvedRailPreview preview = resolvePreview(client, player, blockHit, session);
		if (preview == null) {
			return;
		}

		renderPreview(context, client, preview);
	}

	public static void spawnParticlePreview(MinecraftClient client, PreviewSession session) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.crosshairTarget == null) {
			return;
		}
		if (!(client.crosshairTarget instanceof BlockHitResult blockHit) || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
			return;
		}

		ResolvedRailPreview preview = resolvePreview(client, player, blockHit, session);
		if (preview == null) {
			return;
		}

		DustParticleEffect railParticle = preview.valid() ? VALID_RAIL_PARTICLE : INVALID_RAIL_PARTICLE;
		DustParticleEffect tieParticle = preview.valid() ? VALID_TIE_PARTICLE : INVALID_TIE_PARTICLE;
		double lastTieDistance = Double.NEGATIVE_INFINITY;
		List<RailSample> samples = preview.samples();
		for (int index = 0; index < samples.size(); index += 2) {
			RailSample sample = samples.get(index);
			Vec3d center = sample.position();
			Vec3d lateral = RailGeometry.lateralVector(sample.tangent()).multiply(TRACK_HALF_WIDTH);
			Vec3d leftRail = center.add(lateral).add(0.0D, 0.03D, 0.0D);
			Vec3d rightRail = center.subtract(lateral).add(0.0D, 0.03D, 0.0D);

			spawnParticle(client, railParticle, leftRail);
			spawnParticle(client, railParticle, rightRail);

			if (sample.distance() - lastTieDistance >= TIE_STEP) {
				Vec3d left = center.add(lateral.multiply(0.92D));
				Vec3d right = center.subtract(lateral.multiply(0.92D));
				for (int tieIndex = 0; tieIndex < TIE_PEGS; tieIndex++) {
					double progress = TIE_PEGS == 1 ? 0.5D : tieIndex / (double) (TIE_PEGS - 1);
					Vec3d pegCenter = left.lerp(right, progress).add(0.0D, -0.04D, 0.0D);
					spawnParticle(client, tieParticle, pegCenter);
				}
				lastTieDistance = sample.distance();
			}
		}

		Vec3d first = samples.getFirst().position();
		Vec3d last = samples.getLast().position();
		spawnParticle(client, ParticleTypes.END_ROD, first.add(0.0D, 0.1D, 0.0D));
		spawnParticle(client, ParticleTypes.END_ROD, last.add(0.0D, 0.1D, 0.0D));
	}

	private static ResolvedRailPreview resolvePreview(
		MinecraftClient client,
		ClientPlayerEntity player,
		BlockHitResult blockHit,
		PreviewSession session
	) {
		RailPlacementTargetResolver.RailPlacementTarget target = RailPlacementTargetResolver.resolveTarget(
			client.world,
			blockHit.getBlockPos(),
			blockHit.getSide(),
			player.getHorizontalFacing()
		).orElse(null);
		if (target == null || target.pos().equals(session.startPos())) {
			return null;
		}

		Vec3d start = RailGeometry.worldPosition(session.startPos());
		Vec3d end = RailGeometry.worldPosition(target.pos());
		double distance = start.distanceTo(end);
		if (distance < RailPathGenerator.MIN_PATH_DISTANCE || distance > RailPathGenerator.MAX_PATH_DISTANCE) {
			return null;
		}

		boolean gentle = session.mode() == RailPlacementMode.GENTLE;
		RailPathGenerator.ToolPath toolPath = RailPathGenerator.createToolPath(
			session.startPos(),
			session.startFacing(),
			session.startFacingLocked(),
			target.pos(),
			target.facing(),
			target.lockedFacing(),
			gentle);
		if (toolPath.samples().size() < 2) {
			return null;
		}

		return new ResolvedRailPreview(target, toolPath.samples(), toolPath.valid(), distance);
	}

	private static void renderPreview(WorldRenderContext context, MinecraftClient client, ResolvedRailPreview preview) {
		Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
		VertexConsumer outlineConsumer = context.consumers().getBuffer(RenderLayers.lines());
		List<RailSample> samples = preview.samples();
		int railColor = preview.valid() ? VALID_RAIL_COLOR : INVALID_RAIL_COLOR;
		int tieColor = preview.valid() ? VALID_TIE_COLOR : INVALID_TIE_COLOR;

		double lastTieDistance = Double.NEGATIVE_INFINITY;
		for (RailSample sample : samples) {
			Vec3d center = sample.position();
			Vec3d lateral = RailGeometry.lateralVector(sample.tangent()).multiply(TRACK_HALF_WIDTH);
			Vec3d leftRail = center.add(lateral).add(0.0D, 0.03D, 0.0D);
			Vec3d rightRail = center.subtract(lateral).add(0.0D, 0.03D, 0.0D);

			drawShape(context, outlineConsumer, RAIL_SHAPE, leftRail.subtract(cameraPos), railColor);
			drawShape(context, outlineConsumer, RAIL_SHAPE, rightRail.subtract(cameraPos), railColor);

			if (sample.distance() - lastTieDistance >= TIE_STEP) {
				drawTie(context, outlineConsumer, center.subtract(cameraPos), lateral, tieColor);
				lastTieDistance = sample.distance();
			}
		}

		drawShape(context, outlineConsumer, END_MARKER_SHAPE, samples.getFirst().position().subtract(cameraPos), END_MARKER_COLOR);
		drawShape(context, outlineConsumer, END_MARKER_SHAPE, samples.getLast().position().subtract(cameraPos), END_MARKER_COLOR);
	}

	private static void drawTie(WorldRenderContext context, VertexConsumer consumer, Vec3d center, Vec3d lateral, int tieColor) {
		Vec3d left = center.add(lateral.multiply(0.92D));
		Vec3d right = center.subtract(lateral.multiply(0.92D));

		for (int index = 0; index < TIE_PEGS; index++) {
			double progress = TIE_PEGS == 1 ? 0.5D : index / (double) (TIE_PEGS - 1);
			Vec3d pegCenter = left.lerp(right, progress).add(0.0D, -0.04D, 0.0D);
			drawShape(context, consumer, TIE_SHAPE, pegCenter, tieColor);
		}
	}

	private static void drawShape(WorldRenderContext context, VertexConsumer consumer, VoxelShape shape, Vec3d center, int color) {
		net.minecraft.client.render.VertexRendering.drawOutline(
			context.matrices(),
			consumer,
			shape,
			center.x - 0.5D,
			center.y - 0.5D,
			center.z - 0.5D,
			color,
			ALPHA);
	}

	private static VoxelShape centeredShape(double width, double height, double depth) {
		double minX = (1.0D - width) * 8.0D;
		double minY = (1.0D - height) * 8.0D;
		double minZ = (1.0D - depth) * 8.0D;
		double maxX = 16.0D - minX;
		double maxY = 16.0D - minY;
		double maxZ = 16.0D - minZ;
		return Block.createCuboidShape(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static int rgb(float red, float green, float blue) {
		return ((int) (red * 255.0F) << 16) | ((int) (green * 255.0F) << 8) | (int) (blue * 255.0F);
	}

	private static void spawnParticle(MinecraftClient client, net.minecraft.particle.ParticleEffect particle, Vec3d pos) {
		client.world.addParticleClient(particle, false, false, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
	}

	private record ResolvedRailPreview(
		RailPlacementTargetResolver.RailPlacementTarget target,
		List<RailSample> samples,
		boolean valid,
		double distance
	) {
	}
}
