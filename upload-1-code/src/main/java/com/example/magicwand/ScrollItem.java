package com.example.magicwand;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/** Spell scrolls. They are never used up: right-click as often as the cooldown and mana allow. */
public class ScrollItem extends Item {
    public enum Kind { SPEED, DRILL, GROWTH, RECALL, CLONE, BRIDGE }

    private final Kind kind;

    public ScrollItem(Settings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    // ---- Tweak these to taste (20 ticks = 1 second) ----
    // Speedster
    private static final int SPEED_BASE_DURATION_TICKS = 1200; // 60 seconds (+30s per Extended Haste level)
    private static final int SPEED_BASE_LEVEL = 2;             // 2 = Speed III (+1 per Hyper Speed level)
    // Drill
    private static final double DRILL_BASE_DISTANCE = 30.0;    // +15 per Deep Bore level
    private static final double DRILL_BASE_RADIUS = 1.6;       // +0.5 per Wide Bore level
    // Growth
    private static final int GROWTH_BASE_RADIUS = 4;           // +2 per Wide Bloom level
    // Recall
    private static final int RECALL_BASE_COOLDOWN_TICKS = 2400; // 2 minutes, -20% per Quick Return level
    // Clone
    private static final int CLONE_BASE_DURATION_TICKS = 160;  // 8 seconds (+5s per Lasting Illusion level)
    private static final int CLONE_INVISIBLE_TICKS = 100;      // you turn invisible for 5 seconds
    // Bridge
    private static final int BRIDGE_BASE_LENGTH = 12;          // blocks (+4 per Long Bridge level)
    private static final int BRIDGE_BASE_DURATION_TICKS = 300; // 15 seconds (+5s per Lasting Bridge level)

    private int manaCost() {
        return switch (kind) {
            case SPEED -> 40;
            case DRILL -> 50;
            case GROWTH -> 30;
            case RECALL -> 60;
            case CLONE -> 40;
            case BRIDGE -> 35;
        };
    }

    private int cooldownTicks(PlayerEntity user) {
        return switch (kind) {
            case SPEED -> 1800; // 90 seconds
            case DRILL -> 300;  // 15 seconds
            case GROWTH -> 600; // 30 seconds
            case RECALL -> Math.max(200, Math.round(RECALL_BASE_COOLDOWN_TICKS
                    * (1.0F - 0.2F * UpgradeManager.level(user, "quick_return"))));
            case CLONE -> 900;  // 45 seconds
            case BRIDGE -> 300; // 15 seconds
        };
    }
    // -----------------------------------------------------

    private String requiredUpgrade() {
        return switch (kind) {
            case SPEED -> "swift_step";
            case DRILL -> "drill_bore";
            case GROWTH -> "verdant_touch";
            case RECALL -> "homeward";
            case CLONE -> "mirror_image";
            case BRIDGE -> "skybridge";
        };
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }
        return cast(sw, user, user.getStackInHand(hand));
    }

    /** Casts this scroll. Also used by the Spell Book. The scroll is never consumed. */
    public ActionResult cast(ServerWorld sw, PlayerEntity user, ItemStack stack) {
        if (user.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.FAIL;
        }

        String required = requiredUpgrade();
        if (UpgradeManager.level(user, required) < 1) {
            UpgradeManager.sendLocked(user, required);
            return ActionResult.FAIL;
        }

        if (!ManaManager.tryUse(user, manaCost())) {
            return ActionResult.FAIL;
        }

        boolean ok = true;
        switch (kind) {
            case SPEED -> speed(sw, user);
            case DRILL -> {
                double distance = DRILL_BASE_DISTANCE + 15.0 * UpgradeManager.level(user, "deep_bore");
                double radius = DRILL_BASE_RADIUS + 0.5 * UpgradeManager.level(user, "wide_bore");
                DrillManager.start(sw, user, distance, radius);
            }
            case GROWTH -> growth(sw, user);
            case RECALL -> ok = recall(sw, user);
            case CLONE -> clone(sw, user);
            case BRIDGE -> ok = bridge(sw, user);
        }

        if (!ok) {
            ManaManager.refund(user, manaCost()); // nothing happened, so no mana lost
            return ActionResult.FAIL;
        }

        user.getItemCooldownManager().set(stack, cooldownTicks(user));
        return ActionResult.SUCCESS; // note: the scroll is NOT consumed
    }

