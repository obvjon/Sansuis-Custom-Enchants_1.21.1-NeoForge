package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class EchoStrikeHandler {

    private static final ResourceKey<Enchantment> ECHO_STRIKE_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "echo_strike"));

    private static final List<PendingEcho> PENDING = new ArrayList<>();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        LivingEntity target = event.getEntity();
        if (target == null || !target.isAlive()) return;

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        Optional<? extends Holder.Reference<Enchantment>> maybeHolder =
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(ECHO_STRIKE_KEY);
        if (maybeHolder.isEmpty()) return;
        Holder<Enchantment> holder = maybeHolder.get();

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, weapon);
        if (enchLevel <= 0) return;

        // Buffed proc chance: 25% / 50% / 75%
        float chance = 0.25f * enchLevel;
        if (player.getRandom().nextFloat() > chance) return;

        // Damage scaling: L1=50%, L2=75%, L3=100%
        float multiplier = 0.5f + 0.25f * (enchLevel - 1);
        float damage = event.getNewDamage() * multiplier;

        // Delay (30 ticks ~ 1.5s)
        PENDING.add(new PendingEcho(level, player.getUUID(), target.getUUID(), damage, 30));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;

        // iterate over a snapshot to avoid ConcurrentModificationExceptions
        List<PendingEcho> snapshot = new ArrayList<>(PENDING);

        for (PendingEcho echo : snapshot) {
            // decrement delay on the actual object (snapshot contains same reference)
            echo.delay--;

            if (echo.delay <= 0) {
                // Process and remove from the real list inside try/finally to guarantee cleanup
                try {
                    ServerLevel level = echo.level;
                    if (level == null) {
                        continue;
                    }

                    Player player = level.getPlayerByUUID(echo.playerId);

                    // Lookup entity by UUID and ensure it's a LivingEntity
                    Entity entity = level.getEntity(echo.targetId);
                    if (!(entity instanceof LivingEntity target)) {
                        // target not present or not a living entity — skip
                        continue;
                    }

                    if (player != null && target != null && target.isAlive()) {
                        // reset invulnerability frames so echo can hit
                        target.invulnerableTime = 0;

                        DamageSources sources = level.damageSources();
                        DamageSource echoSource = sources.playerAttack(player);

                        // Apply echo damage
                        target.hurt(echoSource, echo.damage);

                        // small knockback toward away from player
                        Vec3 dir = target.position().subtract(player.position());
                        if (dir.lengthSqr() > 0.0001) {
                            Vec3 norm = dir.normalize();
                            target.push(norm.x * 0.2, 0.1, norm.z * 0.2);
                        }

                        // vanilla sweep effect at the target (server-side)
                        level.levelEvent(2003, target.blockPosition(), 0);
                    }
                } catch (Throwable ex) {
                    // Prevent a single bad echo from crashing the server.
                    ex.printStackTrace();
                } finally {
                    // always remove processed echo from the real list
                    PENDING.remove(echo);
                }
            }
        }
    }

    private static class PendingEcho {
        final ServerLevel level;
        final UUID playerId;
        final UUID targetId;
        final float damage;
        int delay;

        PendingEcho(ServerLevel level, UUID playerId, UUID targetId, float damage, int delay) {
            this.level = level;
            this.playerId = playerId;
            this.targetId = targetId;
            this.damage = damage;
            this.delay = delay;
        }
    }
}
