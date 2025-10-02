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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class EarthenGripHandler {

    private static final ResourceKey<Enchantment> EARTHEN_GRIP_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "earthen_grip"));

    // Track lingering timers per player
    private static final Map<UUID, Integer> lingerTimers = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(EARTHEN_GRIP_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        ItemStack boots = player.getInventory().getArmor(0);
        if (boots.isEmpty()) {
            clearEffects(player);
            return;
        }

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, boots);
        if (enchLevel <= 0) {
            clearEffects(player);
            return;
        }

        BlockState below = player.level().getBlockState(player.blockPosition().below());

        boolean onEarth =
                below.is(Blocks.STONE) || below.is(Blocks.DEEPSLATE) || below.is(Blocks.DIRT) ||
                        below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.MOSS_BLOCK) ||
                        below.is(Blocks.PACKED_MUD) || below.is(Blocks.CLAY);

        int duration = 40; // 2s, refreshed constantly

        if (onEarth) {
            // Resistance scales: L1=I, L2=II, L3=III
            int resistanceAmp = enchLevel - 1;

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resistanceAmp, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0, true, false, true)); // Slowness I

            // Reset linger timer
            lingerTimers.put(player.getUUID(), 20); // 20 ticks = 1s linger
        } else {
            int ticks = lingerTimers.getOrDefault(player.getUUID(), 0);
            if (ticks > 0) {
                // Keep both effects active during linger
                int resistanceAmp = enchLevel - 1;
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, resistanceAmp, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false, true));

                lingerTimers.put(player.getUUID(), ticks - 1);
            } else {
                clearEffects(player);
            }
        }
    }

    private static void clearEffects(Player player) {
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        lingerTimers.remove(player.getUUID());
    }
}