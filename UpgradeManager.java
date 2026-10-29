package com.example.magicwand;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Remembers which upgrades each player bought. Saved in the world folder as magicwand_upgrades.json. */
public final class UpgradeManager {
    private UpgradeManager() {}

    private static final Gson GSON = new Gson();
    private static final Type FILE_TYPE = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();

    private static final Map<UUID, Map<String, Integer>> DATA = new HashMap<>();
    private static Path file;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            file = server.getSavePath(WorldSavePath.ROOT).resolve("magicwand_upgrades.json");
            load();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            save();
            DATA.clear();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));

        ServerPlayNetworking.registerGlobalReceiver(PurchasePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String id = payload.id();
            context.server().execute(() -> purchase(player, id));
        });
    }

    /** How many levels of this upgrade the player has (0 = not bought). */
    public static int level(PlayerEntity player, String id) {
        Map<String, Integer> mine = DATA.get(player.getUuid());
        if (mine == null) return 0;
        return mine.getOrDefault(id, 0);
    }

    /** Tells the player a power is locked. */
    public static void sendLocked(PlayerEntity player, String id) {
        Upgrades.Upgrade up = Upgrades.get(id);
        String name = up == null ? id : up.name();
        player.sendMessage(Text.literal(name + " is locked! Press U to unlock it.").formatted(Formatting.RED), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_DISPENSER_FAIL,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private static void purchase(ServerPlayerEntity p, String id) {
        Upgrades.Upgrade up = Upgrades.get(id);
        if (up == null) return;

        int lvl = level(p, id);
        if (lvl >= up.maxLevel()) {
            p.sendMessage(Text.literal(up.name() + " is already maxed.").formatted(Formatting.YELLOW), true);
            return;
        }
        if (up.requires() != null && level(p, up.requires()) < 1) {
            Upgrades.Upgrade req = Upgrades.get(up.requires());
            p.sendMessage(Text.literal("Requires " + (req == null ? up.requires() : req.name()) + " first.")
                    .formatted(Formatting.RED), true);
            return;
        }

        int cost = up.costs()[lvl];
        boolean free = p.isCreative();
        if (!free && p.experienceLevel < cost) {
            p.sendMessage(Text.literal("Not enough XP! You need " + cost + " levels.").formatted(Formatting.RED), true);
            p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_DISPENSER_FAIL,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            return;
        }

        if (!free) {
            p.addExperienceLevels(-cost);
        }
        DATA.computeIfAbsent(p.getUuid(), k -> new HashMap<>()).put(id, lvl + 1);
        save();
        sync(p);
        ManaManager.forceSync(p); // in case max mana changed

        p.sendMessage(Text.literal("Unlocked " + up.name() + " (level " + (lvl + 1) + ")!")
                .formatted(Formatting.GREEN), true);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private static void sync(ServerPlayerEntity p) {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> mine = DATA.get(p.getUuid());
        if (mine != null) {
            for (Map.Entry<String, Integer> e : mine.entrySet()) {
                if (sb.length() > 0) sb.append(';');
                sb.append(e.getKey()).append('=').append(e.getValue());
            }
        }
        ServerPlayNetworking.send(p, new UpgradesPayload(sb.toString()));
    }

    private static void load() {
        DATA.clear();
        try {
            if (file != null && Files.exists(file)) {
                Map<String, Map<String, Integer>> loaded = GSON.fromJson(Files.readString(file), FILE_TYPE);
                if (loaded != null) {
                    for (Map.Entry<String, Map<String, Integer>> e : loaded.entrySet()) {
                        DATA.put(UUID.fromString(e.getKey()), new HashMap<>(e.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        if (file == null) return;
        try {
            Map<String, Map<String, Integer>> out = new HashMap<>();
            for (Map.Entry<UUID, Map<String, Integer>> e : DATA.entrySet()) {
                out.put(e.getKey().toString(), e.getValue());
            }
            Files.writeString(file, GSON.toJson(out));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
