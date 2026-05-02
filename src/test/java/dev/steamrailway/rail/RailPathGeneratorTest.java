package dev.steamrailway.rail;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailPathGeneratorTest {
	@Test
	void allowsParallelOneBlockOffsetWhenThereIsEnoughForwardRun() {
		RailPathGenerator.ToolPath path = RailPathGenerator.createToolPath(
			new BlockPos(0, 64, 0),
			Direction.SOUTH,
			false,
			new BlockPos(1, 64, 6),
			Direction.SOUTH,
			false,
			false);

		assertTrue(path.valid(), "one-block lateral offset should be placeable once the run is long enough");
		assertEquals(RailPathGenerator.ToolPathKind.CURVE, path.kind());
		assertTrue(path.samples().size() >= 2);
	}

	@Test
	void allowsShortSmallDiagonalConnections() {
		RailPathGenerator.ToolPath path = RailPathGenerator.createToolPath(
			new BlockPos(0, 64, 0),
			Direction.SOUTH,
			false,
			new BlockPos(1, 64, 3),
			Direction.SOUTH,
			false,
			false);

		assertTrue(path.valid(), "short diagonal offsets should stay available instead of being forced invalid");
		assertTrue(path.samples().size() >= 2);
	}
}
