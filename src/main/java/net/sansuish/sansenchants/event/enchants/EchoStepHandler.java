package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.List;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class EchoStepHandler {

    private static final ResourceKey<Enchantment> ECHO_STEP_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "echo_step"));

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        // Check boots for enchant
        ItemStack boots = player.getInventory().getArmor(0);
        if (boots.isEmpty()) return;

        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var holderOpt = registry.getHolder(ECHO_STEP_KEY);
        if (holderOpt.isEmpty()) return;
        Holder<Enchantment> holder = holderOpt.get();

        int enchLevel = EnchantmentHelper.getTagEnchantmentLevel(holder, boots);
        if (enchLevel <= 0) return;

        // Trigger: sprint + airborne, every ~3 seconds
        if (player.isSprinting() && !player.onGround() && player.tickCount % 60 == 0) {
            confuseMobs(level, player, enchLevel);
        }
    }

    private static void confuseMobs(ServerLevel level, Player player, int enchLevel) {
        // Longer confusion time: 120 ticks base (6s), +80 ticks (4s) per level
        int duration = 120 + (80 * (enchLevel - 1));

        List<Mob> mobs = level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(12),
                mob -> mob.getTarget() == player);

        for (Mob mob : mobs) {
            if (level.random.nextFloat() < 0.5f * enchLevel) {
                mob.setTarget(null);

                // Re-assign after delay
                int delay = duration;
                level.getServer().tell(new TickTask(level.getServer().getTickCount() + delay, () -> {
                    if (mob.isAlive()) {
                        mob.setTarget(player);
                    }
                }));
            }
        }

        // Smoke puff effect
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                player.getX(), player.getY(), player.getZ(),
                15, 0.5, 1.0, 0.5, 0.01
        );
    }
}
