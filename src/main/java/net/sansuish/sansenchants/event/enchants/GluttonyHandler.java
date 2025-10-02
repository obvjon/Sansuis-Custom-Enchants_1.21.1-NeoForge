package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class GluttonyHandler {

    private static final ResourceKey<Enchantment> GLUTTONY_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "gluttony"));

    // Track last feed tick for each player
    private static final Map<UUID, Integer> lastFeedTick = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Check enchant level on armor
        var registry = serverPlayer.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(GLUTTONY_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        int level = 0;
        for (ItemStack armor : serverPlayer.getArmorSlots()) {
            level += EnchantmentHelper.getTagEnchantmentLevel(holder, armor);
        }
        if (level <= 0) return;

        FoodData foodData = serverPlayer.getFoodData();
        if (foodData.getFoodLevel() >= 20) return; // already full

        // Cooldown times based on level
        int cooldown;
        switch (level) {
            case 1 -> cooldown = 2400; // 2 minutes
            case 2 -> cooldown = 900;  // 45 seconds
            case 3 -> cooldown = 300;  // 15 seconds
            default -> cooldown = 2400;
        }

        int gameTime = (int) serverPlayer.level().getGameTime();
        UUID uuid = serverPlayer.getUUID();

        int last = lastFeedTick.getOrDefault(uuid, -cooldown);

        if (gameTime - last < cooldown) {
            return; // still on cooldown
        }

        // Try to eat from hotbar
        for (int i = 0; i < 9; i++) { // hotbar slots
            ItemStack stack = serverPlayer.getInventory().getItem(i);
            FoodProperties food = stack.getItem().getFoodProperties(stack, serverPlayer);
            if (food == null) continue; // not edible

            int foodRestore = food.nutrition();
            float saturation = food.saturation() * 2.0F;

            foodData.eat(foodRestore, saturation + (0.5f * level));
            stack.shrink(1);

            // Eating animation + sounds
            serverPlayer.level().levelEvent(2003, serverPlayer.blockPosition(), 0);
            serverPlayer.level().playSound(
                    null,
                    serverPlayer.getX(),
                    serverPlayer.getY(),
                    serverPlayer.getZ(),
                    SoundEvents.GENERIC_EAT,
                    serverPlayer.getSoundSource(),
                    1.0F,
                    1.0F
            );
            serverPlayer.level().playSound(
                    null,
                    serverPlayer.getX(),
                    serverPlayer.getY(),
                    serverPlayer.getZ(),
                    SoundEvents.PLAYER_BURP,
                    serverPlayer.getSoundSource(),
                    0.8F,
                    1.0F
            );

            lastFeedTick.put(uuid, gameTime); // update cooldown
            break;
        }
    }
}