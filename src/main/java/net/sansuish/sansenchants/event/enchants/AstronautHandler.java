package net.sansuish.sansenchants.event.enchants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class AstronautHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Only run on server
        if (player.level().isClientSide) return;

        // Resolve enchantment
        var registry = player.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

        var holderOpt = registry.getHolder(ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "astronaut"));
        if (holderOpt.isEmpty()) return;

        var holder = holderOpt.get();

        // Check all armor pieces, take highest level
        int highestLevel = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            int lvl = EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
            if (lvl > highestLevel) highestLevel = lvl;
        }

        if (highestLevel > 0) {
            int duration = 40; // 2s, refreshed every tick

            // Jump boost is weaker now (half level scaling, capped at 1)
            int jumpAmp = Math.min(1, highestLevel / 2);
            if (jumpAmp > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, jumpAmp - 1, false, false, false));
            }

            // Slow falling only at level 2+ and still weaker (shorter uptime)
            if (highestLevel >= 2) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false, false));
            } else {
                player.removeEffect(MobEffects.SLOW_FALLING);
            }

        } else {
            // Remove effects if enchantment gone
            player.removeEffect(MobEffects.JUMP);
            player.removeEffect(MobEffects.SLOW_FALLING);
        }
    }
}
