package dev.steamrailway.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.steamrailway.debug.DebugPathRenderer;
import dev.steamrailway.persistence.SRSaveData;
import dev.steamrailway.rail.RailEdge;
import dev.steamrailway.rail.RailNetwork;
import dev.steamrailway.rail.RailNode;
import dev.steamrailway.rail.RailPathGenerator;
import dev.steamrailway.rail.TrackAnchorBlock;
import dev.steamrailway.train.TrainSpawnManager;
import dev.steamrailway.train.TrainState;
import dev.steamrailway.train.entity.TrainEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

public final class SRCommands {
	private static final double ANCHOR_RAYCAST_DISTANCE = 10.0D;
	private static final double DEFAULT_TRAIN_SPEED = 0.2D;

	private SRCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerRoot(dispatcher));
	}

	private static void registerRoot(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("sr")
				.then(CommandManager.literal("debug")
					.then(CommandManager.literal("ping")
						.executes(context -> {
							sendFeedback(context.getSource(), "command.steam_railway.debug.ping");
							return 1;
						})))
				.then(CommandManager.literal("anchor")
					.then(CommandManager.literal("bind")
						.then(CommandManager.argument("name", StringArgumentType.word())
							.executes(context -> bindAnchor(
								context.getSource(),
								StringArgumentType.getString(context, "name"))))))
				.then(CommandManager.literal("path")
					.then(CommandManager.literal("create")
						.then(CommandManager.argument("from", StringArgumentType.word())
							.then(CommandManager.argument("to", StringArgumentType.word())
								.executes(context -> createPath(
									context.getSource(),
									StringArgumentType.getString(context, "from"),
									StringArgumentType.getString(context, "to"))))))
					.then(CommandManager.literal("list")
						.executes(context -> listPaths(context.getSource())))
					.then(CommandManager.literal("debug")
						.then(CommandManager.literal("show")
							.executes(context -> showPathDebug(context.getSource(), null))
							.then(CommandManager.argument("pathId", StringArgumentType.word())
								.executes(context -> showPathDebug(
									context.getSource(),
									StringArgumentType.getString(context, "pathId")))))
						.then(CommandManager.literal("clear")
							.executes(context -> clearPathDebug(context.getSource())))))
				.then(CommandManager.literal("train")
					.then(CommandManager.literal("spawn")
						.then(CommandManager.argument("pathId", StringArgumentType.word())
							.executes(context -> spawnTrain(
								context.getSource(),
								StringArgumentType.getString(context, "pathId")))))
					.then(CommandManager.literal("start")
						.then(CommandManager.argument("trainId", StringArgumentType.word())
							.executes(context -> startTrain(
								context.getSource(),
								StringArgumentType.getString(context, "trainId")))))
					.then(CommandManager.literal("stop")
						.then(CommandManager.argument("trainId", StringArgumentType.word())
							.executes(context -> stopTrain(
								context.getSource(),
								StringArgumentType.getString(context, "trainId")))))
					.then(CommandManager.literal("list")
						.executes(context -> listTrains(context.getSource())))
					.then(CommandManager.literal("debug")
						.then(CommandManager.literal("show")
							.then(CommandManager.argument("trainId", StringArgumentType.word())
								.executes(context -> showTrainDebug(
									context.getSource(),
									StringArgumentType.getString(context, "trainId")))))
						.then(CommandManager.literal("entity")
							.then(CommandManager.argument("trainId", StringArgumentType.word())
								.executes(context -> showTrainEntityDebug(
									context.getSource(),
									StringArgumentType.getString(context, "trainId"))))))));
	}

	private static int bindAnchor(ServerCommandSource source, String requestedName) {
		ServerPlayerEntity player = getPlayer(source);
		BlockHitResult hit = getTargetedAnchor(player);
		if (hit == null) {
			sendError(source, "command.steam_railway.anchor.bind.look_at");
			return 0;
		}

		String displayName = requestedName.trim();
		if (displayName.isBlank()) {
			sendError(source, "command.steam_railway.anchor.bind.blank");
			return 0;
		}

		BlockPos pos = hit.getBlockPos();
		RailNetwork network = SRSaveData.get(source.getServer()).railNetwork();
		if (network.getNode(displayName) != null) {
			sendError(source, "command.steam_railway.anchor.bind.exists", displayName);
			return 0;
		}

		RailNode node = network.bindNode(displayName, source.getWorld(), pos);
		SRSaveData.get(source.getServer()).markDirty();
		sendFeedback(source, "command.steam_railway.anchor.bind.success", node.name(), pos.toShortString());
		return 1;
	}

	private static int createPath(ServerCommandSource source, String fromName, String toName) {
		RailNetwork network = SRSaveData.get(source.getServer()).railNetwork();
		RailNode fromNode = network.getNode(fromName);
		RailNode toNode = network.getNode(toName);

		if (fromNode == null) {
			sendError(source, "command.steam_railway.path.error.unknown_anchor", fromName);
			return 0;
		}
		if (toNode == null) {
			sendError(source, "command.steam_railway.path.error.unknown_anchor", toName);
			return 0;
		}
		if (fromNode.id().equals(toNode.id())) {
			sendError(source, "command.steam_railway.path.error.same_anchor");
			return 0;
		}
		if (!fromNode.dimension().equals(toNode.dimension())) {
			sendError(source, "command.steam_railway.path.error.dimension_mismatch");
			return 0;
		}

		ServerWorld world = source.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(fromNode.dimension())));
		if (world == null) {
			sendError(source, "command.steam_railway.path.error.dimension_unavailable", fromNode.dimension());
			return 0;
		}

		BlockState fromState = world.getBlockState(fromNode.blockPos());
		BlockState toState = world.getBlockState(toNode.blockPos());
		if (!fromState.isOf(SRBlocks.TRACK_ANCHOR)) {
			sendError(source, "command.steam_railway.path.error.anchor_missing", fromNode.name());
			return 0;
		}
		if (!toState.isOf(SRBlocks.TRACK_ANCHOR)) {
			sendError(source, "command.steam_railway.path.error.anchor_missing", toNode.name());
			return 0;
		}

		double distance = fromNode.worldPos().distanceTo(toNode.worldPos());
		if (distance < RailPathGenerator.MIN_PATH_DISTANCE) {
			sendError(source, "command.steam_railway.path.error.too_close");
			return 0;
		}
		if (distance > RailPathGenerator.MAX_PATH_DISTANCE) {
			sendError(source, "command.steam_railway.path.error.too_far");
			return 0;
		}

		String edgeId = network.nextEdgeId();
		RailEdge edge = RailPathGenerator.createEdge(
			edgeId,
			fromNode,
			fromState.get(TrackAnchorBlock.FACING),
			toNode,
			toState.get(TrackAnchorBlock.FACING));

		network.putEdge(edge);
		SRSaveData.get(source.getServer()).markDirty();

		ServerPlayerEntity player = getOptionalPlayer(source);
		if (player != null) {
			DebugPathRenderer.showPath(player.getUuid(), edge.id());
			DebugPathRenderer.renderPath(world, network, player, edge);
		}

		sendFeedback(
			source,
			"command.steam_railway.path.create.success",
			edge.id(),
			fromNode.name(),
			toNode.name(),
			String.format(Locale.ROOT, "%.1f", edge.length()));
		return 1;
	}

	private static int listPaths(ServerCommandSource source) {
		RailNetwork network = SRSaveData.get(source.getServer()).railNetwork();
		if (network.edges().isEmpty()) {
			sendFeedback(source, "command.steam_railway.path.list.empty");
			return 1;
		}

		sendFeedback(source, "command.steam_railway.path.list.header", network.edges().size());
		for (RailEdge edge : network.edges().values()) {
			RailNode fromNode = network.getNodeById(edge.fromNodeId());
			RailNode toNode = network.getNodeById(edge.toNodeId());
			String fromName = fromNode == null ? edge.fromNodeId() : fromNode.name();
			String toName = toNode == null ? edge.toNodeId() : toNode.name();
			sendFeedback(
				source,
				"command.steam_railway.path.list.entry",
				edge.id(),
				fromName,
				toName,
				String.format(Locale.ROOT, "%.1f", edge.length()),
				edge.samples().size());
		}
		return network.edges().size();
	}

	private static int showPathDebug(ServerCommandSource source, String requestedPathId) {
		ServerPlayerEntity player = getPlayer(source);
		RailNetwork network = SRSaveData.get(source.getServer()).railNetwork();
		String pathId = requestedPathId;
		if (pathId == null) {
			pathId = DebugPathRenderer.getSelectedPathId(player.getUuid()).orElseGet(network::latestEdgeId);
		}
		if (pathId == null) {
			sendError(source, "command.steam_railway.path.debug.none");
			return 0;
		}

		RailEdge edge = network.getEdge(pathId);
		if (edge == null) {
			sendError(source, "command.steam_railway.path.debug.unknown", pathId);
			return 0;
		}

		RailNode fromNode = network.getNodeById(edge.fromNodeId());
		if (fromNode == null) {
			sendError(source, "command.steam_railway.path.debug.source_missing");
			return 0;
		}

		ServerWorld world = source.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(fromNode.dimension())));
		if (world == null) {
			sendError(source, "command.steam_railway.path.debug.dimension_unavailable", fromNode.dimension());
			return 0;
		}

		DebugPathRenderer.showPath(player.getUuid(), edge.id());
		DebugPathRenderer.renderPath(world, network, player, edge);
		sendFeedback(source, "command.steam_railway.path.debug.show", edge.id());
		return 1;
	}

	private static int clearPathDebug(ServerCommandSource source) {
		ServerPlayerEntity player = getPlayer(source);
		DebugPathRenderer.clearPath(player.getUuid());
		sendFeedback(source, "command.steam_railway.path.debug.clear");
		return 1;
	}

	private static int spawnTrain(ServerCommandSource source, String pathId) {
		SRSaveData saveData = SRSaveData.get(source.getServer());
		RailNetwork network = saveData.railNetwork();
		RailEdge edge = network.getEdge(pathId);
		if (edge == null) {
			sendError(source, "command.steam_railway.train.spawn.path_unknown", pathId);
			return 0;
		}

		RailNode fromNode = network.getNodeById(edge.fromNodeId());
		if (fromNode == null) {
			sendError(source, "command.steam_railway.train.spawn.source_missing");
			return 0;
		}

		String trainId = saveData.nextTrainId();
		TrainState trainState = TrainState.createStopped(trainId, fromNode.dimension(), edge.id());
		trainState = TrainSpawnManager.syncVisibleEntity(source.getServer(), edge, trainState);
		saveData.putTrain(trainState);
		saveData.markDirty();
		String visibleEntityId = trainState.visibleEntityId().orElse("none");
		sendFeedback(source, "command.steam_railway.train.spawn.success", trainId, edge.id(), visibleEntityId);
		return 1;
	}

	private static int startTrain(ServerCommandSource source, String trainId) {
		SRSaveData saveData = SRSaveData.get(source.getServer());
		TrainState trainState = saveData.getTrain(trainId);
		if (trainState == null) {
			sendError(source, "command.steam_railway.train.error.unknown", trainId);
			return 0;
		}

		RailEdge edge = saveData.railNetwork().getEdge(trainState.edgeId());
		if (edge == null) {
			sendError(source, "command.steam_railway.train.error.path_missing", trainState.edgeId());
			return 0;
		}

		TrainState moving = trainState.movingAt(DEFAULT_TRAIN_SPEED);
		saveData.putTrain(moving);
		saveData.markDirty();
		sendFeedback(
			source,
			"command.steam_railway.train.start.success",
			trainId,
			moving.edgeId(),
			String.format(Locale.ROOT, "%.2f", DEFAULT_TRAIN_SPEED));
		return 1;
	}

	private static int stopTrain(ServerCommandSource source, String trainId) {
		SRSaveData saveData = SRSaveData.get(source.getServer());
		TrainState trainState = saveData.getTrain(trainId);
		if (trainState == null) {
			sendError(source, "command.steam_railway.train.error.unknown", trainId);
			return 0;
		}

		TrainState stopped = trainState.stoppedAt(trainState.distanceOnEdge());
		saveData.putTrain(stopped);
		saveData.markDirty();
		sendFeedback(source, "command.steam_railway.train.stop.success", trainId);
		return 1;
	}

	private static int listTrains(ServerCommandSource source) {
		SRSaveData saveData = SRSaveData.get(source.getServer());
		if (saveData.trains().isEmpty()) {
			sendFeedback(source, "command.steam_railway.train.list.empty");
			return 1;
		}

		sendFeedback(source, "command.steam_railway.train.list.header", saveData.trains().size());
		for (TrainState trainState : saveData.trains().values()) {
			sendFeedback(
				source,
				"command.steam_railway.train.list.entry",
				trainState.trainId(),
				trainState.edgeId(),
				String.format(Locale.ROOT, "%.2f", trainState.distanceOnEdge()),
				String.format(Locale.ROOT, "%.2f", trainState.speed()),
				Text.translatable("status.steam_railway." + trainState.status().serializedName()),
				trainState.visibleEntityId().orElse("none"));
		}
		return saveData.trains().size();
	}

	private static int showTrainDebug(ServerCommandSource source, String trainId) {
		ServerPlayerEntity player = getPlayer(source);
		SRSaveData saveData = SRSaveData.get(source.getServer());
		TrainState trainState = saveData.getTrain(trainId);
		if (trainState == null) {
			sendError(source, "command.steam_railway.train.error.unknown", trainId);
			return 0;
		}

		if (!player.getEntityWorld().getRegistryKey().getValue().equals(Identifier.of(trainState.dimension()))) {
			sendError(source, "command.steam_railway.train.debug.dimension_mismatch");
			return 0;
		}

		RailEdge edge = saveData.railNetwork().getEdge(trainState.edgeId());
		if (edge == null) {
			sendError(source, "command.steam_railway.train.error.path_missing", trainState.edgeId());
			return 0;
		}

		DebugPathRenderer.showTrain(player.getUuid(), trainState.trainId());
		DebugPathRenderer.renderTrainPosition(player.getEntityWorld(), edge, trainState.distanceOnEdge());
		sendFeedback(source, "command.steam_railway.train.debug.show", trainState.trainId());
		return 1;
	}

	private static int showTrainEntityDebug(ServerCommandSource source, String trainId) {
		SRSaveData saveData = SRSaveData.get(source.getServer());
		TrainState trainState = saveData.getTrain(trainId);
		if (trainState == null) {
			sendError(source, "command.steam_railway.train.error.unknown", trainId);
			return 0;
		}

		TrainEntity entity = TrainSpawnManager.findVisibleEntity(source.getServer(), trainState).orElse(null);
		String entityState = entity == null ? "missing" : "present";
		String entityPos = entity == null
			? "n/a"
			: String.format(Locale.ROOT, "%.2f, %.2f, %.2f", entity.getX(), entity.getY(), entity.getZ());

		sendFeedback(
			source,
			"command.steam_railway.train.debug.entity",
			trainState.trainId(),
			trainState.visibleEntityId().orElse("none"),
			String.format(Locale.ROOT, "%.2f", trainState.distanceOnEdge()),
			Text.translatable("command.steam_railway.train.debug.entity.state." + entityState),
			entityPos);
		return 1;
	}

	private static ServerPlayerEntity getPlayer(ServerCommandSource source) {
		try {
			return source.getPlayerOrThrow();
		} catch (Exception exception) {
			throw new IllegalStateException("This command requires a player executor.", exception);
		}
	}

	private static ServerPlayerEntity getOptionalPlayer(ServerCommandSource source) {
		try {
			return source.getPlayerOrThrow();
		} catch (Exception exception) {
			return null;
		}
	}

	private static BlockHitResult getTargetedAnchor(ServerPlayerEntity player) {
		HitResult hit = player.raycast(ANCHOR_RAYCAST_DISTANCE, 0.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		if (!player.getEntityWorld().getBlockState(blockHit.getBlockPos()).isOf(SRBlocks.TRACK_ANCHOR)) {
			return null;
		}
		return blockHit;
	}

	private static void sendFeedback(ServerCommandSource source, String key, Object... args) {
		source.sendFeedback(() -> Text.translatable(key, args), false);
	}

	private static void sendError(ServerCommandSource source, String key, Object... args) {
		source.sendError(Text.translatable(key, args));
	}
}
