package com.example.magicwand;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class WandUtil {
    private WandUtil() {}

    /** Where the player is pointing: the point hit, and the entity hit (or null). */
    public record Aim(Vec3d point, Entity entity) {}

    public static Aim aim(World world, PlayerEntity user, double range, boolean hitFluids) {
        Vec3d start = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(range));

        BlockHitResult blockHit = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                hitFluids ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE,
                user));
        Vec3d blockPoint = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();
        double blockDistSq = start.squaredDistanceTo(blockPoint);

        Box box = user.getBoundingBox().stretch(look.multiply(range)).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.raycast(
                user, start, end, box,
                e -> !e.isSpectator() && e.canHit(),
                range * range);

        if (entityHit != null && start.squaredDistanceTo(entityHit.getPos()) < blockDistSq) {
            return new Aim(entityHit.getPos(), entityHit.getEntity());
        }
        return new Aim(blockPoint, null);
    }

    /** Draws a line of particles from start to end. */
    public static void trail(ServerWorld world, Vec3d start, Vec3d end, ParticleEffect particle) {
        Vec3d diff = end.subtract(start);
        double length = diff.length();
        if (length < 0.1) return;
        Vec3d dir = diff.normalize();
        for (double d = 1.0; d < length; d += 0.5) {
            Vec3d p = start.add(dir.multiply(d));
            world.spawnParticles(particle, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }
}
