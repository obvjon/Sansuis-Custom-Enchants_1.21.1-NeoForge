package net.sansuish.sansenchants.event.enchants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.sansuish.sansenchants.SansEnchants;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class TridentTeleporterHandler {

    @SubscribeEvent
    public static void onTridentImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownTrident trident)) return;

        Entity owner = trident.getOwner();
        if (!(owner instanceof Player player)) return;
        if (player.level().isClientSide) return;

        ItemStack stack = trident.getWeaponItem();
        if (stack.isEmpty()) return;

        var registry = player.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

        var holderOpt = registry.getHolder(ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "warp_trident"));
        if (holderOpt.isEmpty()) return;

        var holder = holderOpt.get();
        int level = EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        if (level <= 0) return;

        HitResult hit = event.getRayTraceResult();
        if (hit == null) return;
        Vec3 hitPos = hit.getLocation();
        if (hitPos == null) return;

        // Teleport
        player.teleportTo(hitPos.x, hitPos.y, hitPos.z);

        // Distance-based exhaustion
        double distance = player.position().distanceTo(hitPos);
        float exhaustion = Math.min(2.5F + (float)(distance / 10.0F) * 2.5F, 20.0F);
        player.causeFoodExhaustion(exhaustion);

        // Sound & particles
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.level().levelEvent(2003, player.blockPosition(), 0);

        // Cooldown scaling per player
        int cooldownTicks = switch (level) {
            case 1 -> 120; // 6s
            case 2 -> 80;  // 4s
            case 3 -> 40;  // 2s
            default -> 120;
        };

        // ✅ Per-player cooldown indicator (shows sweep in hotbar)
        player.getCooldowns().addCooldown(stack.getItem(), cooldownTicks);

        // Return trident
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
        trident.discard();
    }
}
