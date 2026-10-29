package com.example.magicwand;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps track of every player's mana. Mana refills over time and resets to full when you rejoin. */
public final class ManaManager {
    private ManaManager() {}

    // ---- Tweak these to taste ----
    public static final int MAX_MANA = 100;
    private static final float REGEN_PER_TICK = 0.15F; // 3 mana per second (full bar in about 33 s)
    // ------------------------------

    private static final Map<UUID, Float> MANA = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                float m = MANA.getOrDefault(p.getUuid(), (float) MAX_MANA);
                if (m < MAX_MANA) {
                    m = Math.min(MAX_MANA, m + REGEN_PER_TICK);
                }
                MANA.put(p.getUuid(), m);
                sync(p, false);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            MANA.put(p.getUuid(), (float) MAX_MANA);
            sync(p, true);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            MANA.remove(handler.player.getUuid());
            LAST_SENT.remove(handler.player.getUuid());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            MANA.clear();
            LAST_SENT.clear();
        });
    }

    /** Spends mana if the player has enough. If not, tells them and returns false. */
    public static boolean tryUse(PlayerEntity user, int cost) {
        if (!(user instanceof ServerPlayerEntity p)) {
            return true;
        }
        float m = MANA.getOrDefault(p.getUuid(), (float) MAX_MANA);
        if (m < cost) {
            p.sendMessage(Text.literal("Not enough mana!").formatted(Formatting.RED), true);
            p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_DISPENSER_FAIL,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            return false;
        }
        MANA.put(p.getUuid(), m - cost);
        sync(p, true);
        return true;
    }

    /** Gives mana back, for example when a teleport fizzles. */
    public static void refund(PlayerEntity user, int amount) {
        if (!(user instanceof ServerPlayerEntity p)) {
            return;
        }
        float m = MANA.getOrDefault(p.getUuid(), (float) MAX_MANA);
        MANA.put(p.getUuid(), Math.min(MAX_MANA, m + amount));
        sync(p, true);
    }

    private static void sync(ServerPlayerEntity p, boolean force) {
        int current = (int) Math.floor(MANA.getOrDefault(p.getUuid(), (float) MAX_MANA));
        if (force || LAST_SENT.getOrDefault(p.getUuid(), -1) != current) {
            ServerPlayNetworking.send(p, new ManaPayload(current, MAX_MANA));
            LAST_SENT.put(p.getUuid(), current);
        }
    }
}
