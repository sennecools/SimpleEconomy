package com.simpleeconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.TransactionLog;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.util.SoundHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.*;

import com.mojang.authlib.GameProfile;

public class EconomyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /bal and /balance with shared command tree
        registerBalanceCommand(dispatcher, "bal");
        registerBalanceCommand(dispatcher, "balance");

        // /pay <player> <amount> - send money to another player
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(ctx -> {
                        ServerPlayer sender = ctx.getSource().getPlayerOrException();
                        ServerPlayer receiver = EntityArgument.getPlayer(ctx, "player");
                        double amount = DoubleArgumentType.getDouble(ctx, "amount");

                        if (sender.getUUID().equals(receiver.getUUID())) {
                            ctx.getSource().sendFailure(Component.literal("You cannot pay yourself!"));
                            return 0;
                        }

                        if (!EconomyManager.hasBalance(sender, amount)) {
                            ctx.getSource().sendFailure(Component.literal(
                                "Insufficient funds! You only have " + EconomyManager.formatBalance(EconomyManager.getBalance(sender)) + " " + ModConfig.getCurrencyName()
                            ));
                            return 0;
                        }

                        EconomyManager.removeBalance(sender, amount);
                        EconomyManager.addBalance(receiver, amount);

                        // Log transactions
                        TransactionLog log = TransactionLog.get(sender.getServer());
                        log.addTransaction(sender.getUUID(), TransactionLog.Transaction.payment(
                            amount, receiver.getUUID(), receiver.getName().getString()
                        ));
                        log.addTransaction(receiver.getUUID(), TransactionLog.Transaction.received(
                            amount, sender.getUUID(), sender.getName().getString()
                        ));

                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Sent " + EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName() + " to " + receiver.getName().getString()
                        ).withStyle(s -> s.withColor(0x55FF55)), false);

                        receiver.sendSystemMessage(Component.literal("[PAY] ")
                            .withStyle(s -> s.withColor(0x55FFFF).withBold(true))
                            .append(Component.literal("Received " + EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName() + " from " + sender.getName().getString())
                                .withStyle(s -> s.withColor(0x55FFFF).withBold(false))));
                        SoundHelper.playPaymentReceivedSound(receiver);

                        return 1;
                    })
                )
            )
        );

        // /eco - admin economy commands
        dispatcher.register(Commands.literal("eco")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            double amount = DoubleArgumentType.getDouble(ctx, "amount");

                            EconomyManager.addBalance(target, amount);

                            // Log the transaction
                            TransactionLog log = TransactionLog.get(target.getServer());
                            log.addTransaction(target.getUUID(), TransactionLog.Transaction.adminAdd(amount));

                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Added " + EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName() + " to " + target.getName().getString() +
                                ". New balance: " + EconomyManager.formatBalance(EconomyManager.getBalance(target)) + " " + ModConfig.getCurrencyName()
                            ), true);

                            target.sendSystemMessage(Component.literal(
                                "You received " + EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName() + "!"
                            ).withStyle(s -> s.withColor(0x55FF55)));

                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            double amount = DoubleArgumentType.getDouble(ctx, "amount");

                            double current = EconomyManager.getBalance(target);
                            double toRemove = Math.min(amount, current);
                            EconomyManager.removeBalance(target, toRemove);

                            // Log the transaction
                            TransactionLog log = TransactionLog.get(target.getServer());
                            log.addTransaction(target.getUUID(), TransactionLog.Transaction.adminRemove(toRemove));

                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Removed " + EconomyManager.formatBalance(toRemove) + " " + ModConfig.getCurrencyName() + " from " + target.getName().getString() +
                                ". New balance: " + EconomyManager.formatBalance(EconomyManager.getBalance(target)) + " " + ModConfig.getCurrencyName()
                            ), true);

                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            double amount = DoubleArgumentType.getDouble(ctx, "amount");

                            double oldBalance = EconomyManager.getBalance(target);
                            EconomyManager.setBalance(target, amount);

                            // Log the transaction
                            TransactionLog log = TransactionLog.get(target.getServer());
                            if (amount > oldBalance) {
                                log.addTransaction(target.getUUID(), TransactionLog.Transaction.adminAdd(amount - oldBalance));
                            } else if (amount < oldBalance) {
                                log.addTransaction(target.getUUID(), TransactionLog.Transaction.adminRemove(oldBalance - amount));
                            }

                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Set " + target.getName().getString() + "'s balance to " + EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName()
                            ), true);

                            target.sendSystemMessage(Component.literal(
                                "Your balance has been set to " + EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName()
                            ).withStyle(s -> s.withColor(0xFFFF55)));

                            return 1;
                        })
                    )
                )
            )
        );

        // /transactions - view transaction history in chat
        dispatcher.register(Commands.literal("transactions")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                showTransactionHistory(ctx.getSource(), player, 10);
                return 1;
            })
        );

        // /history - alias for /transactions
        dispatcher.register(Commands.literal("history")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                showTransactionHistory(ctx.getSource(), player, 10);
                return 1;
            })
        );

        // /baltop - money leaderboard (standalone alias)
        dispatcher.register(Commands.literal("baltop")
            .executes(ctx -> {
                showLeaderboard(ctx.getSource(), 1);
                return 1;
            })
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> {
                    int page = IntegerArgumentType.getInteger(ctx, "page");
                    showLeaderboard(ctx.getSource(), page);
                    return 1;
                })
            )
        );

        // /leaderboard - alias for /baltop
        dispatcher.register(Commands.literal("leaderboard")
            .executes(ctx -> {
                showLeaderboard(ctx.getSource(), 1);
                return 1;
            })
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> {
                    int page = IntegerArgumentType.getInteger(ctx, "page");
                    showLeaderboard(ctx.getSource(), page);
                    return 1;
                })
            )
        );

        // /richest - alias for /baltop
        dispatcher.register(Commands.literal("richest")
            .executes(ctx -> {
                showLeaderboard(ctx.getSource(), 1);
                return 1;
            })
        );
    }

    private static void registerBalanceCommand(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(Commands.literal(name)
            // /bal — own balance
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                double balance = EconomyManager.getBalance(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Your balance: " + EconomyManager.formatBalance(balance) + " " + ModConfig.getCurrencyName()
                ).withStyle(s -> s.withColor(0xFFD700)), false);
                return 1;
            })
            // /bal top [page] — leaderboard subcommand
            .then(Commands.literal("top")
                .executes(ctx -> {
                    showLeaderboard(ctx.getSource(), 1);
                    return 1;
                })
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int page = IntegerArgumentType.getInteger(ctx, "page");
                        showLeaderboard(ctx.getSource(), page);
                        return 1;
                    })
                )
            )
            // /bal <player> — any player can look up another
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    double balance = EconomyManager.getBalance(target);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        target.getName().getString() + "'s balance: " + EconomyManager.formatBalance(balance) + " " + ModConfig.getCurrencyName()
                    ).withStyle(s -> s.withColor(0xFFD700)), false);
                    return 1;
                })
            )
        );
    }

    private static void showTransactionHistory(CommandSourceStack source, ServerPlayer player, int limit) {
        TransactionLog log = TransactionLog.get(player.getServer());
        List<TransactionLog.Transaction> transactions = log.getRecentTransactions(player.getUUID(), limit);

        if (transactions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No transactions found.").withStyle(s -> s.withColor(0x888888)), false);
            return;
        }

        source.sendSuccess(() -> Component.literal("=== Transaction History ===").withStyle(s -> s.withBold(true).withColor(0xFFD700)), false);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
        for (TransactionLog.Transaction t : transactions) {
            String amountStr = (t.amount() >= 0 ? "+" : "") + EconomyManager.formatBalance(t.amount());
            int color = t.amount() >= 0 ? 0x55FF55 : 0xFF5555;
            source.sendSuccess(() -> Component.literal(
                " " + sdf.format(new Date(t.timestamp())) + " | " + t.description() + " | " + amountStr
            ).withStyle(s -> s.withColor(color)), false);
        }
    }

    private static final int ENTRIES_PER_PAGE = 10;

    private static void showLeaderboard(CommandSourceStack source, int page) {
        var server = source.getServer();
        List<Map.Entry<UUID, Double>> topBalances = EconomyManager.getTopBalances(server);

        if (topBalances.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No players with balances found!").withStyle(s -> s.withColor(0xFF5555)), false);
            return;
        }

        int totalPages = (int) Math.ceil((double) topBalances.size() / ENTRIES_PER_PAGE);
        page = Math.min(page, totalPages);
        int startIdx = (page - 1) * ENTRIES_PER_PAGE;
        int endIdx = Math.min(startIdx + ENTRIES_PER_PAGE, topBalances.size());

        // Header
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("=== RICHEST PLAYERS ===")
            .withStyle(s -> s.withColor(0xFFD700).withBold(true)), false);

        // Entries
        for (int i = startIdx; i < endIdx; i++) {
            Map.Entry<UUID, Double> entry = topBalances.get(i);
            int rank = i + 1;
            String playerName = getPlayerName(server, entry.getKey());
            double balance = entry.getValue();

            int nameColor;
            if (rank == 1) {
                nameColor = 0xFFD700; // Gold
            } else if (rank == 2) {
                nameColor = 0xC0C0C0; // Silver
            } else if (rank == 3) {
                nameColor = 0xCD7F32; // Bronze
            } else {
                nameColor = 0xFFFFFF;
            }

            final int r = rank;
            final String n = playerName;
            final int nc = nameColor;
            final String bal = EconomyManager.formatBalance(balance);
            final String currency = ModConfig.getCurrencyName();

            source.sendSuccess(() -> Component.literal(String.format("%2d. ", r)).withStyle(s -> s.withColor(0xAAAAAA))
                .append(Component.literal(n).withStyle(s -> s.withColor(nc)))
                .append(Component.literal(" - ").withStyle(s -> s.withColor(0x555555)))
                .append(Component.literal(bal).withStyle(s -> s.withColor(0x55FF55)))
                .append(Component.literal(" " + currency).withStyle(s -> s.withColor(0x888888))), false);
        }

        // Footer with page info
        final int p = page;
        final int tp = totalPages;
        source.sendSuccess(() -> Component.literal("Page " + p + "/" + tp)
            .withStyle(s -> s.withColor(0xAAAAAA))
            .append(Component.literal(" - /baltop " + (p < tp ? (p + 1) : 1))
                .withStyle(s -> s.withColor(0x888888))), false);

        // Show requester's rank if they're a player and not in top 10
        try {
            ServerPlayer player = source.getPlayerOrException();
            int playerRank = EconomyManager.getPlayerRank(server, player.getUUID());
            double playerBalance = EconomyManager.getBalance(player);

            if (playerRank > ENTRIES_PER_PAGE || page > 1) {
                source.sendSuccess(() -> Component.literal("Your rank: #" + playerRank + " (" +
                    EconomyManager.formatBalance(playerBalance) + " " + ModConfig.getCurrencyName() + ")")
                    .withStyle(s -> s.withColor(0x55FFFF)), false);
            }
        } catch (Exception ignored) {
            // Console doesn't have a rank
        }

        source.sendSuccess(() -> Component.literal(""), false);
    }

    private static String getPlayerName(net.minecraft.server.MinecraftServer server, UUID uuid) {
        // Check if player is online
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName().getString();
        }

        // Try to get cached profile - this returns the profile if it exists
        var profileCache = server.getProfileCache();
        if (profileCache != null) {
            Optional<GameProfile> profile = profileCache.get(uuid);
            if (profile.isPresent() && profile.get().getName() != null) {
                return profile.get().getName();
            }
        }

        // Fallback to shortened UUID
        return uuid.toString().substring(0, 8) + "...";
    }
}
