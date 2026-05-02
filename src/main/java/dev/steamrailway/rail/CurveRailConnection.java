package dev.steamrailway.rail;

import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record CurveRailConnection(
	String edgeId,
	BlockPos targetPos,
	List<RailSample> samples,
	boolean primary
) {
	private static final String EDGE_ID_KEY = "edge_id";
	private static final String TARGET_POS_KEY = "target_pos";
	private static final String PRIMARY_KEY = "primary";
	private static final String SAMPLES_KEY = "samples";

	public CurveRailConnection {
		samples = List.copyOf(samples);
	}

	public CurveRailConnection reversed(BlockPos reversedTargetPos, boolean reversedPrimary) {
		if (samples.isEmpty()) {
			return new CurveRailConnection(edgeId, reversedTargetPos, List.of(), reversedPrimary);
		}

		double totalLength = samples.getLast().distance();
		List<RailSample> reversedSamples = new ArrayList<>(samples.size());
		for (int index = samples.size() - 1; index >= 0; index--) {
			RailSample sample = samples.get(index);
			reversedSamples.add(new RailSample(
				sample.x(),
				sample.y(),
				sample.z(),
				-sample.tangentX(),
				-sample.tangentY(),
				-sample.tangentZ(),
				totalLength - sample.distance()));
		}

		return new CurveRailConnection(edgeId, reversedTargetPos, reversedSamples, reversedPrimary);
	}

	public void writeData(WriteView view) {
		view.putString(EDGE_ID_KEY, edgeId);
		view.putLong(TARGET_POS_KEY, targetPos.asLong());
		view.putBoolean(PRIMARY_KEY, primary);

		WriteView.ListView sampleList = view.getList(SAMPLES_KEY);
		for (RailSample sample : samples) {
			WriteView sampleView = sampleList.add();
			sampleView.putDouble("x", sample.x());
			sampleView.putDouble("y", sample.y());
			sampleView.putDouble("z", sample.z());
			sampleView.putDouble("tangent_x", sample.tangentX());
			sampleView.putDouble("tangent_y", sample.tangentY());
			sampleView.putDouble("tangent_z", sample.tangentZ());
			sampleView.putDouble("distance", sample.distance());
		}
	}

	public static CurveRailConnection fromReadView(ReadView view) {
		String edgeId = view.getString(EDGE_ID_KEY, "");
		BlockPos targetPos = BlockPos.fromLong(view.getLong(TARGET_POS_KEY, BlockPos.ORIGIN.asLong()));
		boolean primary = view.getBoolean(PRIMARY_KEY, false);

		List<RailSample> samples = new ArrayList<>();
		view.getListReadView(SAMPLES_KEY).stream().forEach(sampleView -> {
			samples.add(new RailSample(
				sampleView.getDouble("x", 0.0D),
				sampleView.getDouble("y", 0.0D),
				sampleView.getDouble("z", 0.0D),
				sampleView.getDouble("tangent_x", 0.0D),
				sampleView.getDouble("tangent_y", 0.0D),
				sampleView.getDouble("tangent_z", 0.0D),
				sampleView.getDouble("distance", 0.0D)));
		});

		return new CurveRailConnection(edgeId, targetPos, samples, primary);
	}
}
