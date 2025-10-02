package net.sansuish.sansenchants.event.enchants;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class HomingArrowHandler {
    private static final String TAG_TARGET = "sansenchants:homing_target";

    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        Level level = arrow.level();
        if (level.isClientSide) return;

        int enchLevel = 1; // TODO: replace with real enchant lookup
        if (enchLevel <= 0) return;

        Entity shooter = arrow.getOwner();
        if (!(shooter instanceof LivingEntity)) return;

        LivingEntity target = getTarget(arrow, (ServerLevel) level, enchLevel);
        if (target != null && target.isAlive()) {
            steerTowards(arrow, target, enchLevel);
        }
    }

    private static LivingEntity getTarget(AbstractArrow arrow, ServerLevel level, int enchLevel) {
        Vec3 pos = arrow.position();
        double range = 24 + enchLevel * 12;
        AABB box = new AABB(pos.x - range, pos.y - range, pos.z - range,
                pos.x + range, pos.y + range, pos.z + range);

        Vec3 arrowDir = arrow.getDeltaMovement().normalize();
        double maxAngle = Math.toRadians(20 + enchLevel * 2); // much smaller cone

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive()
                        && e != arrow.getOwner()
                        && !(e instanceof Player p && p.isCreative())
                        && isWithinCone(arrow, e, arrowDir, maxAngle));

        if (candidates.isEmpty()) return null;

        // find closest candidate
        LivingEntity closest = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(arrow)))
                .orElse(null);

        if (closest == null) return null;

        // stickiness: keep current target unless new one is 20% closer
        if (arrow.getPersistentData().hasUUID(TAG_TARGET)) {
            Entity current = level.getEntity(arrow.getPersistentData().getUUID(TAG_TARGET));
            if (current instanceof LivingEntity cur && cur.isAlive()) {
                double distCur = cur.distanceToSqr(arrow);
                double distNew = closest.distanceToSqr(arrow);

                if (distNew * 1.2 >= distCur) {
                    return cur; // keep current target
                }
            }
        }

        // update to new target
        arrow.getPersistentData().putUUID(TAG_TARGET, closest.getUUID());
        return closest;
    }

    private static boolean isWithinCone(AbstractArrow arrow, Entity target, Vec3 arrowDir, double maxAngle) {
        Vec3 toTarget = target.position().subtract(arrow.position()).normalize();
        double dot = arrowDir.dot(toTarget);
        double angle = Math.acos(dot);
        return angle <= maxAngle;
    }

    private static void steerTowards(AbstractArrow arrow, LivingEntity target, int enchLevel) {
        // aim toward mid-body
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(arrow.position());
        Vec3 curVel = arrow.getDeltaMovement();

        double steerStrength = 0.25 + enchLevel * 0.10;
        if (steerStrength > 0.85) steerStrength = 0.85;

        Vec3 newVel = curVel.scale(1 - steerStrength)
                .add(toTarget.normalize().scale(steerStrength * curVel.length()));
        arrow.setDeltaMovement(newVel);

        float yRot = (float) (Mth.atan2(newVel.x, newVel.z) * (180F / Math.PI));
        float xRot = (float) (Mth.atan2(newVel.y, Math.sqrt(newVel.x * newVel.x + newVel.z * newVel.z)) * (180F / Math.PI));
        arrow.setYRot(yRot);
        arrow.setXRot(xRot);
        arrow.yRotO = yRot;
        arrow.xRotO = xRot;
    }
}
