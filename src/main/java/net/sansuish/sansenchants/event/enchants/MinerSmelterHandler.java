package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.sansuish.sansenchants.SansEnchants;

import java.util.*;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class MinerSmelterHandler {

    private static final Set<Block> ORE_BLOCKS = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE
    );

    private static final Set<Block> FOOD_BLOCKS = Set.of(
            Blocks.POTATOES,
            Blocks.KELP
    );

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getPlayer();
        if (player == null) return;

        var registry = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

        var holderOpt = registry.getHolder(ResourceLocation.fromNamespaceAndPath(SansEnchants.MOD_ID, "miner_smelter"));
        if (holderOpt.isEmpty()) return;
        var holder = holderOpt.get();

        ItemStack tool = player.getMainHandItem();
        int enchLevel = EnchantmentHelper.getItemEnchantmentLevel(holder, tool);
        if (enchLevel <= 0) return;

        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // original drops already apply Fortune and tool modifiers
        List<ItemStack> originalDrops = Block.getDrops(state, level, pos, blockEntity, player, tool);
        if (originalDrops.isEmpty()) return;

        RecipeManager recipeManager = level.getRecipeManager();
        List<ItemStack> finalDrops = new ArrayList<>();
        float totalXp = 0f;
        boolean replacedAny = false;
        int smeltedCount = 0; // track number of items smelted

        for (ItemStack drop : originalDrops) {
            boolean smeltThis = false;

            if (enchLevel >= 1 && ORE_BLOCKS.contains(state.getBlock())) smeltThis = true;
            if (enchLevel >= 2 && FOOD_BLOCKS.contains(state.getBlock())) smeltThis = true;
            if (enchLevel >= 3) smeltThis = true; // allow all valid smelt recipes (and lava-touch handled below)

            if (smeltThis) {
                Optional<RecipeHolder<SmeltingRecipe>> recipeOpt =
                        recipeManager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(drop), level);

                if (recipeOpt.isPresent()) {
                    ItemStack smelted = recipeOpt.get().value().getResultItem(level.registryAccess());
                    if (!smelted.isEmpty()) {
                        // preserve count (includes Fortune)
                        ItemStack out = smelted.copy();
                        out.setCount(drop.getCount());
                        finalDrops.add(out);

                        float recipeXp = recipeOpt.get().value().getExperience();
                        totalXp += recipeXp * drop.getCount();

                        smeltedCount += drop.getCount();
                        replacedAny = true;
                        continue;
                    }
                }
            }

            // fallback: vanilla drop
            finalDrops.add(drop.copy());
        }

        // 🔥 Special L3 “lava-touch” overrides (stone → smooth stone, cobble → stone, sand → glass)
        if (enchLevel >= 3) {
            if (state.is(Blocks.STONE)) {
                finalDrops.clear();
                finalDrops.add(new ItemStack(Blocks.SMOOTH_STONE, 1));
                smeltedCount = 1;
                replacedAny = true;
            } else if (state.is(Blocks.COBBLESTONE)) {
                finalDrops.clear();
                finalDrops.add(new ItemStack(Blocks.STONE, 1));
                smeltedCount = 1;
                replacedAny = true;
            } else if (state.is(Blocks.SAND)) {
                finalDrops.clear();
                finalDrops.add(new ItemStack(Blocks.GLASS, 1));
                smeltedCount = 1;
                replacedAny = true;
            }
        }

        if (!replacedAny) return;

        // Replace block with air and spawn our computed drops + xp
        event.setCanceled(true);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        for (ItemStack toDrop : finalDrops) {
            if (!toDrop.isEmpty()) {
                Block.popResource(level, pos, toDrop.copy());
            }
        }

        int xpToSpawn = (int) totalXp;
        float frac = totalXp - xpToSpawn;
        if (level.getRandom().nextFloat() < frac) xpToSpawn++;
        if (xpToSpawn > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), xpToSpawn);
        }

        // normal durability & mending/unbreaking interactions (this applies vanilla mining damage)
        tool.mineBlock(level, state, pos, player);

        // ⚠️ Overheat drawback — ONLY when we actually smelted/replaced drops
        if (replacedAny && enchLevel >= 1) {
            // Base exhaustion per item
            float perItemExhaustion = switch (enchLevel) {
                case 1 -> 1.0f; // each smelt ~10x sprint tick
                case 2 -> 2.0f; // punishing at mid level
                case 3 -> 3.0f; // very punishing at max
                default -> 1.0f;
            };
            float totalExhaustion = perItemExhaustion * smeltedCount;
            player.causeFoodExhaustion(totalExhaustion);

            // Extra durability damage scales with number of smelted items
            int perItemDamage = switch (enchLevel) {
                case 2 -> 1;
                case 3 -> 2;
                default -> 0;
            };
            int extraDamage = perItemDamage * smeltedCount;

            if (extraDamage > 0) {
                tool.hurtAndBreak(extraDamage, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);

                if (tool.isEmpty()) {
                    level.playSound(null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
    }
}
