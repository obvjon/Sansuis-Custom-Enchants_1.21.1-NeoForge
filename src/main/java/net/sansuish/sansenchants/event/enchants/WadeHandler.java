package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.Optional;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class WadeHandler {

    private static final ResourceKey<Enchantment> WADE_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "wade"));

    private static Holder<Enchantment> WADE_HOLDER = null;

    // simple per-player cooldown for sound
    private static long lastSoundTick = 0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) return;

        if (WADE_HOLDER == null) {
            Optional<Holder.Reference<Enchantment>> maybe =
                    player.level().registryAccess()
                            .registryOrThrow(Registries.ENCHANTMENT)
                            .getHolder(WADE_KEY);
            if (maybe.isEmpty()) return;
            WADE_HOLDER = maybe.get();
        }

        int level = EnchantmentHelper.getTagEnchantmentLevel(WADE_HOLDER, boots);
        if (level <= 0) return;

        BlockPos pos = player.blockPosition().below();

        if (player.level().getFluidState(pos).is(FluidTags.WATER)) {
            Vec3 motion = player.getDeltaMovement();

            if (motion.y < 0.0D) {
                motion = new Vec3(motion.x, 0.0D, motion.z);
                player.fallDistance = 0.0F;
                player.setOnGround(true);
            }

            double speedBoost = 1.0D + (0.1D * level);
            motion = new Vec3(motion.x * speedBoost, motion.y, motion.z * speedBoost);
            player.setDeltaMovement(motion);

            // play subtle splash/swim sound every ~10 ticks
            long gameTime = player.level().getGameTime();
            if (gameTime - lastSoundTick > 10) {
                RandomSource rand = player.getRandom();
                player.level().playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.PLAYER_SWIM,
                        SoundSource.PLAYERS,
                        0.02F, // 🔊 much quieter volume
                        0.9F + rand.nextFloat() * 0.2F
                );
                lastSoundTick = gameTime;
            }
        }
    }
}
