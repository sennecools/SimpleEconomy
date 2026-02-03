package com.simpleeconomy;

import com.simpleeconomy.command.EconomyCommands;
import com.simpleeconomy.command.ShopCommands;
import com.simpleeconomy.network.NetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SimpleEconomy.MOD_ID)
public class SimpleEconomy {
    public static final String MOD_ID = "simpleeconomy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SimpleEconomy(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("SimpleEconomy initializing...");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            LOGGER.info("SimpleEconomy network registered");
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("SimpleEconomy loaded on server!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher());
        ShopCommands.register(event.getDispatcher());
        LOGGER.info("SimpleEconomy commands registered");
    }
}
