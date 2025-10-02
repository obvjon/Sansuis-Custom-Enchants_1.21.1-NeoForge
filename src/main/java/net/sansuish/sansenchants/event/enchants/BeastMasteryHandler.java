package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class BeastMasteryHandler {

    private static final ResourceKey<Enchantment> BEAST_MASTERY_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "beast_mastery"));

    // Track applied modifiers so we can safely remove/reapply them
    private static final Map<UUID, AttributeModifier> DAMAGE_MODS = new HashMap<>();
    private static final Map<UUID, AttributeModifier> HEALTH_MODS = new HashMap<>();
    private static final Map<UUID, AttributeModifier> SPEED_MODS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(BEAST_MASTERY_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        // Count total enchantment level across all armor
        int totalLevel = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            totalLevel += EnchantmentHelper.getTagEnchantmentLevel(holder, armor);
        }
        if (totalLevel <= 0) return;

        // Find all nearby tamed animals owned by this player (16-block radius)
        List<TamableAnimal> pets = player.level().getEntitiesOfClass(
                TamableAnimal.class,
                player.getBoundingBox().inflate(16),
                pet -> pet.isOwnedBy(player)
        );

        // Apply buffs to each pet
        for (TamableAnimal pet : pets) {
            applyOrUpdateBuff(pet, totalLevel);
        }
    }

    private static void applyOrUpdateBuff(TamableAnimal pet, int level) {
        double damageBoost = 0.10 * level; // +10% damage per level
        double healthBoost = 0.05 * level; // +5% health per level
        double speedBoost  = 0.10 * level; // +10% speed per level (noticeable but not broken)

        UUID petId = pet.getUUID();

        // --- Attack Damage ---
        AttributeInstance attack = pet.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            AttributeModifier prev = DAMAGE_MODS.remove(petId);
            if (prev != null) attack.removeModifier(prev);

            AttributeModifier mod = new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "beast_mastery_damage"),
                    damageBoost,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            attack.addPermanentModifier(mod);
            DAMAGE_MODS.put(petId, mod);
        }

        // --- Max Health ---
        AttributeInstance health = pet.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            AttributeModifier prev = HEALTH_MODS.remove(petId);
            if (prev != null) health.removeModifier(prev);

            AttributeModifier mod = new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "beast_mastery_health"),
                    healthBoost,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            health.addPermanentModifier(mod);
            HEALTH_MODS.put(petId, mod);

            // Heal pet up to new max if boosted
            pet.setHealth(pet.getMaxHealth());
        }

        // --- Movement Speed ---
        AttributeInstance speed = pet.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            AttributeModifier prev = SPEED_MODS.remove(petId);
            if (prev != null) speed.removeModifier(prev);

            AttributeModifier mod = new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "beast_mastery_speed"),
                    speedBoost,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            speed.addPermanentModifier(mod);
            SPEED_MODS.put(petId, mod);
        }
    }
}
