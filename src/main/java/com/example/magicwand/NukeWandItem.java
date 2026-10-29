package com.example.magicwand;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Nuke Wand. Right-click: big blast where you aim. Sneak + right-click: nuke (needs the Nuke upgrade).
 * There is no crafting recipe, so it can only be taken from the creative inventory.
 */
public class NukeWandItem extends Item {
    // ---- Tweak these to taste (20 ticks = 1 second) ----
    private static final boolean CREATIVE_ONLY = true;     // survival players can't use it even if given one
    private static final float BIG_BLAST_BASE_POWER = 6.0F; // TNT is 4. +2 per Blast Power level
    private static final float NUKE_BASE_POWER = 25.0F;     // +5 per Nuke Yield level
    private static final int BIG_BLAST_COOLDOWN_TICKS = 20;
    private static final int NUKE_COOLDOWN_TICKS = 200;
    private static final int BIG_BLAST_MANA_COST = 40;
    private static final int NUKE_MANA_COST = 100;
    private static final double RANGE = 100.0;               // how far you can aim
    // -----------------------------------------------------

    public NukeWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = user.getStackInHand(hand);

        if (CREATIVE_ONLY && !user.isCreative()) {
            user.sendMessage(Text.literal("Only creative mode players can use the Nuke Wand.")
                    .formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        boolean sneaking = user.isSneaking();
        if (sneaking && UpgradeManager.level(user, "nuke") < 1) {
            UpgradeManager.sendLocked(user, "nuke");
            return ActionResult.FAIL;
        }

        if (!ManaManager.tryUse(user, sneaking ? NUKE_MANA_COST : BIG_BLAST_MANA_COST)) {
            return ActionResult.FAIL;
        }

        WandUtil.Aim aim = WandUtil.aim(sw, user, RANGE, false);
        Vec3d p = aim.point();
        boolean fire = UpgradeManager.level(user, "incendiary") >= 1;

        if (sneaking) {
            float power = NUKE_BASE_POWER + 5.0F * UpgradeManager.level(user, "nuke_yield");
            nuke(sw, user, p, power, fire);
            user.getItemCooldownManager().set(stack, NUKE_COOLDOWN_TICKS);
        } else {
            float power = BIG_BLAST_BASE_POWER + 2.0F * UpgradeManager.level(user, "blast_power");
            bigBlast(sw, user, p, power, fire);
            user.getItemCooldownManager().set(stack, BIG_BLAST_COOLDOWN_TICKS);
        }
        return ActionResult.SUCCESS;
    }

    private void bigBlast(ServerWorld world, PlayerEntity user, Vec3d p, float power, boolean fire) {
        WandUtil.trail(world, user.getEyePos().add(0, -0.2, 0), p, ParticleTypes.FLAME);
        world.createExplosion(user, p.x, p.y, p.z, power, fire, World.ExplosionSourceType.TNT);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }

    private void nuke(ServerWorld world, PlayerEntity user, Vec3d p, float power, boolean fire) {
        WandUtil.trail(world, user.getEyePos().add(0, -0.2, 0), p, ParticleTypes.FLAME);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                SoundCategory.PLAYERS, 4.0F, 0.5F);

        world.createExplosion(user, p.x, p.y, p.z, power, fire, World.ExplosionSourceType.TNT);

        // Mushroom cloud: a rising stem with a wide cap
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 1, p.z, 6, power * 0.15, 1, power * 0.15, 0);
        for (int i = 0; i < 14; i++) {
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 2 + i * 1.6, p.z, 14, 1.5, 0.8, 1.5, 0.03);
            world.spawnParticles(ParticleTypes.FLAME, p.x, p.y + 2 + i * 1.6, p.z, 6, 1.0, 0.6, 1.0, 0.04);
        }
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 26, p.z, 120, 7.0, 2.0, 7.0, 0.05);
        world.spawnParticles(ParticleTypes.FLASH, p.x, p.y + 3, p.z, 1, 0, 0, 0, 0);
    }
}
