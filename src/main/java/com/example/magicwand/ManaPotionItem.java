package com.example.magicwand;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/** Drinkable mana potion. Gives you a glass bottle back. */
public class ManaPotionItem extends Item {
    private final int amount;

    public ManaPotionItem(Settings settings, int amount) {
        super(settings);
        this.amount = amount;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user); // handles drinking + glass bottle

        if (world instanceof ServerWorld sw && user instanceof ServerPlayerEntity player) {
            ManaManager.addMana(player, amount);
            sw.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 0.4, 0.6, 0.4, 0.1);
            player.sendMessage(Text.literal("+" + amount + " mana").formatted(Formatting.AQUA), true);
        }
        return result;
    }
}
