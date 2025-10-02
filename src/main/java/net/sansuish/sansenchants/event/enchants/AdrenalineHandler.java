package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class AdrenalineHandler {

    private static final ResourceKey<Enchantment> ADRENALINE_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "adrenaline"));

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(ADRENALINE_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        int highestLevel = 0;

        // Look for highest Adrenaline level on any armor piece
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            int lvl = EnchantmentHelper.getTagEnchantmentLevel(holder, stack);
            if (lvl > highestLevel) highestLevel = lvl;
        }

        if (highestLevel <= 0) return;

        // Trigger threshold: below 25% health
        float healthPct = player.getHealth() / player.getMaxHealth();
        if (healthPct > 0.25f) {
            // clear buffs if above threshold
            player.removeEffect(MobEffects.DAMAGE_BOOST);
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            return;
        }

        int duration = 40; // 2 seconds, refreshed every tick

        // Strength scaling
        int strengthAmp = highestLevel - 1; // lvl1 = 0 amp, lvl3 = amp2 (Strength III)
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, strengthAmp, true, false, true));

        // Speed scaling
        int speedAmp = Math.min(2, highestLevel); // up to Speed III
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, speedAmp - 1, true, false, true));
    }
}
