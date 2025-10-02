package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.Optional;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class TwerkerHandler {

    private static final ResourceKey<Enchantment> TWERKER_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "twerker"));

    private static final Map<Player, Boolean> CROUCH_STATE = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel world)) return;

        boolean crouching = player.isCrouching();
        boolean wasCrouching = CROUCH_STATE.getOrDefault(player, false);
        CROUCH_STATE.put(player, crouching);

        if (!crouching || wasCrouching) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        Optional<? extends Holder.Reference<Enchantment>> maybeHolder =
                world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(TWERKER_KEY);
        if (maybeHolder.isEmpty()) return;

        Holder<Enchantment> holder = maybeHolder.get();
        int level = EnchantmentHelper.getTagEnchantmentLevel(holder, stack);
        if (level <= 0) return;

        // Exhaustion cost per crouch
        player.causeFoodExhaustion(0.25f * level);

        // Loop over a tighter area (radius 1 in X/Z = 3x3 total)
        BlockPos center = player.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy >= -1; dy--) { // block at feet and block below
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);

                    if (state.getBlock() instanceof CropBlock crop) {
                        int age = crop.getAge(state);
                        if (age < crop.getMaxAge()) {
                            // grow 1 stage
                            BlockState newState = crop.getStateForAge(age + 1);
                            world.setBlock(pos, newState, 2);
                            world.levelEvent(2005, pos, 0);
                        } else if (level >= 2) {
                            // auto-harvest + replant
                            crop.playerDestroy(world, player, pos, state,
                                    world.getBlockEntity(pos), ItemStack.EMPTY);
                            world.setBlock(pos, crop.getStateForAge(0), 2);
                        }
                    }
                }
            }
        }
    }
}
