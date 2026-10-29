package com.example.magicwand;

import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MagicWandItem extends Item {
    // ---- Tweak these to taste ----
    private static final int FIREBALL_COOLDOWN_TICKS = 10;   // 20 ticks = 1 second
    private static final int TELEPORT_COOLDOWN_TICKS = 40;
    private static final double TELEPORT_MAX_DISTANCE = 12.0; // blocks
    private static final int FIREBALL_MANA_COST = 10;
    private static final int TELEPORT_MANA_COST = 25;
    // ------------------------------

    public MagicWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = user.getStackInHand(hand);
        boolean sneaking = user.isSneaking();
        int cost = sneaking ? TELEPORT_MANA_COST : FIREBALL_MANA_COST;

        if (!ManaManager.tryUse(user, cost)) {
            return ActionResult.FAIL;
        }

        if (sneaking) {
            if (!teleport(sw, user)) {
                ManaManager.refund(user, cost); // fizzled, so no mana lost
            }
            user.getItemCooldownManager().set(stack, TELEPORT_COOLDOWN_TICKS);
        } else {
            shootFireball(sw, user);
            user.getItemCooldownManager().set(stack, FIREBALL_COOLDOWN_TICKS);
        }
        return ActionResult.SUCCESS;
    }

    private void shootFireball(World world, PlayerEntity user) {
        Vec3d look = user.getRotationVec(1.0F);
        Vec3d spawnPos = user.getEyePos().add(look.multiply(1.0));

        SmallFireballEntity fireball = new SmallFireballEntity(world, user, look);
        fireball.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        world.spawnEntity(fireball);

        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    /** Returns false if something blocked the teleport. */
    private boolean teleport(ServerWorld world, PlayerEntity user) {
        Vec3d look = user.getRotationVec(1.0F);
        Box box = user.getBoundingBox();

        double bestDistance = 0;
        for (double d = 0.5; d <= TELEPORT_MAX_DISTANCE; d += 0.5) {
            Box moved = box.offset(look.multiply(d));
            if (!world.isSpaceEmpty(user, moved)) {
                break;
            }
            bestDistance = d;
        }

        if (bestDistance < 1.0) {
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.PLAYERS, 0.8F, 1.2F);
            return false;
        }

        Vec3d from = user.getPos();
        Vec3d to = from.add(look.multiply(bestDistance));

        poof(world, from);
        user.requestTeleport(to.x, to.y, to.z);
        user.fallDistance = 0;
        poof(world, to);

        world.playSound(null, to.x, to.y, to.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private void poof(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0, pos.z, 30, 0.4, 0.8, 0.4, 0.2);
    }
}
