package com.simpleeconomy;

import com.simpleeconomy.command.CoinflipCommand;
import com.simpleeconomy.command.DailyRewardCommand;
import com.simpleeconomy.command.EconomyCommands;
import com.simpleeconomy.command.ShopCommands;
import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.PlayerDataSavedData;
import com.simpleeconomy.data.TransactionLog;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.util.SoundHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SimpleEconomy.MOD_ID)
public class SimpleEconomy {
    public static final String MOD_ID = "simpleeconomy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SimpleEconomy(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("SimpleEconomy initializing...");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ModConfig.load();
        LOGGER.info("SimpleEconomy loaded on server!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher());
        ShopCommands.register(event.getDispatcher());
        CoinflipCommand.register(event.getDispatcher());
        DailyRewardCommand.register(event.getDispatcher());
        LOGGER.info("SimpleEconomy commands registered");
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataSavedData playerData = PlayerDataSavedData.get(player.getServer());

            // Feature 4: Starting balance for new players
            if (!playerData.hasReceivedStartingBalance(player.getUUID())) {
                double startBal = ModConfig.getStartingBalance();
                EconomyManager.addBalance(player, startBal);
                playerData.markStartingBalanceReceived(player.getUUID());

                // Log the transaction
                TransactionLog log = TransactionLog.get(player.getServer());
                log.addTransaction(player.getUUID(), TransactionLog.Transaction.startingBalance(startBal));

                // Notify the player
                player.sendSystemMessage(Component.literal("Welcome! You received " +
                    EconomyManager.formatBalance(startBal) +
                    " " + ModConfig.getCurrencyName() + " as a starting balance!").withStyle(s -> s.withColor(0x55FF55)));

                SoundHelper.playStartingBalanceSound(player);
                LOGGER.info("Gave starting balance of {} to new player {}", startBal, player.getName().getString());
            }

            // Feature 2: Offline sales summary
            if (playerData.hasOfflineSales(player.getUUID())) {
                PlayerDataSavedData.OfflineSalesSummary summary = playerData.getAndClearOfflineSales(player.getUUID());
                if (summary != null && summary.saleCount() > 0) {
                    player.sendSystemMessage(Component.literal("While you were away: " +
                        summary.saleCount() + " sale(s), +" +
                        EconomyManager.formatBalance(summary.totalCoins()) + " " + ModConfig.getCurrencyName() + "!")
                        .withStyle(s -> s.withColor(0x55FF55).withBold(true)));

                    SoundHelper.playSaleSound(player);
                }
            }

            // Feature: Weekly interest
            DailyRewardCommand.checkWeeklyInterest(player);
        }
    }
}
