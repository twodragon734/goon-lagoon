package com.example.magicwand;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server to client: all of a player's upgrade levels, written as "id=level;id=level". */
public record UpgradesPayload(String data) implements CustomPayload {
    public static final CustomPayload.Id<UpgradesPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MagicWandMod.MOD_ID, "upgrades"));

    public static final PacketCodec<RegistryByteBuf, UpgradesPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, UpgradesPayload::data,
            UpgradesPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
