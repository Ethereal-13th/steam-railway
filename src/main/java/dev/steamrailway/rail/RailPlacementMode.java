package dev.steamrailway.rail;

public enum RailPlacementMode {
	NORMAL(false),
	GENTLE(true);

	private final boolean gentle;

	RailPlacementMode(boolean gentle) {
		this.gentle = gentle;
	}

	public boolean gentle() {
		return gentle;
	}

	public static RailPlacementMode fromGentle(boolean gentle) {
		return gentle ? GENTLE : NORMAL;
	}
}
