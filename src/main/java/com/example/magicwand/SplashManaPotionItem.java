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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Splash mana potion. Right-click to throw it where you are looking (it lands instantly),
 * and everyone standing near the splash gets mana, you included if you are close.
 */
public class SplashManaPotionItem extends Item {
    // ---- Tweak these to taste ----
    private static final int MANA_AMOUNT = 40;
    private static final double SPLASH_RADIUS = 4.0;
    private static final double THROW_RANGE = 20.0;
    // ------------------------------

    public SplashManaPotionItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = user.getStackInHand(hand);

        WandUtil.Aim aim = WandUtil.aim(sw, user, THROW_RANGE, false);
        Vec3d p = aim.point();

        // Flight trail, then the splash
        WandUtil.trail(sw, user.getEyePos().add(0, -0.2, 0), p, ParticleTypes.ENCHANT);
        sw.spawnParticles(ParticleTypes.SPLASH, p.x, p.y + 0.2, p.z, 50, 0.6, 0.3, 0.6, 0.1);
        sw.spawnParticles(ParticleTypes.ENCHANT, p.x, p.y + 0.5, p.z, 40, 0.6, 0.6, 0.6, 0.2);
        sw.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(p),
                SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);

        Box area = new Box(p, p).expand(SPLASH_RADIUS);
        for (PlayerEntity target : sw.getEntitiesByClass(PlayerEntity.class, area, e -> true)) {
            if (target.squaredDistanceTo(p) <= SPLASH_RADIUS * SPLASH_RADIUS) {
                ManaManager.addMana(target, MANA_AMOUNT);
                target.sendMessage(Text.literal("+" + MANA_AMOUNT + " mana").formatted(Formatting.AQUA), true);
            }
        }

        user.getItemCooldownManager().set(stack, 10);
        stack.decrementUnlessCreative(1, user);
        return ActionResult.SUCCESS;
    }
}
