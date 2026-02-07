package com.simpleeconomy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simpleeconomy.SimpleEconomy;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "simpleeconomy");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

    private static ConfigData data = new ConfigData();

    private static class ConfigData {
        String currencyName = "coins";
        double startingBalance = 100.0;
        double taxRate = 0.05;
        int dailyBaseReward = 100;
        int dailyRewardIncrement = 50;
        int maxStreak = 7;
        double weeklyInterestRate = 0.10;
        double maxInterestAmount = 500.0;
        double killRewardPercent = 0.0;
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIR);

            if (Files.exists(CONFIG_FILE)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                    ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                    if (loaded != null) {
                        data = loaded;
                    }
                }
                SimpleEconomy.LOGGER.info("Loaded config from {}", CONFIG_FILE);
            } else {
                // Write defaults
                save();
                SimpleEconomy.LOGGER.info("Created default config at {}", CONFIG_FILE);
            }

            validate();
        } catch (IOException e) {
            SimpleEconomy.LOGGER.error("Failed to load config, using defaults", e);
            data = new ConfigData();
        }
    }

    private static void save() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        }
    }

    private static void validate() {
        if (data.currencyName == null || data.currencyName.isBlank()) {
            data.currencyName = "coins";
        }
        if (data.startingBalance < 0) data.startingBalance = 0;
        if (data.taxRate < 0) data.taxRate = 0;
        if (data.dailyBaseReward < 0) data.dailyBaseReward = 0;
        if (data.dailyRewardIncrement < 0) data.dailyRewardIncrement = 0;
        if (data.maxStreak < 1) data.maxStreak = 1;
        if (data.weeklyInterestRate < 0) data.weeklyInterestRate = 0;
        if (data.maxInterestAmount < 0) data.maxInterestAmount = 0;
        if (data.killRewardPercent < 0) data.killRewardPercent = 0;
        if (data.killRewardPercent > 1) data.killRewardPercent = 1;
    }

    public static String getCurrencyName() {
        return data.currencyName;
    }

    public static double getStartingBalance() {
        return data.startingBalance;
    }

    public static double getTaxRate() {
        return data.taxRate;
    }

    public static int getDailyBaseReward() {
        return data.dailyBaseReward;
    }

    public static int getDailyRewardIncrement() {
        return data.dailyRewardIncrement;
    }

    public static int getMaxStreak() {
        return data.maxStreak;
    }

    public static double getWeeklyInterestRate() {
        return data.weeklyInterestRate;
    }

    public static double getMaxInterestAmount() {
        return data.maxInterestAmount;
    }

    public static double getKillRewardPercent() {
        return data.killRewardPercent;
    }
}