    // ---------------- Speedster ----------------
    private void speed(ServerWorld world, PlayerEntity user) {
        int duration = SPEED_BASE_DURATION_TICKS + 600 * UpgradeManager.level(user, "extended_haste");
        int level = SPEED_BASE_LEVEL + UpgradeManager.level(user, "hyper_speed");
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, level));
        world.spawnParticles(ParticleTypes.CLOUD, user.getX(), user.getY() + 0.2, user.getZ(),
                30, 0.4, 0.1, 0.4, 0.1);
        world.spawnParticles(ParticleTypes.CRIT, user.getX(), user.getY() + 1.0, user.getZ(),
                20, 0.4, 0.6, 0.4, 0.2);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.PLAYERS, 1.0F, 1.4F);
    }

    // ---------------- Growth: bone meal on everything around you ----------------
    private void growth(ServerWorld world, PlayerEntity user) {
        int radius = GROWTH_BASE_RADIUS + 2 * UpgradeManager.level(user, "wide_bloom");
        int passes = 1 + UpgradeManager.level(user, "overgrowth");
        BlockPos center = user.getBlockPos();
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 64); // a throwaway stack, nothing is taken from you

        for (int pass = 0; pass < passes; pass++) {
            for (BlockPos pos : BlockPos.iterate(center.add(-radius, -2, -radius), center.add(radius, 2, radius))) {
                BlockPos fixed = pos.toImmutable();
                if (BoneMealItem.useOnFertilizable(meal, world, fixed)) {
                    meal.setCount(64);
                    world.syncWorldEvent(2005, fixed, 15); // the green bone meal sparkles
                }
            }
        }
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, user.getX(), user.getY() + 1.0, user.getZ(),
                30, radius * 0.4, 0.6, radius * 0.4, 0.0);
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_BONE_MEAL_USE,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    // ---------------- Recall: back to your bed (or world spawn) ----------------
    private boolean recall(ServerWorld world, PlayerEntity user) {
        if (!(user instanceof ServerPlayerEntity sp)) {
            return false;
        }

        BlockPos spawn = sp.getSpawnPointPosition();
        Vec3d target;
        if (spawn != null) {
            if (sp.getSpawnPointDimension() != world.getRegistryKey()) {
                sp.sendMessage(Text.literal("Your spawn point is in another dimension.")
                        .formatted(Formatting.RED), true);
                return false;
            }
            target = Vec3d.ofBottomCenter(spawn.up()); // just above your bed
        } else if (world.getRegistryKey() == World.OVERWORLD) {
            BlockPos ws = world.getSpawnPos();
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, ws.getX(), ws.getZ());
            target = new Vec3d(ws.getX() + 0.5, y, ws.getZ() + 0.5);
        } else {
            sp.sendMessage(Text.literal("No spawn point set here. Sleep in a bed first.")
                    .formatted(Formatting.RED), true);
            return false;
        }

        Vec3d from = user.getPos();
        world.spawnParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 40, 0.4, 0.8, 0.4, 0.3);
        user.requestTeleport(target.x, target.y, target.z);
        user.fallDistance = 0;
        world.spawnParticles(ParticleTypes.PORTAL, target.x, target.y + 1.0, target.z, 40, 0.4, 0.8, 0.4, 0.3);
        world.playSound(null, target.x, target.y, target.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 0.8F);

        if (UpgradeManager.level(user, "safe_return") >= 1) {
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
        }
        return true;
    }

    // ---------------- Clone: decoys that mobs attack ----------------
    private void clone(ServerWorld world, PlayerEntity user) {
        int count = 1 + UpgradeManager.level(user, "twin_clones");
        int duration = CLONE_BASE_DURATION_TICKS + 100 * UpgradeManager.level(user, "lasting_illusion");
        ScrollEffectsManager.spawnClones(world, user, count, duration);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, CLONE_INVISIBLE_TICKS, 0));
    }

    // ---------------- Bridge: temporary glass bridge ----------------
    private boolean bridge(ServerWorld world, PlayerEntity user) {
        int length = BRIDGE_BASE_LENGTH + 4 * UpgradeManager.level(user, "long_bridge");
        int duration = BRIDGE_BASE_DURATION_TICKS + 100 * UpgradeManager.level(user, "lasting_bridge");
        boolean built = ScrollEffectsManager.buildBridge(world, user, length, duration);
        if (!built) {
            user.sendMessage(Text.literal("Nowhere to build a bridge here.").formatted(Formatting.YELLOW), true);
        }
        return built;
    }
}
