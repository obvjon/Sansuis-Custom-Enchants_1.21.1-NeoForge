package net.sansuish.sansenchants.event.enchants;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
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

import java.util.*;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class PhotosynthesisHandler {

    private static final ResourceKey<Enchantment> PHOTOSYNTHESIS_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "photosynthesis"));

    // Per-player sunlight energy storage
    private static final Map<UUID, Integer> SOLAR_CHARGE = new HashMap<>();
    private static final int MAX_CHARGE = 2400; // ~2 minutes worth of ticks

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        // Find if player has photosynthesis enchant
        ItemStack chest = player.getInventory().getArmor(2); // chestplate slot
        if (chest.isEmpty()) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(PHOTOSYNTHESIS_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, chest);
        if (enchLevel <= 0) return;

        boolean isDay = level.isDay();
        boolean inSunlight = isDay && level.canSeeSky(player.blockPosition());

        // Initialize solar charge if missing
        SOLAR_CHARGE.putIfAbsent(player.getUUID(), 0);
        int charge = SOLAR_CHARGE.get(player.getUUID());

        // 🌞 Daytime effects
        if (inSunlight) {
            // Feed hunger very slowly (every 2 minutes ~ 2400 ticks)
            if (player.tickCount % 2400 == 0 && player.getFoodData().getFoodLevel() < 20) {
                player.getFoodData().eat(1, 0.0f); // no saturation, just hunger pip
            }

            // Only L3 accumulates solar charge
            if (enchLevel >= 3) {
                charge = Math.min(MAX_CHARGE, charge + 1); // 1 tick of charge per tick in sunlight
            }
        }

        // 🌙 Nighttime effects if charged
        if (!isDay && enchLevel >= 3 && charge > 0) {
            // Night Vision (long enough to prevent flashing)
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
            // Glowing aura
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, true));

            charge = Math.max(0, charge - 1); // drain 1 per tick
        }

        SOLAR_CHARGE.put(player.getUUID(), charge);
    }
}
