package com.simpleeconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.DailyRewardData;
import com.simpleeconomy.data.TransactionLog;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.util.SoundHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DailyRewardCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("daily")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return claimDaily(player);
            })
        );

        // Alias
        dispatcher.register(Commands.literal("claim")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return claimDaily(player);
            })
        );

        // Check streak without claiming
        dispatcher.register(Commands.literal("streak")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return showStreak(player);
            })
        );
    }

    private static int claimDaily(ServerPlayer player) {
        DailyRewardData data = DailyRewardData.get(player.getServer());
        DailyRewardData.PlayerRewardInfo info = data.getPlayerInfo(player.getUUID());

        long currentDay = DailyRewardData.getCurrentDay();
        long daysSinceLastClaim = currentDay - info.lastClaimDay;

        // Already claimed today
        if (daysSinceLastClaim == 0) {
            player.sendSystemMessage(Component.literal("[DAILY] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
                .append(Component.literal("You already claimed your daily reward today!").withStyle(s -> s.withColor(0xFF5555).withBold(false))));
            player.sendSystemMessage(Component.literal("  Current streak: ").withStyle(s -> s.withColor(0xAAAAAA))
                .append(Component.literal(info.streak + " day(s)").withStyle(s -> s.withColor(0x55FF55))));
            return 0;
        }

        // Calculate new streak
        int newStreak;
        if (daysSinceLastClaim == 1) {
            // Consecutive day - increase streak
            newStreak = Math.min(info.streak + 1, ModConfig.getMaxStreak());
        } else {
            // Streak broken - reset to 1
            newStreak = 1;
        }

        // Calculate reward based on streak
        int reward = ModConfig.getDailyBaseReward() + (newStreak - 1) * ModConfig.getDailyRewardIncrement();

        // Give reward
        EconomyManager.addBalance(player, reward);

        // Log transaction
        TransactionLog log = TransactionLog.get(player.getServer());
        log.addTransaction(player.getUUID(), TransactionLog.Transaction.dailyReward(reward, newStreak));

        // Update data
        info.streak = newStreak;
        info.lastClaimDay = currentDay;
        data.updatePlayerInfo(player.getUUID(), info);

        // Send message
        String currency = ModConfig.getCurrencyName();

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("=== DAILY REWARD CLAIMED ===")
            .withStyle(s -> s.withColor(0xFFD700).withBold(true)));

        player.sendSystemMessage(Component.literal(" Reward: ").withStyle(s -> s.withColor(0xAAAAAA))
            .append(Component.literal("+" + reward + " " + currency).withStyle(s -> s.withColor(0x55FF55).withBold(true))));

        int maxStreak = ModConfig.getMaxStreak();
        player.sendSystemMessage(Component.literal(" Streak: ").withStyle(s -> s.withColor(0xAAAAAA))
            .append(Component.literal(newStreak + "/" + maxStreak + " days").withStyle(s -> s.withColor(newStreak >= maxStreak ? 0xFF5555 : 0xFFFF55))));

        if (newStreak < maxStreak) {
            int nextReward = ModConfig.getDailyBaseReward() + newStreak * ModConfig.getDailyRewardIncrement();
            player.sendSystemMessage(Component.literal(" Tomorrow: ").withStyle(s -> s.withColor(0xAAAAAA))
                .append(Component.literal("+" + nextReward + " " + currency).withStyle(s -> s.withColor(0x888888))));
        } else {
            player.sendSystemMessage(Component.literal(" MAX STREAK!").withStyle(s -> s.withColor(0xFF5555).withBold(true)));
        }

        player.sendSystemMessage(Component.literal(""));

        SoundHelper.playDailyRewardSound(player);

        return 1;
    }

    private static int showStreak(ServerPlayer player) {
        DailyRewardData data = DailyRewardData.get(player.getServer());
        DailyRewardData.PlayerRewardInfo info = data.getPlayerInfo(player.getUUID());

        long currentDay = DailyRewardData.getCurrentDay();
        long daysSinceLastClaim = currentDay - info.lastClaimDay;

        int displayStreak = info.streak;
        boolean canClaim = daysSinceLastClaim > 0;
        boolean streakBroken = daysSinceLastClaim > 1;

        if (streakBroken && info.lastClaimDay > 0) {
            displayStreak = 0; // Streak would reset
        }

        player.sendSystemMessage(Component.literal("[STREAK] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
            .append(Component.literal("Your streak: ").withStyle(s -> s.withColor(0xAAAAAA).withBold(false)))
            .append(Component.literal(displayStreak + "/" + ModConfig.getMaxStreak() + " days").withStyle(s -> s.withColor(0x55FF55).withBold(false))));

        if (canClaim) {
            if (streakBroken && info.lastClaimDay > 0) {
                player.sendSystemMessage(Component.literal("  \u26A0 Streak broken! Use /daily to start fresh.").withStyle(s -> s.withColor(0xFFAA00)));
            } else {
                int nextReward = ModConfig.getDailyBaseReward() + Math.min(displayStreak, ModConfig.getMaxStreak() - 1) * ModConfig.getDailyRewardIncrement();
                player.sendSystemMessage(Component.literal("  \u2714 Daily available! Reward: +" + nextReward + " " + ModConfig.getCurrencyName()).withStyle(s -> s.withColor(0x55FF55)));
            }
        } else {
            player.sendSystemMessage(Component.literal("  \u2716 Already claimed today. Come back tomorrow!").withStyle(s -> s.withColor(0xFF5555)));
        }

        return 1;
    }

    /**
     * Check and give weekly interest - call this on player login
     */
    public static void checkWeeklyInterest(ServerPlayer player) {
        DailyRewardData data = DailyRewardData.get(player.getServer());
        DailyRewardData.PlayerRewardInfo info = data.getPlayerInfo(player.getUUID());

        long currentWeek = DailyRewardData.getCurrentWeek();

        // First time or already claimed this week
        if (info.lastInterestDay == 0) {
            info.lastInterestDay = currentWeek;
            data.updatePlayerInfo(player.getUUID(), info);
            return;
        }

        if (info.lastInterestDay >= currentWeek) {
            return; // Already got interest this week
        }

        // Calculate interest
        double balance = EconomyManager.getBalance(player);
        if (balance <= 0) {
            info.lastInterestDay = currentWeek;
            data.updatePlayerInfo(player.getUUID(), info);
            return;
        }

        double interest = balance * ModConfig.getWeeklyInterestRate();
        interest = Math.min(interest, ModConfig.getMaxInterestAmount()); // Cap it

        if (interest < 0.01) {
            info.lastInterestDay = currentWeek;
            data.updatePlayerInfo(player.getUUID(), info);
            return;
        }

        // Give interest
        EconomyManager.addBalance(player, interest);

        // Log it
        TransactionLog log = TransactionLog.get(player.getServer());
        log.addTransaction(player.getUUID(), TransactionLog.Transaction.interest(interest));

        // Update
        info.lastInterestDay = currentWeek;
        data.updatePlayerInfo(player.getUUID(), info);

        // Notify player
        String interestStr = EconomyManager.formatBalance(interest);
        player.sendSystemMessage(Component.literal("[BANK] ").withStyle(s -> s.withColor(0x55FFFF).withBold(true))
            .append(Component.literal("Weekly interest: +").withStyle(s -> s.withColor(0x55FF55).withBold(false)))
            .append(Component.literal(interestStr + " " + ModConfig.getCurrencyName()).withStyle(s -> s.withColor(0xFFD700).withBold(false)))
            .append(Component.literal(" (" + Math.round(ModConfig.getWeeklyInterestRate() * 100) + "% of balance, max " + EconomyManager.formatBalance(ModConfig.getMaxInterestAmount()) + ")").withStyle(s -> s.withColor(0x888888).withBold(false))));
    }

}
