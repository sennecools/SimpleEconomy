package com.simpleeconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.economy.EconomyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CoinflipCommand {

    // Pending challenges: target UUID -> Challenge
    private static final Map<UUID, Challenge> pendingChallenges = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Random random = new Random();

    // Challenge timeout in seconds
    private static final int CHALLENGE_TIMEOUT = 60;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("coinflip")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(1.0))
                    .executes(ctx -> {
                        ServerPlayer challenger = ctx.getSource().getPlayerOrException();
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        double amount = DoubleArgumentType.getDouble(ctx, "amount");
                        return challenge(challenger, target, amount);
                    })
                )
            )
            .then(Commands.literal("accept")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    return acceptChallenge(player);
                })
            )
            .then(Commands.literal("deny")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    return denyChallenge(player);
                })
            )
            .then(Commands.literal("cancel")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    return cancelChallenge(player);
                })
            )
        );

        // Aliases
        dispatcher.register(Commands.literal("cf")
            .redirect(dispatcher.getRoot().getChild("coinflip"))
        );
    }

    private static int challenge(ServerPlayer challenger, ServerPlayer target, double amount) {
        // Can't challenge yourself
        if (challenger.getUUID().equals(target.getUUID())) {
            challenger.sendSystemMessage(Component.literal("You can't coinflip yourself!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check if challenger has enough money
        if (!EconomyManager.hasBalance(challenger, amount)) {
            challenger.sendSystemMessage(Component.literal("You don't have enough coins! You need ")
                .withStyle(s -> s.withColor(0xFF5555))
                .append(Component.literal(EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName())
                    .withStyle(s -> s.withColor(0xFFD700))));
            return 0;
        }

        // Check if target has enough money
        if (!EconomyManager.hasBalance(target, amount)) {
            challenger.sendSystemMessage(Component.literal(target.getName().getString() + " doesn't have enough coins!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check if target already has a pending challenge
        if (pendingChallenges.containsKey(target.getUUID())) {
            challenger.sendSystemMessage(Component.literal(target.getName().getString() + " already has a pending coinflip!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check if challenger already sent a challenge
        for (Challenge c : pendingChallenges.values()) {
            if (c.challengerUUID.equals(challenger.getUUID())) {
                challenger.sendSystemMessage(Component.literal("You already have a pending challenge! Use /coinflip cancel to cancel it.")
                    .withStyle(s -> s.withColor(0xFF5555)));
                return 0;
            }
        }

        // Create challenge
        Challenge challenge = new Challenge(challenger.getUUID(), challenger.getName().getString(),
            target.getUUID(), target.getName().getString(), amount, challenger.getServer());
        pendingChallenges.put(target.getUUID(), challenge);

        // Notify both players
        challenger.sendSystemMessage(Component.literal("")
            .append(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true)))
            .append(Component.literal("Challenge sent to ").withStyle(s -> s.withColor(0xFFFF55)))
            .append(Component.literal(target.getName().getString()).withStyle(s -> s.withColor(0x55FF55)))
            .append(Component.literal(" for ").withStyle(s -> s.withColor(0xFFFF55)))
            .append(Component.literal(EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName()).withStyle(s -> s.withColor(0xFFD700)))
            .append(Component.literal("!").withStyle(s -> s.withColor(0xFFFF55))));

        // Create clickable accept button
        Component acceptButton = Component.literal(" [ACCEPT] ")
            .withStyle(s -> s
                .withColor(0x55FF55)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/coinflip accept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to accept the coinflip!").withStyle(st -> st.withColor(0x55FF55)))));

        // Create clickable deny button
        Component denyButton = Component.literal(" [DENY] ")
            .withStyle(s -> s
                .withColor(0xFF5555)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/coinflip deny"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to deny the coinflip!").withStyle(st -> st.withColor(0xFF5555)))));

        target.sendSystemMessage(Component.literal(""));
        target.sendSystemMessage(Component.literal("=== COINFLIP CHALLENGE ===")
            .withStyle(s -> s.withColor(0xFFD700).withBold(true)));
        target.sendSystemMessage(Component.literal(" ")
            .append(Component.literal(challenger.getName().getString()).withStyle(s -> s.withColor(0x55FF55)))
            .append(Component.literal(" challenges you!").withStyle(s -> s.withColor(0xFFFFFF))));
        target.sendSystemMessage(Component.literal(" Amount: ").withStyle(s -> s.withColor(0xAAAAAA))
            .append(Component.literal(EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName()).withStyle(s -> s.withColor(0xFFD700))));
        target.sendSystemMessage(Component.literal(" ")
            .append(acceptButton)
            .append(Component.literal("  "))
            .append(denyButton));

        // Schedule timeout
        scheduler.schedule(() -> {
            Challenge c = pendingChallenges.remove(target.getUUID());
            if (c != null) {
                ServerPlayer p1 = c.server.getPlayerList().getPlayer(c.challengerUUID);
                ServerPlayer p2 = c.server.getPlayerList().getPlayer(c.targetUUID);
                if (p1 != null) {
                    p1.sendSystemMessage(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
                        .append(Component.literal("Challenge to " + c.targetName + " expired!").withStyle(s -> s.withColor(0xFF5555).withBold(false))));
                }
                if (p2 != null) {
                    p2.sendSystemMessage(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
                        .append(Component.literal("Challenge from " + c.challengerName + " expired!").withStyle(s -> s.withColor(0xFF5555).withBold(false))));
                }
            }
        }, CHALLENGE_TIMEOUT, TimeUnit.SECONDS);

        return 1;
    }

    private static int acceptChallenge(ServerPlayer player) {
        Challenge challenge = pendingChallenges.remove(player.getUUID());
        if (challenge == null) {
            player.sendSystemMessage(Component.literal("You don't have any pending coinflip challenges!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        ServerPlayer challenger = player.getServer().getPlayerList().getPlayer(challenge.challengerUUID);
        if (challenger == null) {
            player.sendSystemMessage(Component.literal("The challenger is no longer online!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Verify both still have the money
        if (!EconomyManager.hasBalance(challenger, challenge.amount)) {
            player.sendSystemMessage(Component.literal(challenger.getName().getString() + " no longer has enough coins!")
                .withStyle(s -> s.withColor(0xFF5555)));
            challenger.sendSystemMessage(Component.literal("Coinflip cancelled - you don't have enough coins!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }
        if (!EconomyManager.hasBalance(player, challenge.amount)) {
            player.sendSystemMessage(Component.literal("You no longer have enough coins!")
                .withStyle(s -> s.withColor(0xFF5555)));
            challenger.sendSystemMessage(Component.literal("Coinflip cancelled - " + player.getName().getString() + " doesn't have enough coins!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Take money from both
        EconomyManager.removeBalance(challenger, challenge.amount);
        EconomyManager.removeBalance(player, challenge.amount);

        // Start the flip animation
        startCoinflip(challenger, player, challenge.amount);
        return 1;
    }

    private static int denyChallenge(ServerPlayer player) {
        Challenge challenge = pendingChallenges.remove(player.getUUID());
        if (challenge == null) {
            player.sendSystemMessage(Component.literal("You don't have any pending coinflip challenges!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        ServerPlayer challenger = player.getServer().getPlayerList().getPlayer(challenge.challengerUUID);

        player.sendSystemMessage(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
            .append(Component.literal("You denied the challenge from " + challenge.challengerName + "!").withStyle(s -> s.withColor(0xFF5555).withBold(false))));

        if (challenger != null) {
            challenger.sendSystemMessage(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
                .append(Component.literal(player.getName().getString() + " denied your coinflip challenge!").withStyle(s -> s.withColor(0xFF5555).withBold(false))));
        }

        return 1;
    }

    private static int cancelChallenge(ServerPlayer player) {
        // Find and remove challenge where player is the challenger
        Challenge toRemove = null;
        UUID targetUUID = null;
        for (Map.Entry<UUID, Challenge> entry : pendingChallenges.entrySet()) {
            if (entry.getValue().challengerUUID.equals(player.getUUID())) {
                toRemove = entry.getValue();
                targetUUID = entry.getKey();
                break;
            }
        }

        if (toRemove == null) {
            player.sendSystemMessage(Component.literal("You don't have any outgoing coinflip challenges!")
                .withStyle(s -> s.withColor(0xFF5555)));
            return 0;
        }

        pendingChallenges.remove(targetUUID);

        ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetUUID);
        player.sendSystemMessage(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
            .append(Component.literal("Challenge cancelled!").withStyle(s -> s.withColor(0xFFAA00).withBold(false))));

        if (target != null) {
            target.sendSystemMessage(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
                .append(Component.literal(player.getName().getString() + " cancelled the coinflip challenge!").withStyle(s -> s.withColor(0xFFAA00).withBold(false))));
        }

        return 1;
    }

    private static void startCoinflip(ServerPlayer player1, ServerPlayer player2, double amount) {
        double totalPot = amount * 2;

        // Send to both players only
        sendToBoth(player1, player2, Component.literal(""));
        sendToBoth(player1, player2, Component.literal("=== COINFLIP ===")
            .withStyle(s -> s.withColor(0xFFD700).withBold(true)));
        sendToBoth(player1, player2, Component.literal(" ")
            .append(Component.literal(player1.getName().getString()).withStyle(s -> s.withColor(0x55FF55).withBold(true)))
            .append(Component.literal(" vs ").withStyle(s -> s.withColor(0xAAAAAA)))
            .append(Component.literal(player2.getName().getString()).withStyle(s -> s.withColor(0x55FFFF).withBold(true))));
        sendToBoth(player1, player2, Component.literal(" Pot: ")
            .withStyle(s -> s.withColor(0xAAAAAA))
            .append(Component.literal(EconomyManager.formatBalance(totalPot) + " " + ModConfig.getCurrencyName()).withStyle(s -> s.withColor(0xFFD700).withBold(true))));

        // Determine winner now (but reveal later)
        boolean player1Wins = random.nextBoolean();
        ServerPlayer winner = player1Wins ? player1 : player2;
        ServerPlayer loser = player1Wins ? player2 : player1;

        // Animation sequence
        String[] flipFrames = {
            " ◐ Flipping...",
            " ◓ Flipping.. ",
            " ◑ Flipping.  ",
            " ◒ Flipping   ",
            " ◐ Flipping.  ",
            " ◓ Flipping.. ",
            " ◑ Flipping...",
            " ◒ Flipping..."
        };

        // Schedule animation frames
        for (int i = 0; i < 8; i++) {
            final int frame = i;
            scheduler.schedule(() -> {
                sendToBoth(player1, player2, Component.literal(flipFrames[frame])
                    .withStyle(s -> s.withColor(0xFFFF55)));
            }, 300L * i, TimeUnit.MILLISECONDS);
        }

        // Final result after animation
        scheduler.schedule(() -> {
            sendToBoth(player1, player2, Component.literal(""));
            sendToBoth(player1, player2, Component.literal("=== WINNER ===")
                .withStyle(s -> s.withColor(0x55FF55).withBold(true)));
            sendToBoth(player1, player2, Component.literal(" ")
                .append(Component.literal(winner.getName().getString()).withStyle(s -> s.withColor(0x55FF55).withBold(true)))
                .append(Component.literal(" won ").withStyle(s -> s.withColor(0xAAAAAA)))
                .append(Component.literal(EconomyManager.formatBalance(totalPot) + " " + ModConfig.getCurrencyName() + "!").withStyle(s -> s.withColor(0xFFD700).withBold(true))));

            // Give winner the pot
            EconomyManager.addBalance(player1.getServer(), winner.getUUID(), totalPot);

            // Personal messages
            winner.sendSystemMessage(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
                .append(Component.literal("You won ").withStyle(s -> s.withColor(0x55FF55).withBold(false)))
                .append(Component.literal(EconomyManager.formatBalance(totalPot) + " " + ModConfig.getCurrencyName()).withStyle(s -> s.withColor(0xFFD700).withBold(false)))
                .append(Component.literal(" against " + loser.getName().getString() + "!").withStyle(s -> s.withColor(0x55FF55).withBold(false))));

            loser.sendSystemMessage(Component.literal("[COINFLIP] ").withStyle(s -> s.withColor(0xFFD700).withBold(true))
                .append(Component.literal("You lost ").withStyle(s -> s.withColor(0xFF5555).withBold(false)))
                .append(Component.literal(EconomyManager.formatBalance(amount) + " " + ModConfig.getCurrencyName()).withStyle(s -> s.withColor(0xFFD700).withBold(false)))
                .append(Component.literal(" to " + winner.getName().getString() + "!").withStyle(s -> s.withColor(0xFF5555).withBold(false))));

        }, 2500L, TimeUnit.MILLISECONDS);
    }

    private static void sendToBoth(ServerPlayer p1, ServerPlayer p2, Component message) {
        p1.sendSystemMessage(message);
        p2.sendSystemMessage(message);
    }

    private record Challenge(UUID challengerUUID, String challengerName,
                            UUID targetUUID, String targetName,
                            double amount, MinecraftServer server) {
    }
}
