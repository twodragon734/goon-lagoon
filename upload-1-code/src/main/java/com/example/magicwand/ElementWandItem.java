package com.example.magicwand;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ElementWandItem extends Item {
    public enum Kind { LIGHTNING, ICE, WIND, HEAL }

    private final Kind kind;

    public ElementWandItem(Settings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    // ---- Tweak these to taste (20 ticks = 1 second) ----
    private int cooldownTicks() {
        return switch (kind) {
            case LIGHTNING -> 40;
            case ICE -> 30;
            case WIND -> 40;
            case HEAL -> 60;
        };
    }

    private int manaCost(PlayerEntity user) {
        return switch (kind) {
            case LIGHTNING -> 30;
            case ICE -> 15;
            case WIND -> user.isSneaking() ? 25 : 20;
            case HEAL -> 25;
        };
    }
    // -----------------------------------------------------

    private String requiredUpgrade(PlayerEntity user) {
        return switch (kind) {
            case LIGHTNING -> "lightning_strike";
            case ICE -> "frost_bolt";
            case WIND -> user.isSneaking() ? "tempest" : "gust";
            case HEAL -> "healing_touch";
        };
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = user.getStackInHand(hand);

        String required = requiredUpgrade(user);
        if (UpgradeManager.level(user, required) < 1) {
            UpgradeManager.sendLocked(user, required);
            return ActionResult.FAIL;
        }

        if (!ManaManager.tryUse(user, manaCost(user))) {
            return ActionResult.FAIL;
        }

        switch (kind) {
            case LIGHTNING -> lightning(sw, user);
            case ICE -> ice(sw, user);
            case WIND -> wind(sw, user);
            case HEAL -> heal(sw, user);
        }
        user.getItemCooldownManager().set(stack, cooldownTicks());
        return ActionResult.SUCCESS;
    }

    // ---------------- Lightning: strike where you look ----------------
    private void lightning(ServerWorld world, PlayerEntity user) {
        double range = 60 + 20.0 * UpgradeManager.level(user, "long_reach");
        int strikes = 1 + UpgradeManager.level(user, "storm_caller");

        WandUtil.Aim aim = WandUtil.aim(world, user, range, false);
        Vec3d p = aim.point();

        for (int i = 0; i < strikes; i++) {
            Vec3d spot = p;
            if (i > 0) {
                // extra bolts land randomly near the target
                spot = p.add((world.random.nextDouble() - 0.5) * 6.0, 0, (world.random.nextDouble() - 0.5) * 6.0);
            }
            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.refreshPositionAfterTeleport(spot.x, spot.y, spot.z);
                if (user instanceof ServerPlayerEntity sp) {
                    bolt.setChanneler(sp);
                }
                world.spawnEntity(bolt);
            }
        }
    }

    // ---------------- Ice: freeze mobs, water, and cover ground in snow ----------------
    private void ice(ServerWorld world, PlayerEntity user) {
        int freezeTicks = 120 + 60 * UpgradeManager.level(user, "deep_freeze");
        int radius = 2 + UpgradeManager.level(user, "blizzard");

        WandUtil.Aim aim = WandUtil.aim(world, user, 30, true);
        Vec3d p = aim.point();

        Vec3d start = user.getEyePos().add(0, -0.2, 0);
        WandUtil.trail(world, start, p, ParticleTypes.SNOWFLAKE);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.PLAYERS, 0.8F, 1.6F);

        if (aim.entity() instanceof LivingEntity target) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, freezeTicks, 6), user);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, freezeTicks, 0), user);
            target.setFrozenTicks(freezeTicks + 80);
            target.damage(world, world.getDamageSources().freeze(), 3.0F);
            world.spawnParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getBodyY(0.5), target.getZ(),
                    25, 0.4, 0.6, 0.4, 0.05);
        }

        BlockPos center = BlockPos.ofFloored(p);
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -1, -radius), center.add(radius, 1, radius))) {
            BlockPos fixed = pos.toImmutable();
            if (world.getBlockState(fixed).isOf(Blocks.WATER)) {
                world.setBlockState(fixed, Blocks.ICE.getDefaultState());
            } else if (world.getBlockState(fixed).isAir()
                    && Blocks.SNOW.getDefaultState().canPlaceAt(world, fixed)) {
                world.setBlockState(fixed, Blocks.SNOW.getDefaultState());
            }
        }
    }

    // ---------------- Wind: launch yourself, or sneak for a shockwave ----------------
    private void wind(ServerWorld world, PlayerEntity user) {
        Vec3d look = user.getRotationVec(1.0F);
        double power = 1.0 + 0.2 * UpgradeManager.level(user, "lift_power");

        if (user.isSneaking()) {
            // Shockwave: push everything in front of you away
            for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class,
                    user.getBoundingBox().expand(8.0), x -> x != user && x.isAlive())) {
                Vec3d to = e.getPos().subtract(user.getPos());
                if (to.length() > 8.0 || to.normalize().dotProduct(look) < 0.4) continue;
                e.addVelocity(look.x * 2.0 * power, 0.6 * power, look.z * 2.0 * power);
                e.velocityModified = true;
            }
            for (int i = 1; i <= 8; i++) {
                Vec3d p = user.getEyePos().add(look.multiply(i));
                world.spawnParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 4, 0.3, 0.3, 0.3, 0.02);
            }
        } else {
            // Launch yourself
            user.setVelocity(look.x * 0.5, 1.2 * power, look.z * 0.5);
            user.velocityModified = true;
            user.fallDistance = 0;
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
            world.spawnParticles(ParticleTypes.CLOUD, user.getX(), user.getY(), user.getZ(),
                    25, 0.4, 0.1, 0.4, 0.05);
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
                SoundCategory.PLAYERS, 1.0F, 1.2F);
    }

    // ---------------- Heal: heal whatever you point at, or yourself ----------------
    private void heal(ServerWorld world, PlayerEntity user) {
        float amount = 8.0F + 4.0F * UpgradeManager.level(user, "greater_heal");
        int regenLevel = 1 + UpgradeManager.level(user, "renewal");

        WandUtil.Aim aim = WandUtil.aim(world, user, 20, false);
        LivingEntity target = aim.entity() instanceof LivingEntity le ? le : user;

        target.heal(amount);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, regenLevel));
        world.spawnParticles(ParticleTypes.HEART, target.getX(), target.getBodyY(0.5) + 0.5, target.getZ(),
                8, 0.4, 0.4, 0.4, 0.05);
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }
}
