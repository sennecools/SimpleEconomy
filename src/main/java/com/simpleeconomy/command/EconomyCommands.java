package com.simpleeconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.simpleeconomy.economy.EconomyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EconomyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /balance - check your own balance
        dispatcher.register(Commands.literal("balance")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                double balance = EconomyManager.getBalance(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Your balance: " + EconomyManager.formatBalance(balance) + " coins"
                ), false);
                return 1;
            })
            .then(Commands.argument("player", EntityArgument.player())
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    double balance = EconomyManager.getBalance(target);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        target.getName().getString() + "'s balance: " + EconomyManager.formatBalance(balance) + " coins"
                    ), false);
                    return 1;
                })
            )
        );

        // /bal - alias for balance
        dispatcher.register(Commands.literal("bal")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                double balance = EconomyManager.getBalance(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Your balance: " + EconomyManager.formatBalance(balance) + " coins"
                ), false);
                return 1;
            })
        );

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
                                "Insufficient funds! You only have " + EconomyManager.formatBalance(EconomyManager.getBalance(sender)) + " coins"
                            ));
                            return 0;
                        }

                        EconomyManager.removeBalance(sender, amount);
                        EconomyManager.addBalance(receiver, amount);

                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Sent " + EconomyManager.formatBalance(amount) + " coins to " + receiver.getName().getString()
                        ), false);

                        receiver.sendSystemMessage(Component.literal(
                            "Received " + EconomyManager.formatBalance(amount) + " coins from " + sender.getName().getString()
                        ));

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
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Added " + EconomyManager.formatBalance(amount) + " coins to " + target.getName().getString() +
                                ". New balance: " + EconomyManager.formatBalance(EconomyManager.getBalance(target)) + " coins"
                            ), true);

                            target.sendSystemMessage(Component.literal(
                                "You received " + EconomyManager.formatBalance(amount) + " coins!"
                            ));

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

                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Removed " + EconomyManager.formatBalance(toRemove) + " coins from " + target.getName().getString() +
                                ". New balance: " + EconomyManager.formatBalance(EconomyManager.getBalance(target)) + " coins"
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

                            EconomyManager.setBalance(target, amount);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "Set " + target.getName().getString() + "'s balance to " + EconomyManager.formatBalance(amount) + " coins"
                            ), true);

                            return 1;
                        })
                    )
                )
            )
        );
    }
}
