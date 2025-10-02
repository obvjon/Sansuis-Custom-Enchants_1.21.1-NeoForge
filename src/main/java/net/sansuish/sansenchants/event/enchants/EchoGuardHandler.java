package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.*;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class EchoGuardHandler {

    private static final ResourceKey<Enchantment> ECHOGUARD_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "echo_guard"));

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        ItemStack shield = player.getUseItem();
        if (shield.isEmpty()) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(ECHOGUARD_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, shield);
        if (enchLevel <= 0) return;

        // Compute time window
        int windowTicks = switch (enchLevel) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 15;
            default -> 5;
        };

        // How long shield has been held
        int useTicks = player.getUseItemRemainingTicks();
        int totalDuration = shield.getUseDuration(player);
        int ticksHeld = totalDuration - useTicks;

        if (ticksHeld <= windowTicks) {
            // ✅ Successful parry
            LivingEntity attacker = event.getDamageSource().getEntity() instanceof LivingEntity living ? living : null;
            if (attacker != null) {
                // Retaliatory damage
                float retaliateDamage = 1.0F + enchLevel;
                attacker.hurt(level.damageSources().playerAttack(player), retaliateDamage);

                // Knockback
                Vec3 push = attacker.position().subtract(player.position()).normalize().scale(0.5D + (0.2D * enchLevel));
                attacker.push(push.x, 0.2D, push.z);

                // Sounds
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.2F);

                // 🌟 Visual feedback
                // Crit particles at player
                for (int i = 0; i < 10; i++) {
                    double dx = player.getRandom().nextGaussian() * 0.2;
                    double dy = player.getRandom().nextGaussian() * 0.2;
                    double dz = player.getRandom().nextGaussian() * 0.2;
                    level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getEyeY(), player.getZ(), 1, dx, dy, dz, 0.1);
                }

                // Sweep particles around attacker
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        attacker.getX(), attacker.getY(0.5), attacker.getZ(),
                        5, 0.3, 0.3, 0.3, 0.1);

                // Brief glowing effect on attacker
                attacker.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, true));
            }
        }
    }
}
