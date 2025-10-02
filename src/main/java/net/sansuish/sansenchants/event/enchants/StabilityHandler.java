package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class StabilityHandler {

    private static final ResourceKey<Enchantment> STABILITY_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "stability"));

    // Level 1: Prevent farmland trampling
    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(STABILITY_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        ItemStack boots = player.getInventory().getArmor(0); // boots slot
        if (boots.isEmpty()) return;

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, boots);
        if (enchLevel >= 1) {
            event.setCanceled(true); // farmland never tramples
        }
    }

    // Level 2: Powder snow immunity
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(STABILITY_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        ItemStack boots = player.getInventory().getArmor(0);
        if (boots.isEmpty()) return;

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, boots);
        if (enchLevel < 2) return;

        BlockState below = player.level().getBlockState(player.blockPosition().below());
        if (below.is(Blocks.POWDER_SNOW)) {
            event.setCanceled(true); // cancel fall damage
            player.fallDistance = 0f;
            // prevents sinking
            player.setOnGround(true);
        }
    }

    // Level 3: Knockback resistance effect
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(STABILITY_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        ItemStack boots = player.getInventory().getArmor(0);
        if (boots.isEmpty()) return;

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, boots);
        if (enchLevel < 3) {
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            return;
        }

        // Apply knockback resistance as a permanent effect
        int duration = 40; // 2s, refreshed every tick
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, true, false, true));
    }
}
