package com.simpleeconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.network.packets.OpenShopBrowserPacket;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ShopCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /shops - open shop browser GUI
        dispatcher.register(Commands.literal("shops")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                NetworkHandler.sendToPlayer(player, new OpenShopBrowserPacket());
                return 1;
            })
        );

        // /shop - alias for /shops
        dispatcher.register(Commands.literal("shop")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                NetworkHandler.sendToPlayer(player, new OpenShopBrowserPacket());
                return 1;
            })
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(ctx, "name");

                        if (name.length() > 32) {
                            ctx.getSource().sendFailure(Component.literal("Shop name must be 32 characters or less!"));
                            return 0;
                        }

                        Shop shop = ShopManager.createShop(player, name);
                        if (shop == null) {
                            ctx.getSource().sendFailure(Component.literal(
                                "Could not create shop! You may have reached the maximum of " +
                                ShopManager.getMaxShopsPerPlayer() + " shops."
                            ));
                            return 0;
                        }

                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Shop '" + name + "' created! Use /shops to manage it."
                        ), false);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    List<Shop> shops = ShopSavedData.get(player.getServer()).getShopsByOwner(player.getUUID());

                    if (shops.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "You don't have any shops. Use /shop create <name> to create one!"
                        ), false);
                        return 1;
                    }

                    ctx.getSource().sendSuccess(() -> Component.literal("Your shops:"), false);
                    for (Shop shop : shops) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            " - " + shop.getShopName() + " (" + shop.getItems().size() + " items, " +
                            shop.getTotalSales() + " sales)"
                        ), false);
                    }
                    return 1;
                })
            )
        );

        // /marketplace - another alias
        dispatcher.register(Commands.literal("marketplace")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                NetworkHandler.sendToPlayer(player, new OpenShopBrowserPacket());
                return 1;
            })
        );
    }
}
