package com.example.magicwand;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client to server: "I want to buy this upgrade". */
public record PurchasePayload(String id) implements CustomPayload {
    public static final CustomPayload.Id<PurchasePayload> ID =
            new CustomPayload.Id<>(Identifier.of(MagicWandMod.MOD_ID, "purchase"));

    public static final PacketCodec<RegistryByteBuf, PurchasePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, PurchasePayload::id,
            PurchasePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
