package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.*;

public class TimberHandler {

    private static final ResourceLocation TIMBER_ID =
            ResourceLocation.fromNamespaceAndPath("sansenchants", "timber");

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        Player player = event.getPlayer();
        if (player == null) return;

        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof AxeItem)) return; // only axes

        var enchantHolder = event.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolder(TIMBER_ID)
                .orElse(null);
        if (enchantHolder == null) return;

        int level = EnchantmentHelper.getItemEnchantmentLevel(enchantHolder, tool);
        if (level <= 0) return;

        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return; // only trigger on logs

        BlockPos origin = event.getPos();

        // ✅ BASE CHECK: only activate Timber if block under is "soil-like"
        BlockState below = event.getLevel().getBlockState(origin.below());
        if (!isSoilBlock(below)) {
            return; // just break one block normally
        }

        int maxLogs = switch (level) {
            case 1 -> 16;
            case 2 -> 64;
            case 3 -> 256;
            default -> 32;
        };

        chopTree((ServerLevel) event.getLevel(), origin, player, maxLogs);
    }

    private boolean isSoilBlock(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.DIRT) ||
                block == Blocks.GRASS_BLOCK ||
                block == Blocks.PODZOL ||
                block == Blocks.MYCELIUM ||
                block == Blocks.ROOTED_DIRT ||
                block == Blocks.MOSS_BLOCK;
    }

    private void chopTree(ServerLevel world, BlockPos origin, Player player, int maxLogs) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(origin);
        int logsBroken = 0;

        while (!queue.isEmpty() && logsBroken < maxLogs) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;

            BlockState state = world.getBlockState(pos);
            if (!state.is(BlockTags.LOGS)) continue;

            // break log
            world.destroyBlock(pos, true, player);
            logsBroken++;

            // check neighbors for more logs
            for (int dy = 1; dy >= -1; dy--) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = pos.offset(dx, dy, dz);
                        BlockState neighborState = world.getBlockState(neighbor);

                        if (neighborState.is(BlockTags.LOGS) && !visited.contains(neighbor)) {
                            queue.add(neighbor);
                        }
                        // break attached leaves but don’t recurse through them
                        else if (neighborState.is(BlockTags.LEAVES)) {
                            world.destroyBlock(neighbor, true, player);
                        }
                    }
                }
            }
        }
    }
}
