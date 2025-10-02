package net.sansuish.sansenchants;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.sansuish.sansenchants.event.enchants.TimberHandler;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SansEnchants.MOD_ID)
public class SansEnchants {
    public static final String MOD_ID = "sansenchants";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SansEnchants(IEventBus modEventBus, ModContainer modContainer) {
        // Register creative tabs
        ModCreativeModeTabs.register(modEventBus);

        // Register lifecycle listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        NeoForge.EVENT_BUS.register(new TimberHandler());

        // Register ourselves to global event bus for @SubscribeEvent
        NeoForge.EVENT_BUS.register(this);

        // Config
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Runs during common setup
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Add items to creative tabs
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Called when the server starts
    }

}
