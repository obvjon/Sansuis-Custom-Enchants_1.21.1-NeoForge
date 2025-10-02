package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.Random;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class DisarmHandler {

    private static final ResourceKey<Enchantment> DISARM_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "disarm"));

    private static final Random RAND = new Random();

    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (!(attacker.level() instanceof ServerLevel level)) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(DISARM_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) return;

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, weapon);
        if (enchLevel <= 0) return;

        LivingEntity victim = event.getEntity();

        // 🔥 New: much higher chance
        // 20% per level = up to 60% at level 3
        float chance = 0.20f * enchLevel;
        if (RAND.nextFloat() > chance) return;

        // Try to disarm mainhand first
        ItemStack targetWeapon = victim.getMainHandItem();
        if (!targetWeapon.isEmpty()) {
            dropItem(level, victim, targetWeapon, EquipmentSlot.MAINHAND);
            return;
        }

        // If no mainhand, try offhand (shield/secondary)
        ItemStack offhand = victim.getOffhandItem();
        if (!offhand.isEmpty()) {
            dropItem(level, victim, offhand, EquipmentSlot.OFFHAND);
        }
    }

    private static void dropItem(ServerLevel level, LivingEntity victim, ItemStack stack, EquipmentSlot slot) {
        ItemStack drop = stack.copy();
        victim.setItemSlot(slot, ItemStack.EMPTY);

        ItemEntity itemEntity = new ItemEntity(
                level,
                victim.getX(),
                victim.getY() + 0.5,
                victim.getZ(),
                drop
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);

        level.playSound(null, victim.blockPosition(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS,
                0.8F, 1.2F);
    }
}
