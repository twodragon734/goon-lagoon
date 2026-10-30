package com.example.magicwand;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runs the Clone Scroll decoys and the Bridge Scroll bridges, a little bit each server tick. */
public final class ScrollEffectsManager {
    private ScrollEffectsManager() {}

    private static final class Clone {
        final ServerWorld world;
        final ArmorStandEntity stand;
        int ticksLeft;

        Clone(ServerWorld world, ArmorStandEntity stand, int ticksLeft) {
            this.world = world;
            this.stand = stand;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final class Bridge {
        final ServerWorld world;
        final List<BlockPos> blocks;
        int ticksLeft;

        Bridge(ServerWorld world, List<BlockPos> blocks, int ticksLeft) {
            this.world = world;
            this.blocks = blocks;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final List<Clone> CLONES = new ArrayList<>();
    private static final List<Bridge> BRIDGES = new ArrayList<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickClones();
            tickBridges();
        });

        // Clean up so nothing is left behind when the world closes.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Clone c : CLONES) {
                c.stand.discard();
            }
            CLONES.clear();
            for (Bridge b : BRIDGES) {
                removeBridgeBlocks(b);
            }
            BRIDGES.clear();
        });
    }

    // ------------------------------------------------------------------
    // Clone Scroll: armor-stand decoys that hostile mobs attack instead of you
    // ------------------------------------------------------------------
    public static void spawnClones(ServerWorld world, PlayerEntity user, int count, int durationTicks) {
        String name = user.getName().getString() + "'s Clone";

        for (int i = 0; i < count; i++) {
            double angle = user.getYaw() * Math.PI / 180.0 + (2.0 * Math.PI * i) / count + Math.PI / 2.0;
            double x = user.getX() + Math.cos(angle) * 1.5;
            double z = user.getZ() + Math.sin(angle) * 1.5;

            ArmorStandEntity stand = new ArmorStandEntity(world, x, user.getY(), z);

            // Locked down: no hitbox to click, can't be hurt, equipment can't be taken (no item duplication).
            NbtCompound tag = new NbtCompound();
            tag.putBoolean("Marker", true);
            tag.putBoolean("Invulnerable", true);
            tag.putInt("DisabledSlots", 4144959);
            stand.readNbt(tag);

            // Copy your gear for looks only.
            for (EquipmentSlot slot : new EquipmentSlot[] {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                stand.equipStack(slot, user.getEquippedStack(slot).copy());
            }
            if (stand.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                stand.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
            }

            stand.setCustomName(Text.literal(name));
            stand.setCustomNameVisible(false);
            stand.addCommandTag("magicwand_clone");
            stand.refreshPositionAndAngles(x, user.getY(), z, user.getYaw(), 0.0F);

            CLONES.add(new Clone(world, stand, durationTicks)); // track it before it appears
            world.spawnEntity(stand);
            world.spawnParticles(ParticleTypes.POOF, x, user.getY() + 1.0, z, 15, 0.3, 0.5, 0.3, 0.05);
        }
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL,
                SoundCategory.PLAYERS, 1.0F, 1.2F);
    }

    private static void tickClones() {
        Iterator<Clone> it = CLONES.iterator();
        while (it.hasNext()) {
            Clone c = it.next();
            if (c.stand.isRemoved()) {
                it.remove();
                continue;
            }
            c.ticksLeft--;

            // Every quarter second, pull nearby hostile mobs onto the decoy.
            if (c.ticksLeft % 5 == 0) {
                Box area = new Box(c.stand.getBlockPos()).expand(20.0);
                for (HostileEntity mob : c.world.getEntitiesByClass(HostileEntity.class, area, e -> true)) {
                    LivingEntity target = mob.getTarget();
                    if (target == null || target instanceof PlayerEntity) {
                        mob.setTarget(c.stand);
                    }
                }
            }

            if (c.ticksLeft <= 0) {
                c.world.spawnParticles(ParticleTypes.POOF, c.stand.getX(), c.stand.getY() + 1.0, c.stand.getZ(),
                        20, 0.3, 0.5, 0.3, 0.05);
                c.stand.discard();
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------
    // Bridge Scroll: a temporary glass bridge in front of you
    // ------------------------------------------------------------------
    /** Returns false if there was nothing to build. */
    public static boolean buildBridge(ServerWorld world, PlayerEntity user, int length, int durationTicks) {
        Vec3d dir = Vec3d.fromPolar(0.0F, user.getYaw());   // the way you are facing, flat
        Vec3d perp = new Vec3d(-dir.z, 0.0, dir.x);
        int y = user.getBlockPos().getY() - 1;               // one below your feet

        Set<BlockPos> spots = new LinkedHashSet<>();
        for (int i = 1; i <= length; i++) {
            for (int w = -1; w <= 1; w++) {                  // 3 blocks wide
                Vec3d c = user.getPos().add(dir.multiply(i)).add(perp.multiply(w));
                BlockPos pos = new BlockPos(MathHelper.floor(c.x), y, MathHelper.floor(c.z));
                if (world.getBlockState(pos).isAir()) {
                    spots.add(pos);
                }
            }
        }
        if (spots.isEmpty()) {
            return false;
        }

        List<BlockPos> placed = new ArrayList<>(spots);
        for (BlockPos pos : placed) {
            world.setBlockState(pos, Blocks.CYAN_STAINED_GLASS.getDefaultState());
            world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    1, 0.2, 0.1, 0.2, 0.0);
        }
        BRIDGES.add(new Bridge(world, placed, durationTicks));
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 1.5F, 1.0F);
        return true;
    }

    private static void tickBridges() {
        Iterator<Bridge> it = BRIDGES.iterator();
        while (it.hasNext()) {
            Bridge b = it.next();
            b.ticksLeft--;
            if (b.ticksLeft <= 0) {
                removeBridgeBlocks(b);
                it.remove();
            }
        }
    }

    private static void removeBridgeBlocks(Bridge b) {
        for (BlockPos pos : b.blocks) {
            // Only remove our own glass, never something the player built there since.
            if (b.world.getBlockState(pos).isOf(Blocks.CYAN_STAINED_GLASS)) {
                b.world.removeBlock(pos, false);
                b.world.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        2, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }
}
