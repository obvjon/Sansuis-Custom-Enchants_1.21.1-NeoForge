package net.sansuish.sansenchants.event.enchants;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Set;

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

        // Find our enchantment
        var enchantHolder = event.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolder(TIMBER_ID)
                .orElse(null);

        if (enchantHolder == null) return;

        int level = EnchantmentHelper.getItemEnchantmentLevel(enchantHolder, tool);
        if (level <= 0) return;

        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return; // only trigger on chopping logs

        int maxLogs = switch (level) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 100;
            default -> 10;
        };

        ServerLevel world = (ServerLevel) event.getLevel();
        BlockPos origin = event.getPos();

        Set<BlockPos> visited = new HashSet<>();
        fellTree(world, origin, player, visited, maxLogs);
    }

    private void fellTree(ServerLevel world, BlockPos pos, Player player,
                          Set<BlockPos> visited, int maxLogs) {
        if (visited.size() >= maxLogs) return;
        if (visited.contains(pos)) return;

        BlockState state = world.getBlockState(pos);
        if (!(state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES))) return;

        visited.add(pos);

        // Break block naturally
        world.destroyBlock(pos, true, player);

        // Search all neighbors in a 3x3x3 cube
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue; // skip self
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    fellTree(world, neighbor, player, visited, maxLogs);
                }
            }
        }
    }}