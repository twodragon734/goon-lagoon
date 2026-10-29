package com.example.magicwand;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Spell scrolls. They are never used up: right-click as often as the cooldown and mana allow. */
public class ScrollItem extends Item {
    public enum Kind { SPEED, DRILL }

    private final Kind kind;

    public ScrollItem(Settings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    // ---- Tweak these to taste (20 ticks = 1 second) ----
    private static final int SPEED_BASE_DURATION_TICKS = 1200; // 60 seconds (+30s per Extended Haste level)
    private static final int SPEED_BASE_LEVEL = 2;             // 2 = Speed III (+1 per Hyper Speed level)
    private static final int SPEED_COOLDOWN_TICKS = 1800;      // 90 second cooldown
    private static final int DRILL_COOLDOWN_TICKS = 300;       // 15 second cooldown
    private static final double DRILL_BASE_DISTANCE = 30.0;    // +15 per Deep Bore level
    private static final double DRILL_BASE_RADIUS = 1.6;       // +0.5 per Wide Bore level
    // -----------------------------------------------------

    private int manaCost() {
        return kind == Kind.SPEED ? 40 : 50;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = user.getStackInHand(hand);

        String required = kind == Kind.SPEED ? "swift_step" : "drill_bore";
        if (UpgradeManager.level(user, required) < 1) {
            UpgradeManager.sendLocked(user, required);
            return ActionResult.FAIL;
        }

        if (!ManaManager.tryUse(user, manaCost())) {
            return ActionResult.FAIL;
        }

        switch (kind) {
            case SPEED -> speed(sw, user);
            case DRILL -> {
                double distance = DRILL_BASE_DISTANCE + 15.0 * UpgradeManager.level(user, "deep_bore");
                double radius = DRILL_BASE_RADIUS + 0.5 * UpgradeManager.level(user, "wide_bore");
                DrillManager.start(sw, user, distance, radius);
            }
        }

        int cooldown = kind == Kind.SPEED ? SPEED_COOLDOWN_TICKS : DRILL_COOLDOWN_TICKS;
        user.getItemCooldownManager().set(stack, cooldown);
        return ActionResult.SUCCESS; // note: the scroll is NOT consumed
    }

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
}
