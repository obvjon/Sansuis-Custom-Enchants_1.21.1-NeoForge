package net.sansuish.sansenchants;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SansEnchants.MOD_ID);

    // === Items Tab ===
    public static final Supplier<CreativeModeTab> VANPLUS_BOOKS_TAB = CREATIVE_MODE_TAB.register("sansenchants_books_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
                    .title(Component.translatable("creativetab.sansenchants.sansenchants_books"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // Lookup the enchantments registry
                        HolderLookup.RegistryLookup<Enchantment> lookup =
                                itemDisplayParameters.holders().lookupOrThrow(Registries.ENCHANTMENT);

                        // Add all enchantments from your mod as books (all levels)
                        lookup.listElements().forEach(holder -> {
                            if (holder.key().location().getNamespace().equals(SansEnchants.MOD_ID)) {
                                int maxLevel = holder.value().getMaxLevel();
                                for (int lvl = 1; lvl <= maxLevel; lvl++) {
                                    ItemStack book = EnchantedBookItem.createForEnchantment(
                                            new EnchantmentInstance(holder, lvl));
                                    output.accept(book);
                                }
                            }
                        });
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
