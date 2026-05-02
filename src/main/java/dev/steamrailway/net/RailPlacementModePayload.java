package dev.steamrailway.net;

import dev.steamrailway.SteamRailwayMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record RailPlacementModePayload(boolean gentle) implements CustomPayload {
	public static final Id<RailPlacementModePayload> ID = new Id<>(SteamRailwayMod.id("rail_placement_mode"));
	public static final PacketCodec<RegistryByteBuf, RailPlacementModePayload> CODEC =
		PacketCodec.tuple(PacketCodecs.BYTE, payload -> payload.gentle() ? (byte) 1 : (byte) 0, value -> new RailPlacementModePayload(value != 0));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
