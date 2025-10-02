package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.sansuish.sansenchants.SansEnchants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class MinerSmelterHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // only run server-side
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getPlayer();
        if (player == null) return;

        // resolve the enchantment holder
        var registry = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

        var holderOpt = registry.getHolder(ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "miner_smelter"));
        if (holderOpt.isEmpty()) return;
        var holder = holderOpt.get();

        // check tool has the enchantment
        ItemStack tool = player.getMainHandItem();
        if (EnchantmentHelper.getItemEnchantmentLevel(holder, tool) <= 0) return;

        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // compute the normal drops (this does NOT actually spawn them)
        List<ItemStack> originalDrops = Block.getDrops(state, level, pos, blockEntity, player, tool);
        if (originalDrops.isEmpty()) return;

        RecipeManager recipeManager = level.getRecipeManager();

        // build the final drop list, replacing smeltable drops
        List<ItemStack> finalDrops = new ArrayList<>(originalDrops.size());
        float totalXp = 0f;
        boolean replacedAny = false;

        for (ItemStack drop : originalDrops) {
            Optional<RecipeHolder<SmeltingRecipe>> recipeOpt =
                    recipeManager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(drop), level);

            if (recipeOpt.isPresent()) {
                // get smelt result
                ItemStack smelted = recipeOpt.get().value().getResultItem(level.registryAccess());
                if (!smelted.isEmpty()) {
                    ItemStack out = smelted.copy();
                    out.setCount(drop.getCount()); // keep fortune counts etc.
                    finalDrops.add(out);

                    // accumulate XP like a furnace would
                    float recipeXp = recipeOpt.get().value().getExperience();
                    totalXp += recipeXp * drop.getCount();

                    replacedAny = true;
                    continue;
                }
            }

            // not smeltable -> keep original
            finalDrops.add(drop.copy());
        }

        // if nothing changed, do nothing and let vanilla handle it
        if (!replacedAny) return;

        // cancel the default handling (prevents vanilla from also dropping default items)
        event.setCanceled(true);

        // remove block server-side
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        // spawn the computed drops
        for (ItemStack toDrop : finalDrops) {
            if (!toDrop.isEmpty()) {
                Block.popResource(level, pos, toDrop.copy());
            }
        }

        // spawn XP orbs (use furnace-like rounding)
        int xpToSpawn = (int) totalXp;
        float frac = totalXp - xpToSpawn;
        if (level.getRandom().nextFloat() < frac) xpToSpawn++;
        if (xpToSpawn > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), xpToSpawn);
        }

        // damage tool + trigger vanilla behavior (Unbreaking, Mending, stats, advancements)
        tool.mineBlock(level, state, pos, player);
    }
}
