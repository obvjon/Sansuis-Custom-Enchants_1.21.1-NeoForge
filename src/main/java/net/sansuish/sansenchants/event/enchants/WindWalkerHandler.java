package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class WindWalkerHandler {

    private static final ResourceKey<Enchantment> WIND_WALKER_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "wind_walker"));

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        var registry = serverPlayer.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(WIND_WALKER_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        int level = 0;
        for (ItemStack armor : serverPlayer.getArmorSlots()) {
            level += EnchantmentHelper.getTagEnchantmentLevel(holder, armor);
        }
        if (level <= 0) return;

        // Apply Speed effect only while Elytra flying
        if (serverPlayer.isFallFlying()) {
            int amplifier = Math.min(level, 3) - 1; // L1 = Speed I, L2 = Speed II, L3 = Speed III

            serverPlayer.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    10,       // duration (ticks), refreshed every tick
                    amplifier,
                    true,     // ambient (clean look)
                    false     // no particles
            ));
        }
    }
}
