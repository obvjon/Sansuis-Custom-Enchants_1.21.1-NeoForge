package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.Optional;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class MagnetismHandler {

    private static final ResourceKey<Enchantment> MAGNETISM_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "magnetism"));

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        Optional<? extends Holder.Reference<Enchantment>> maybeHolder =
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(MAGNETISM_KEY);
        if (maybeHolder.isEmpty()) return;
        Holder<Enchantment> holder = maybeHolder.get();

        int pieceCount = 0;

        // Count armor pieces with Magnetism
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

        // Scale by number of enchanted armor pieces
        double radius = 4.0D + pieceCount * 2.0D;   // 6, 8, 10, 12 blocks
        double speed  = 0.10D + pieceCount * 0.05D; // 0.15 → 0.30

        AABB box = new AABB(
                player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius
        );

        // Pull items
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!item.isAlive() || item.hasPickUpDelay()) continue;

            Vec3 motion = player.position().subtract(item.position());
            if (motion.lengthSqr() < 0.01) continue;

            Vec3 pull = motion.normalize().scale(speed);
            item.setDeltaMovement(item.getDeltaMovement().add(pull));
        }

        // Pull XP orbs
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
            if (!orb.isAlive()) continue;

            Vec3 motion = player.position().subtract(orb.position());
            if (motion.lengthSqr() < 0.01) continue;

            Vec3 pull = motion.normalize().scale(speed);
            orb.setDeltaMovement(orb.getDeltaMovement().add(pull));
        }
    }
}
