package com.example.magicwand;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Sent from the server to a player so their screen can show the mana bar. */
public record ManaPayload(int mana, int max) implements CustomPayload {
    public static final CustomPayload.Id<ManaPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MagicWandMod.MOD_ID, "mana"));

    public static final PacketCodec<RegistryByteBuf, ManaPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ManaPayload::mana,
            PacketCodecs.VAR_INT, ManaPayload::max,
            ManaPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
