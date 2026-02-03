package com.simpleeconomy.economy;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.data.EconomySavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class EconomyManager {

    private static final double DEFAULT_STARTING_BALANCE = 0.0;
    private static final double TAX_RATE = 0.05; // 5% transaction tax

    public static double getBalance(MinecraftServer server, UUID playerUUID) {
        EconomySavedData data = EconomySavedData.get(server);
        return data.getBalance(playerUUID);
    }

    public static double getBalance(ServerPlayer player) {
        return getBalance(player.getServer(), player.getUUID());
    }

    public static void setBalance(MinecraftServer server, UUID playerUUID, double amount) {
        EconomySavedData data = EconomySavedData.get(server);
        data.setBalance(playerUUID, Math.max(0, amount));
    }

    public static void setBalance(ServerPlayer player, double amount) {
        setBalance(player.getServer(), player.getUUID(), amount);
    }

    public static boolean addBalance(MinecraftServer server, UUID playerUUID, double amount) {
        if (amount < 0) return false;
        EconomySavedData data = EconomySavedData.get(server);
        double current = data.getBalance(playerUUID);
        data.setBalance(playerUUID, current + amount);
        return true;
    }

    public static boolean addBalance(ServerPlayer player, double amount) {
        return addBalance(player.getServer(), player.getUUID(), amount);
    }

    public static boolean removeBalance(MinecraftServer server, UUID playerUUID, double amount) {
        if (amount < 0) return false;
        EconomySavedData data = EconomySavedData.get(server);
        double current = data.getBalance(playerUUID);
        if (current < amount) return false;
        data.setBalance(playerUUID, current - amount);
        return true;
    }

    public static boolean removeBalance(ServerPlayer player, double amount) {
        return removeBalance(player.getServer(), player.getUUID(), amount);
    }

    public static boolean hasBalance(MinecraftServer server, UUID playerUUID, double amount) {
        return getBalance(server, playerUUID) >= amount;
    }

    public static boolean hasBalance(ServerPlayer player, double amount) {
        return hasBalance(player.getServer(), player.getUUID(), amount);
    }

    public static boolean transfer(ServerPlayer from, UUID toUUID, double amount, boolean applyTax) {
        if (amount <= 0) return false;
        if (!hasBalance(from, amount)) return false;

        double tax = applyTax ? amount * TAX_RATE : 0;
        double received = amount - tax;

        removeBalance(from, amount);
        addBalance(from.getServer(), toUUID, received);

        if (tax > 0) {
            SimpleEconomy.LOGGER.debug("Tax collected: {} coins from transfer of {}", tax, amount);
        }

        return true;
    }

    public static double getTaxRate() {
        return TAX_RATE;
    }

    public static double calculateTax(double amount) {
        return amount * TAX_RATE;
    }

    public static String formatBalance(double amount) {
        if (amount == (long) amount) {
            return String.format("%,d", (long) amount);
        } else {
            return String.format("%,.2f", amount);
        }
    }
}
