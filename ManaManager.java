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
    public static final int MAX_MANA = 100;                 // starting maximum
    private static final float BASE_REGEN_PER_TICK = 0.15F; // 3 mana per second
    // ------------------------------

    private static final Map<UUID, Float> MANA = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    public static int maxMana(PlayerEntity p) {
        return MAX_MANA + 20 * UpgradeManager.level(p, "mana_capacity");
    }

    private static float regenPerTick(PlayerEntity p) {
        return BASE_REGEN_PER_TICK + 0.05F * UpgradeManager.level(p, "mana_regen"); // +1/sec per level
    }

    /** The real cost after the Mana Efficiency upgrade. */
    public static int effectiveCost(PlayerEntity p, int cost) {
        float factor = 1.0F - 0.1F * UpgradeManager.level(p, "mana_efficiency");
        return Math.max(1, Math.round(cost * factor));
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                float max = maxMana(p);
                float m = MANA.getOrDefault(p.getUuid(), max);
                if (m < max) {
                    m = Math.min(max, m + regenPerTick(p));
                } else if (m > max) {
                    m = max;
                }
                MANA.put(p.getUuid(), m);
                sync(p, false);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            MANA.put(p.getUuid(), (float) maxMana(p));
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
    public static boolean tryUse(PlayerEntity user, int baseCost) {
        if (!(user instanceof ServerPlayerEntity p)) {
            return true;
        }
        int cost = effectiveCost(p, baseCost);
        float m = MANA.getOrDefault(p.getUuid(), (float) maxMana(p));
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

    /** Gives back what a spell of this base cost charged, for example when a teleport fizzles. */
    public static void refund(PlayerEntity user, int baseCost) {
        if (!(user instanceof ServerPlayerEntity p)) {
            return;
        }
        float max = maxMana(p);
        float m = MANA.getOrDefault(p.getUuid(), max);
        MANA.put(p.getUuid(), Math.min(max, m + effectiveCost(p, baseCost)));
        sync(p, true);
    }

    public static void forceSync(ServerPlayerEntity p) {
        sync(p, true);
    }

    private static void sync(ServerPlayerEntity p, boolean force) {
        int current = (int) Math.floor(MANA.getOrDefault(p.getUuid(), (float) maxMana(p)));
        if (force || LAST_SENT.getOrDefault(p.getUuid(), -1) != current) {
            ServerPlayNetworking.send(p, new ManaPayload(current, maxMana(p)));
            LAST_SENT.put(p.getUuid(), current);
        }
    }
}
