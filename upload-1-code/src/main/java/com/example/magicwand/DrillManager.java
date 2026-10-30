package com.example.magicwand;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Runs the "drills" summoned by the Drill Scroll, a little bit each server tick. */
public final class DrillManager {
    private DrillManager() {}

    private static final double SPEED = 0.5; // blocks per tick (10 blocks/second)

    private static final class Drill {
        final ServerWorld world;
        final Entity owner;
        final Vec3d dir;
        final double maxDistance;
        final double radius;
        Vec3d pos;
        double travelled = 0;
        int ticks = 0;

        Drill(ServerWorld world, Entity owner, Vec3d pos, Vec3d dir, double maxDistance, double radius) {
            this.world = world;
            this.owner = owner;
            this.pos = pos;
            this.dir = dir;
            this.maxDistance = maxDistance;
            this.radius = radius;
        }
    }

    private static final List<Drill> DRILLS = new ArrayList<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> DRILLS.clear());
    }

    public static void start(ServerWorld world, PlayerEntity user, double maxDistance, double radius) {
        Vec3d look = user.getRotationVec(1.0F);
        // Start a little in front of the player so it doesn't dig out from under their feet.
        Vec3d start = user.getEyePos().add(look.multiply(2.5));
        DRILLS.add(new Drill(world, user, start, look, maxDistance, radius));
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_PISTON_EXTEND,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
    }

    private static void tick() {
        Iterator<Drill> it = DRILLS.iterator();
        while (it.hasNext()) {
            if (!advance(it.next())) {
                it.remove();
            }
        }
    }

    /** Moves one drill forward and breaks blocks. Returns false when it is finished. */
    private static boolean advance(Drill d) {
        ServerWorld w = d.world;
        d.pos = d.pos.add(d.dir.multiply(SPEED));
        d.travelled += SPEED;
        d.ticks++;

        BlockPos headPos = BlockPos.ofFloored(d.pos);
        BlockState headState = w.getBlockState(headPos);
        if (!headState.isAir() && headState.getHardness(w, headPos) < 0) {
            return false; // hit bedrock or similar
        }

        int r = (int) Math.ceil(d.radius);
        for (BlockPos p : BlockPos.iterate(headPos.add(-r, -r, -r), headPos.add(r, r, r))) {
            if (Vec3d.ofCenter(p).squaredDistanceTo(d.pos) > d.radius * d.radius) continue;

            BlockState s = w.getBlockState(p);
            if (s.isAir()) continue;
            if (!s.getFluidState().isEmpty()) continue;   // leave water/lava alone
            if (s.getHardness(w, p) < 0) continue;        // bedrock, barriers...
            if (w.getBlockEntity(p) != null) continue;    // protect chests, furnaces, spawners...

            w.breakBlock(p.toImmutable(), true, d.owner);
        }

        w.spawnParticles(ParticleTypes.CRIT, d.pos.x, d.pos.y, d.pos.z, 6, 0.6, 0.6, 0.6, 0.1);
        w.spawnParticles(ParticleTypes.SMOKE, d.pos.x, d.pos.y, d.pos.z, 4, 0.6, 0.6, 0.6, 0.02);
        if (d.ticks % 4 == 0) {
            w.playSound(null, headPos, SoundEvents.BLOCK_GRINDSTONE_USE,
                    SoundCategory.BLOCKS, 1.0F, 0.8F);
        }

        return d.travelled < d.maxDistance;
    }
}
