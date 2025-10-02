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

        // Page keys (alphabetical list of all enchants)
        List<String> pageKeys = List.of(
                "adrenaline",
                "astronaut",
                "beast_mastery",
                "disarm",
                "earthen_grip",
                "echo_guard",
                "echo_strike",
                "homing",
                "magnetism",
                "smelter",
                "photosynthesis",
                "stability",
                "timber",
                "warp",
                "twerker",
                "wade"
        );

        // Build page index, accounting for 6 index pages
        Map<String, Integer> idx = new HashMap<>();
        int basePage = 6; // index occupies 6 pages
        for (int i = 0; i < pageKeys.size(); i++) {
            idx.put(pageKeys.get(i), i + basePage);
        }

        List<Filterable<Component>> pages = new ArrayList<>();

        // Index pages (2 filled, 4 blank reserved)
        pages.add(Filterable.passThrough(buildIndexPage(1, 6, List.of(
                "adrenaline", "astronaut", "beast_mastery", "disarm",
                "earthen_grip", "echo_guard", "echo_strike", "homing"
        ), idx)));
        pages.add(Filterable.passThrough(buildIndexPage(2, 6, List.of(
                "magnetism", "smelter", "photosynthesis", "stability",
                "timber", "warp", "twerker", "wade"
        ), idx)));
        pages.add(Filterable.passThrough(buildEmptyIndexPage(3, 6)));
        pages.add(Filterable.passThrough(buildEmptyIndexPage(4, 6)));
        pages.add(Filterable.passThrough(buildEmptyIndexPage(5, 6)));
        pages.add(Filterable.passThrough(buildEmptyIndexPage(6, 6)));

        // Enchant pages (start at page 7)
        pages.add(Filterable.passThrough(buildAdrenaline()));
        pages.add(Filterable.passThrough(buildAstronaut()));
        pages.add(Filterable.passThrough(buildBeastMastery()));
        pages.add(Filterable.passThrough(buildDisarm()));
        pages.add(Filterable.passThrough(buildEarthenGrip()));
        pages.add(Filterable.passThrough(buildEchoGuard()));
        pages.add(Filterable.passThrough(buildEchoStrike()));
        pages.add(Filterable.passThrough(buildHoming()));
        pages.add(Filterable.passThrough(buildMagnetism()));
        pages.add(Filterable.passThrough(buildSmelter()));
        pages.add(Filterable.passThrough(buildPhotosynthesis()));
        pages.add(Filterable.passThrough(buildStability()));
        pages.add(Filterable.passThrough(buildTimber()));
        pages.add(Filterable.passThrough(buildWarp()));
        pages.add(Filterable.passThrough(buildTwerker()));
        pages.add(Filterable.passThrough(buildWade()));

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

    private static Component backIndex() {
        return Component.literal("\n\n§8[Back to Index]").withStyle(s ->
                s.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, "1"))
        );
    }

    private static Component buildIndexPage(int num, int total, List<String> keys, Map<String, Integer> idx) {
        Component page = Component.literal("§l§nEnchants Guide\n\n§7Index (Page " + num + "/" + total + ")\n\n");

        for (String key : keys) {
            Component entry = switch (key) {
                case "adrenaline" -> linkTo("§c➤ Adrenaline\n", idx, key);
                case "astronaut" -> linkTo("§9➤ Astronaut\n", idx, key);
                case "beast_mastery" -> linkTo("§2➤ Beast Mastery\n", idx, key);
                case "disarm" -> linkTo("§4➤ Disarm\n", idx, key);
                case "earthen_grip" -> linkTo("§8➤ Earthen Grip\n", idx, key);
                case "echo_guard" -> linkTo("§5➤ Echo Guard\n", idx, key);
                case "echo_strike" -> linkTo("§3➤ Echo Strike\n", idx, key);
                case "homing" -> linkTo("§2➤ Homing\n", idx, key);
                case "magnetism" -> linkTo("§6➤ Magnetism\n", idx, key);
                case "smelter" -> linkTo("§6➤ Smelter\n", idx, key);
                case "photosynthesis" -> linkTo("§a➤ Photosynthesis\n", idx, key);
                case "stability" -> linkTo("§e➤ Stability\n", idx, key);
                case "timber" -> linkTo("§2➤ Timber\n", idx, key);
                case "warp" -> linkTo("§5➤ Warp\n", idx, key);
                case "twerker" -> linkTo("§3➤ Twerker\n", idx, key);
                case "wade" -> linkTo("§1➤ Wade\n", idx, key);
                default -> Component.literal("");
            };
            page = Component.empty().append(page).append(entry);
        }

        return page;
    }

    private static Component buildEmptyIndexPage(int num, int total) {
        return Component.literal("§l§nEnchants Guide\n\n§7Index (Page " + num + "/" + total + ")\n\n§8[Reserved for future enchants]");
    }

    // ---------- enchant pages ----------

    private static Component buildAdrenaline() {
        return Component.literal(
                "§l§cAdrenaline:\n\n" +
                        "§7Triggers at low HP (<25%).\n" +
                        "§7Grants Strength + Speed.\n\n" +
                        "§7Lv1: Speed I, Str I\n" +
                        "§7Lv2: Speed II, Str II\n" +
                        "§7Lv3: Speed III, Str III\n\n" +
                        "§6Tip: Clutch survival."
        ).append(backIndex());
    }

    private static Component buildAstronaut() {
        return Component.literal(
                "§l§9Astronaut:\n\n" +
                        "§7Low-gravity jump + fall.\n" +
                        "§7Lv1: Small boost\n" +
                        "§7Lv2: High jump\n" +
                        "§7Lv3: Near slow-fall\n\n" +
                        "§6Tip: Elytra synergy."
        ).append(backIndex());
    }

    private static Component buildBeastMastery() {
        return Component.literal(
                "§l§2Beast Mastery:\n\n" +
                        "§7Buffs your tamed pets.\n" +
                        "§7Lv1: +10% dmg, +5% hp\n" +
                        "§7Lv2: +20% dmg, +10% hp\n" +
                        "§7Lv3: +30% dmg, +15% hp\n\n" +
                        "§6Tip: Stronger with packs."
        ).append(backIndex());
    }

    private static Component buildDisarm() {
        return Component.literal(
                "§l§4Disarm:\n\n" +
                        "§7Chance to knock items.\n" +
                        "§7Lv1: 20%\n" +
                        "§7Lv2: 40%\n" +
                        "§7Lv3: 60%\n\n" +
                        "§6Tip: Punish shields."
        ).append(backIndex());
    }

    private static Component buildEarthenGrip() {
        return Component.literal(
                "§l§8Earthen Grip:\n\n" +
                        "§7Resistance when grounded.\n" +
                        "§7Adds Slowness I.\n" +
                        "§7Lv1: Res I\n" +
                        "§7Lv2: Res II\n" +
                        "§7Lv3: Res III\n\n" +
                        "§6Tip: Tank tradeoff."
        ).append(backIndex());
    }

    private static Component buildEchoGuard() {
        return Component.literal(
                "§l§5Echo Guard:\n\n" +
                        "§7Shield parry enchant.\n" +
                        "§7Reflects dmg + KB.\n\n" +
                        "§6Tip: Perfect timing."
        ).append(backIndex());
    }

    private static Component buildEchoStrike() {
        return Component.literal(
                "§l§3Echo Strike:\n\n" +
                        "§7Chance to deal echo hit.\n" +
                        "§7Scales with level.\n\n" +
                        "§6Tip: Burst DPS."
        ).append(backIndex());
    }

    private static Component buildHoming() {
        return Component.literal(
                "§l§2Homing:\n\n" +
                        "§7Arrows curve to targets.\n" +
                        "§7Better lock per level.\n\n" +
                        "§6Tip: Best with bows."
        ).append(backIndex());
    }

    private static Component buildMagnetism() {
        return Component.literal(
                "§l§6Magnetism:\n\n" +
                        "§7Pulls in items + XP.\n" +
                        "§7Scales by level.\n\n" +
                        "§6Tip: Farm helper."
        ).append(backIndex());
    }

    private static Component buildSmelter() {
        return Component.literal(
                "§l§6Smelter:\n\n" +
                        "§7Auto-smelts mined ores.\n" +
                        "§7Scales with level.\n\n" +
                        "§6Tip: Saves furnace use."
        ).append(backIndex());
    }

    private static Component buildPhotosynthesis() {
        return Component.literal(
                "§l§aPhotosynthesis:\n\n" +
                        "§7Heals hunger in sunlight.\n" +
                        "§7High lvls add vision.\n\n" +
                        "§6Tip: Outdoor sustain."
        ).append(backIndex());
    }

    private static Component buildStability() {
        return Component.literal(
                "§l§eStability:\n\n" +
                        "§7Boots prevent accidents.\n" +
                        "§7Lv1: No crop trampling\n" +
                        "§7Lv2: No snow sinking\n" +
                        "§7Lv3: Adds Res I\n\n" +
                        "§6Tip: For builders."
        ).append(backIndex());
    }

    private static Component buildTimber() {
        return Component.literal(
                "§l§2Timber:\n\n" +
                        "§7Chop one log → tree falls.\n" +
                        "§7Scales with level.\n\n" +
                        "§6Tip: Wood farms."
        ).append(backIndex());
    }

    private static Component buildWarp() {
        return Component.literal(
                "§l§5Warp:\n\n" +
                        "§7Throw trident to teleport.\n" +
                        "§7Cooldown scales.\n\n" +
                        "§6Tip: Escape tool."
        ).append(backIndex());
    }

    private static Component buildTwerker() {
        return Component.literal(
                "§l§3Twerker:\n\n" +
                        "§7Sneak to grow crops.\n" +
                        "§7High lvls auto-replant.\n\n" +
                        "§6Tip: Farm utility."
        ).append(backIndex());
    }

    private static Component buildWade() {
        return Component.literal(
                "§l§1Wade:\n\n" +
                        "§7Walk faster on water.\n" +
                        "§7Scales with level.\n\n" +
                        "§6Tip: Use with Strider."
        ).append(backIndex());
    }
}
