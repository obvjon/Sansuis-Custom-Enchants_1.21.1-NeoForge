package net.sansuish.sansenchants.event;

import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.player.Player;
import net.sansuish.sansenchants.SansEnchants;

import java.util.*;

@EventBusSubscriber(modid = SansEnchants.MOD_ID)
public class GuideBookEvent {

    @SubscribeEvent
    public static void giveGuideOnFirstJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // Only give once
        boolean hasBook = player.getInventory().items.stream().anyMatch(stack -> {
            if (!stack.is(Items.WRITTEN_BOOK)) return false;
            WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            return content != null && "Enchants Guide".equals(content.title().raw());
        });
        if (hasBook) return;

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        // Page keys
        List<String> pageKeys = List.of(
                "index",
                "echo_strike",
                "timber",
                "echo_guard",
                "homing",
                "warp_trident",
                "astronaut",
                "magnetism",
                "photosynthesis",
                "miners_smelter",
                "twerker",
                "wade",
                "adrenaline",
                "stability",
                "disarm"
        );

        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < pageKeys.size(); i++) {
            idx.put(pageKeys.get(i), i);
        }

        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(buildIndex(idx)));
        pages.add(Filterable.passThrough(buildEchoStrike(idx)));
        pages.add(Filterable.passThrough(buildTimber(idx)));
        pages.add(Filterable.passThrough(buildEchoGuard(idx)));
        pages.add(Filterable.passThrough(buildHoming(idx)));
        pages.add(Filterable.passThrough(buildWarpTrident(idx)));
        pages.add(Filterable.passThrough(buildAstronaut(idx)));
        pages.add(Filterable.passThrough(buildMagnetism(idx)));
        pages.add(Filterable.passThrough(buildPhotosynthesis(idx)));
        pages.add(Filterable.passThrough(buildMinersSmelter(idx)));
        pages.add(Filterable.passThrough(buildTwerker(idx)));
        pages.add(Filterable.passThrough(buildWade(idx)));
        pages.add(Filterable.passThrough(buildAdrenaline(idx)));
        pages.add(Filterable.passThrough(buildStability(idx)));
        pages.add(Filterable.passThrough(buildDisarm(idx)));

        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Enchants Guide"),
                "Sansuish",
                0,
                pages,
                false
        ));

        player.addItem(book);
    }

    // ---------- helpers ----------

    private static Component linkTo(String text, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        String pageIndex = String.valueOf(i != null ? (i + 1) : 1);
        return Component.literal(text).withStyle(s ->
                s.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, pageIndex))
        );
    }

    private static Component backIndex(Map<String, Integer> idx) {
        return linkTo("\n\n§8[Back to Index]", idx, "index");
    }

    // ---------- index ----------

    private static Component buildIndex(Map<String, Integer> idx) {
        return Component.literal("§l§nEnchants Guide\n\n")
                .append(linkTo("§3➤ Echo Strike\n", idx, "echo_strike"))
                .append(linkTo("§2➤ Timber\n", idx, "timber"))
                .append(linkTo("§5➤ Echo Guard\n", idx, "echo_guard"))
                .append(linkTo("§2➤ Homing\n", idx, "homing"))
                .append(linkTo("§5➤ Warp Trident\n", idx, "warp_trident"))
                .append(linkTo("§9➤ Astronaut\n", idx, "astronaut"))
                .append(linkTo("§6➤ Magnetism\n", idx, "magnetism"))
                .append(linkTo("§a➤ Photosynthesis\n", idx, "photosynthesis"))
                .append(linkTo("§6➤ Miner’s Smelter\n", idx, "miners_smelter"))
                .append(linkTo("§3➤ Twerker\n", idx, "twerker"))
                .append(linkTo("§1➤ Wade\n", idx, "wade"))
                .append(linkTo("§c➤ Adrenaline\n", idx, "adrenaline"))
                .append(linkTo("§e➤ Stability\n", idx, "stability"))
                .append(linkTo("§4➤ Disarm\n", idx, "disarm"));
    }

    // ---------- enchant pages ----------

    private static Component buildEchoStrike(Map<String, Integer> idx) {
        return Component.literal(
                "§l§3Echo Strike:\n\n" +
                        "§7Chance to deal a delayed second hit\n" +
                        "§7on attack, echoing your strike.\n" +
                        "§7Level increases trigger chance.\n"
        ).append(backIndex(idx));
    }

    private static Component buildTimber(Map<String, Integer> idx) {
        return Component.literal(
                "§l§2Timber:\n\n" +
                        "§7Chop a log and fell the entire tree.\n" +
                        "§7Works with axes. Higher levels\n" +
                        "§7may improve efficiency.\n"
        ).append(backIndex(idx));
    }

    private static Component buildEchoGuard(Map<String, Integer> idx) {
        return Component.literal(
                "§l§5Echo Guard:\n\n" +
                        "§7Time shield blocks just right to parry.\n" +
                        "§7Retaliates with damage + knockback,\n" +
                        "§7with particles and sounds.\n"
        ).append(backIndex(idx));
    }

    private static Component buildHoming(Map<String, Integer> idx) {
        return Component.literal(
                "§l§2Homing:\n\n" +
                        "§7Arrows curve toward nearby targets.\n" +
                        "§7- Narrow vision cone\n" +
                        "§7- Locks onto closest mob\n" +
                        "§7- Steering force scales by level\n"
        ).append(backIndex(idx));
    }

    private static Component buildWarpTrident(Map<String, Integer> idx) {
        return Component.literal(
                "§l§5Warp Trident:\n\n" +
                        "§7Throwing teleports you to the trident\n" +
                        "§7on impact. Applies exhaustion based\n" +
                        "§7on distance and has a cooldown.\n"
        ).append(backIndex(idx));
    }

    private static Component buildAstronaut(Map<String, Integer> idx) {
        return Component.literal(
                "§l§9Astronaut:\n\n" +
                        "§7Simulates low gravity.\n" +
                        "§7- Jump boost (scaled)\n" +
                        "§7- Slow Falling at higher lvls\n"
        ).append(backIndex(idx));
    }

    private static Component buildMagnetism(Map<String, Integer> idx) {
        return Component.literal(
                "§l§6Magnetism:\n\n" +
                        "§7Pulls items and XP orbs toward you.\n" +
                        "§7Stronger with more enchanted armor.\n"
        ).append(backIndex(idx));
    }

    private static Component buildPhotosynthesis(Map<String, Integer> idx) {
        return Component.literal(
                "§l§aPhotosynthesis:\n\n" +
                        "§7Day: Slowly restores hunger.\n" +
                        "§7Lvl 3 stores solar energy.\n\n" +
                        "§7Night: Use stored charge for\n" +
                        "§7Night Vision + Glowing aura.\n"
        ).append(backIndex(idx));
    }

    private static Component buildMinersSmelter(Map<String, Integer> idx) {
        return Component.literal(
                "§l§6Miner’s Smelter:\n\n" +
                        "§7Ores drop smelted ingots + XP.\n" +
                        "§7Lv2: Smelts some foods.\n" +
                        "§7Lv3: Smelts any block + special\n" +
                        "§7lava-touch effects.\n" +
                        "§8Drawback: Hunger + tool wear.\n"
        ).append(backIndex(idx));
    }

    private static Component buildTwerker(Map<String, Integer> idx) {
        return Component.literal(
                "§l§3Twerker:\n\n" +
                        "§7Sneak near crops to grow them.\n" +
                        "§7Lv2: Auto-harvest and replant.\n" +
                        "§7Costs small exhaustion.\n"
        ).append(backIndex(idx));
    }

    private static Component buildWade(Map<String, Integer> idx) {
        return Component.literal(
                "§l§1Wade:\n\n" +
                        "§7Boots-only enchantment.\n" +
                        "§7Lets you glide across water,\n" +
                        "§7with speed scaling by level.\n"
        ).append(backIndex(idx));
    }

    private static Component buildAdrenaline(Map<String, Integer> idx) {
        return Component.literal(
                "§l§cAdrenaline:\n\n" +
                        "§7Armor enchantment that empowers\n" +
                        "§7you when at low health (<25%).\n" +
                        "§7Lv1: Speed I + Strength I\n" +
                        "§7Lv2: Speed II + Strength II\n" +
                        "§7Lv3: Speed III + Strength III\n"
        ).append(backIndex(idx));
    }

    private static Component buildStability(Map<String, Integer> idx) {
        return Component.literal(
                "§l§eStability:\n\n" +
                        "§7Boots-only enchantment of sure footing.\n" +
                        "§7Lv1: Prevents farmland trampling\n" +
                        "§7Lv2: Walk on powder snow safely\n" +
                        "§7Lv3: Grants Resistance I (less knockback)\n"
        ).append(backIndex(idx));
    }

    private static Component buildDisarm(Map<String, Integer> idx) {
        return Component.literal(
                "§l§4Disarm:\n\n" +
                        "§7Weapon enchantment with a chance to\n" +
                        "§7knock items from enemy hands.\n" +
                        "§7Lv1: 20% chance\n" +
                        "§7Lv2: 40% chance\n" +
                        "§7Lv3: 60% chance\n" +
                        "§7Affects mainhand first, then offhand.\n"
        ).append(backIndex(idx));
    }
}
