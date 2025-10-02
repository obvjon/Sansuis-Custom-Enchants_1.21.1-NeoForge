package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.Optional;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class PhotosynthesisHandler {

    private static final ResourceKey<Enchantment> PHOTOSYNTHESIS_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "photosynthesis"));

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel world)) return;

        // Count how many armor pieces have Photosynthesis
        int pieceCount = 0;
        Optional<? extends Holder.Reference<Enchantment>> maybeHolder =
                world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(PHOTOSYNTHESIS_KEY);
        if (maybeHolder.isEmpty()) return;
        Holder<Enchantment> holder = maybeHolder.get();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            int lvl = EnchantmentHelper.getTagEnchantmentLevel(holder, stack);
            if (lvl > 0) {
                pieceCount++;
            }
        }

        if (pieceCount <= 0) return;

        // Conditions: daytime, sky visible, not raining (outdoors)
        if (!world.isDay()) return;
        if (world.isRainingAt(player.blockPosition())) return;
        if (!world.canSeeSky(player.blockPosition().above())) return;

        // Restore hunger every 400 ticks (~20s), scales with armor pieces
        if (world.getGameTime() % 400 != 0) return;

        if (player.getFoodData().needsFood()) {
            int foodPoints = pieceCount;                // +1 hunger per piece
            float saturation = 0.25f * pieceCount;      // +0.25 saturation per piece
            player.getFoodData().eat(foodPoints, saturation);

            // Subtle particle effect (happy villager green sparkles)
            double x = player.getX();
            double y = player.getY() + 1.5; // around head/torso
            double z = player.getZ();
            for (int i = 0; i < pieceCount; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
                double offsetY = world.random.nextDouble() * 0.3;
                double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        x + offsetX, y + offsetY, z + offsetZ,
                        1, 0, 0, 0, 0);
            }
        }
    }
}
