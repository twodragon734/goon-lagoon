package com.example.magicwand;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Spell Book pulls in every spell scroll you are carrying (inventory, hotbar or hands).
 * Right-click: cast the selected scroll. Sneak + right-click: switch to the next scroll.
 * The scrolls must stay in your inventory: if you drop one, the book can't use it.
 */
public class SpellBookItem extends Item {
    // Which scroll each player has selected (index into allScrolls()). Resets on server restart.
    private static final Map<UUID, Integer> SELECTED = new HashMap<>();

    public SpellBookItem(Settings settings) {
        super(settings);
    }

    /** Every scroll type in the mod. Add new scrolls here and the book will pick them up. */
    private static List<Item> allScrolls() {
        return List.of(MagicWandMod.SPEEDSTER_SCROLL, MagicWandMod.DRILL_SCROLL,
                MagicWandMod.GROWTH_SCROLL, MagicWandMod.RECALL_SCROLL,
                MagicWandMod.CLONE_SCROLL, MagicWandMod.BRIDGE_SCROLL);
    }

    private static ItemStack find(PlayerEntity user, Item item) {
        PlayerInventory inv = user.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int countAvailable(PlayerEntity user) {
        int n = 0;
        for (Item scroll : allScrolls()) {
            if (!find(user, scroll).isEmpty()) n++;
        }
        return n;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld sw)) {
            return ActionResult.SUCCESS;
        }

        List<Item> all = allScrolls();
        int available = countAvailable(user);
        if (available == 0) {
            user.sendMessage(Text.literal("No spell scrolls in your inventory!").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        UUID id = user.getUuid();
        int current = SELECTED.getOrDefault(id, -1);

        if (user.isSneaking()) {
            // Switch to the next scroll you are carrying
            for (int step = 1; step <= all.size(); step++) {
                int idx = Math.floorMod(current + step, all.size());
                ItemStack scroll = find(user, all.get(idx));
                if (!scroll.isEmpty()) {
                    SELECTED.put(id, idx);
                    user.sendMessage(Text.literal("Spell Book: ").formatted(Formatting.AQUA)
                            .append(scroll.getName())
                            .append(Text.literal("  (" + available + " scroll" + (available == 1 ? "" : "s") + ")")
                                    .formatted(Formatting.GRAY)), true);
                    break;
                }
            }
            return ActionResult.SUCCESS;
        }

        // Cast the selected scroll. If it is missing, fall back to the first one you have.
        ItemStack scroll = current >= 0 && current < all.size() ? find(user, all.get(current)) : ItemStack.EMPTY;
        if (scroll.isEmpty()) {
            for (int i = 0; i < all.size(); i++) {
                scroll = find(user, all.get(i));
                if (!scroll.isEmpty()) {
                    SELECTED.put(id, i);
                    break;
                }
            }
        }

        if (scroll.isEmpty() || !(scroll.getItem() instanceof ScrollItem scrollItem)) {
            return ActionResult.FAIL;
        }
        if (user.getItemCooldownManager().isCoolingDown(scroll)) {
            user.sendMessage(Text.literal("That scroll is still on cooldown.").formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }
        return scrollItem.cast(sw, user, scroll);
    }
}
