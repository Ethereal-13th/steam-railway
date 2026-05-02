package dev.steamrailway.client;

import dev.steamrailway.rail.CurveRailConnection;
import dev.steamrailway.rail.ClientCurveRenderRegistry;
import dev.steamrailway.rail.RailGeometry;
import dev.steamrailway.rail.RailSample;
import dev.steamrailway.registry.SRBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import java.util.List;

public final class PlacedRailCurveRenderer {
	private static final double RAIL_SEGMENT_STEP = 0.30D;
	private static final double TIE_SEGMENT_STEP = 1.0D;
	private static final double RAIL_SEGMENT_MARGIN = 0.55D;
	private static final double TIE_SEGMENT_MARGIN = 0.5D;
	private static final BlockState CURVE_RAIL_STATE = SRBlocks.CURVE_RAIL_RAIL_RENDER.getDefaultState();
	private static final BlockState CURVE_TIE_STATE = SRBlocks.CURVE_RAIL_TIE_RENDER.getDefaultState();
	private static final int FULL_BRIGHT = 0x00F000F0;

	private PlacedRailCurveRenderer() {
	}

	public static void register() {
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(PlacedRailCurveRenderer::render);
	}

	private static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.gameRenderer == null || context.consumers() == null) {
			return;
		}

		Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
		for (CurveRailConnection connection : ClientCurveRenderRegistry.all()) {
			if (!connection.primary()) {
				continue;
			}
			renderConnection(context.matrices(), context, cameraPos, connection);
		}
	}

	private static void renderConnection(MatrixStack matrices, WorldRenderContext context, Vec3d cameraPos, CurveRailConnection connection) {
		List<RailSample> samples = connection.samples();
		if (samples.size() < 2) {
			return;
		}

		double length = samples.getLast().distance();
		if (length <= Math.max(RAIL_SEGMENT_MARGIN, TIE_SEGMENT_MARGIN) * 2.0D) {
			return;
		}

		for (double distance = RAIL_SEGMENT_MARGIN; distance < length - RAIL_SEGMENT_MARGIN; distance += RAIL_SEGMENT_STEP) {
			RailSample sample = RailGeometry.sampleAtDistance(samples, distance);
			renderSegment(matrices, context, cameraPos, sample, CURVE_RAIL_STATE, -0.25D);
		}

		for (double distance = TIE_SEGMENT_MARGIN; distance < length - TIE_SEGMENT_MARGIN; distance += TIE_SEGMENT_STEP) {
			RailSample sample = RailGeometry.sampleAtDistance(samples, distance);
			renderSegment(matrices, context, cameraPos, sample, CURVE_TIE_STATE, -0.25D);
		}
	}

	private static void renderSegment(
		MatrixStack matrices,
		WorldRenderContext context,
		Vec3d cameraPos,
		RailSample sample,
		BlockState renderState,
		double yOffset
	) {
		Vec3d point = sample.position();
		Vec3d tangent = sample.tangent();
		float yawDegrees = (float) Math.toDegrees(Math.atan2(tangent.x, tangent.z));
		Vec3d local = point.subtract(cameraPos);

		matrices.push();
		matrices.translate(local.x, local.y + yOffset, local.z);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDegrees));
		matrices.translate(-0.5D, 0.0D, -0.5D);
		MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
			renderState,
			matrices,
			context.consumers(),
			FULL_BRIGHT,
			OverlayTexture.DEFAULT_UV);
		matrices.pop();
	}

}
