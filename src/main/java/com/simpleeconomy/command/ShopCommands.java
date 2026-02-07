package com.simpleeconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.menu.ShopBrowserMenu;
import com.simpleeconomy.menu.MyShopMenu;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.List;
import java.util.UUID;

public class ShopCommands {

    // Suggestion provider for shop names (for tab completion)
    private static final SuggestionProvider<CommandSourceStack> SHOP_SUGGESTIONS = (ctx, builder) -> {
        ShopSavedData data = ShopSavedData.get(ctx.getSource().getServer());
        for (Shop shop : data.getAllShops()) {
            // Quote names with spaces
            String name = shop.getShopName();
            if (name.contains(" ")) {
                builder.suggest("\"" + name + "\"");
            } else {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /shops - open shop browser GUI
        dispatcher.register(Commands.literal("shops")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                player.openMenu(new ShopBrowserMenu.Provider());
                return 1;
            })
        );

        // /shop - alias for /shops
        dispatcher.register(Commands.literal("shop")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                player.openMenu(new ShopBrowserMenu.Provider());
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
                        ).withStyle(s -> s.withColor(0x55FF55)), false);
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
            .then(Commands.literal("add")
                .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 64))
                    .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int quantity = IntegerArgumentType.getInteger(ctx, "quantity");
                            double price = DoubleArgumentType.getDouble(ctx, "price");

                            // Get player's shop
                            List<Shop> shops = ShopSavedData.get(player.getServer()).getShopsByOwner(player.getUUID());
                            if (shops.isEmpty()) {
                                player.sendSystemMessage(Component.literal("[ERROR] ")
                                    .withStyle(s -> s.withColor(0xFF5555).withBold(true))
                                    .append(Component.literal("You don't have a shop! Use /shop create <name> first.")
                                        .withStyle(s -> s.withColor(0xFF5555).withBold(false))));
                                return 0;
                            }

                            Shop shop = shops.get(0);

                            // Check item limit
                            if (shop.getItems().size() >= ShopManager.getMaxItemsPerShop()) {
                                player.sendSystemMessage(Component.literal("[ERROR] ")
                                    .withStyle(s -> s.withColor(0xFF5555).withBold(true))
                                    .append(Component.literal("Your shop is full! Max " + ShopManager.getMaxItemsPerShop() + " items.")
                                        .withStyle(s -> s.withColor(0xFF5555).withBold(false))));
                                return 0;
                            }

                            // Get item from main hand
                            ItemStack heldItem = player.getMainHandItem();
                            if (heldItem.isEmpty()) {
                                player.sendSystemMessage(Component.literal("[ERROR] ")
                                    .withStyle(s -> s.withColor(0xFF5555).withBold(true))
                                    .append(Component.literal("Hold an item in your main hand!")
                                        .withStyle(s -> s.withColor(0xFF5555).withBold(false))));
                                return 0;
                            }

                            // Check quantity
                            int actualQty = Math.min(quantity, heldItem.getCount());
                            if (actualQty <= 0) {
                                player.sendSystemMessage(Component.literal("[ERROR] ")
                                    .withStyle(s -> s.withColor(0xFF5555).withBold(true))
                                    .append(Component.literal("Invalid quantity!")
                                        .withStyle(s -> s.withColor(0xFF5555).withBold(false))));
                                return 0;
                            }

                            // Create the item stack for shop
                            ItemStack shopStack = heldItem.copy();
                            shopStack.setCount(actualQty);

                            // Remove from player inventory
                            heldItem.shrink(actualQty);

                            // Add to shop
                            if (ShopManager.addItemToShop(player, shop.getShopId(), shopStack, price)) {
                                player.sendSystemMessage(Component.literal("[INFO] ")
                                    .withStyle(s -> s.withColor(0xFFFF55).withBold(true))
                                    .append(Component.literal("Added " + actualQty + "x " + shopStack.getHoverName().getString() + " at " + String.format("%.2f", price) + " coins each!")
                                        .withStyle(s -> s.withColor(0xFFFF55).withBold(false))));
                                return 1;
                            } else {
                                // Return items if failed
                                player.getInventory().add(shopStack);
                                player.sendSystemMessage(Component.literal("[ERROR] ")
                                    .withStyle(s -> s.withColor(0xFF5555).withBold(true))
                                    .append(Component.literal("Failed to add item to shop!")
                                        .withStyle(s -> s.withColor(0xFF5555).withBold(false))));
                                return 0;
                            }
                        })
                    )
                )
            )
            .then(Commands.literal("manage")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.openMenu(new MyShopMenu.Provider());
                    return 1;
                })
            )
            .then(Commands.literal("delete")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    List<Shop> shops = ShopSavedData.get(player.getServer()).getShopsByOwner(player.getUUID());

                    if (shops.isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("You don't have a shop to delete!"));
                        return 0;
                    }

                    Shop shop = shops.get(0);
                    String shopName = shop.getShopName();

                    // Return all items to player
                    for (var item : shop.getItems()) {
                        if (item.getStock() > 0 && !item.isInfiniteStock()) {
                            int remaining = item.getStock();
                            while (remaining > 0) {
                                int toGive = Math.min(remaining, item.getItemStack().getMaxStackSize());
                                ItemStack stack = item.getItemStack().copy();
                                stack.setCount(toGive);
                                if (!player.getInventory().add(stack)) {
                                    player.drop(stack, false);
                                }
                                remaining -= toGive;
                            }
                        }
                    }

                    ShopSavedData data = ShopSavedData.get(player.getServer());
                    data.removeShop(shop.getShopId());

                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Shop '" + shopName + "' deleted! Items returned to your inventory."
                    ).withStyle(s -> s.withColor(0xFFAA00)), false);
                    return 1;
                })
            )
        );

        // /marketplace - another alias
        dispatcher.register(Commands.literal("marketplace")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                player.openMenu(new ShopBrowserMenu.Provider());
                return 1;
            })
        );

        // Admin commands
        dispatcher.register(Commands.literal("shop")
            .then(Commands.literal("feature")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("shop", StringArgumentType.string())
                    .suggests(SHOP_SUGGESTIONS)
                    .executes(ctx -> {
                        String shopNameOrId = StringArgumentType.getString(ctx, "shop");
                        ShopSavedData data = ShopSavedData.get(ctx.getSource().getServer());

                        Shop shop = findShop(data, shopNameOrId);
                        if (shop == null) {
                            ctx.getSource().sendFailure(Component.literal("Shop not found: " + shopNameOrId));
                            return 0;
                        }

                        // Toggle featured status
                        boolean newFeatured = !shop.isFeatured();
                        shop.setFeatured(newFeatured);
                        data.setDirty();

                        final String shopName = shop.getShopName();
                        final String status = newFeatured ? "featured" : "unfeatured";
                        final int statusColor = newFeatured ? 0x55FF55 : 0xFFAA00;
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Shop '" + shopName + "' is now " + status + "!"
                        ).withStyle(s -> s.withColor(statusColor)), true);

                        return 1;
                    })
                )
            )
            .then(Commands.literal("listall")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ShopSavedData data = ShopSavedData.get(ctx.getSource().getServer());
                    List<Shop> shops = data.getAllShops();

                    if (shops.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("No shops exist."), false);
                        return 1;
                    }

                    ctx.getSource().sendSuccess(() -> Component.literal("All shops (use shop name in commands):").withStyle(s -> s.withBold(true)), false);
                    for (Shop shop : shops) {
                        String featuredStr = shop.isFeatured() ? " [FEATURED]" : "";
                        int itemCount = shop.getItems().size();
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            " - " + shop.getShopName() + " (by " + shop.getOwnerName() + ", " + itemCount + " items)" + featuredStr
                        ), false);
                    }
                    return 1;
                })
            )
            .then(Commands.literal("setinfinite")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("shop", StringArgumentType.string())
                    .suggests(SHOP_SUGGESTIONS)
                    .executes(ctx -> {
                        String shopNameOrId = StringArgumentType.getString(ctx, "shop");
                        ShopSavedData data = ShopSavedData.get(ctx.getSource().getServer());

                        Shop shop = findShop(data, shopNameOrId);
                        if (shop == null) {
                            ctx.getSource().sendFailure(Component.literal("Shop not found: " + shopNameOrId));
                            return 0;
                        }

                        // Set all items to infinite stock
                        int itemCount = 0;
                        for (var item : shop.getItems()) {
                            item.setInfiniteStock(true);
                            itemCount++;
                        }
                        data.setDirty();

                        final String shopName = shop.getShopName();
                        final int count = itemCount;
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Set " + count + " items in '" + shopName + "' to infinite stock!"
                        ).withStyle(s -> s.withColor(0x55FF55)), true);

                        return 1;
                    })
                )
            )
            .then(Commands.literal("admindelete")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("shop", StringArgumentType.string())
                    .suggests(SHOP_SUGGESTIONS)
                    .executes(ctx -> {
                        String shopNameOrId = StringArgumentType.getString(ctx, "shop");
                        ShopSavedData data = ShopSavedData.get(ctx.getSource().getServer());

                        Shop shop = findShop(data, shopNameOrId);
                        if (shop == null) {
                            ctx.getSource().sendFailure(Component.literal("Shop not found: " + shopNameOrId));
                            return 0;
                        }

                        String shopName = shop.getShopName();
                        String ownerName = shop.getOwnerName();
                        data.removeShop(shop.getShopId());

                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Deleted shop '" + shopName + "' owned by " + ownerName
                        ).withStyle(s -> s.withColor(0xFF5555)), true);
                        return 1;
                    })
                )
            )
        );
    }

    /**
     * Helper method to find a shop by name or UUID
     */
    private static Shop findShop(ShopSavedData data, String nameOrId) {
        // Try to parse as UUID first
        try {
            UUID shopId = UUID.fromString(nameOrId);
            Shop shop = data.getShop(shopId);
            if (shop != null) return shop;
        } catch (IllegalArgumentException ignored) {
        }

        // Try to find by shop name (case-insensitive)
        for (Shop s : data.getAllShops()) {
            if (s.getShopName().equalsIgnoreCase(nameOrId)) {
                return s;
            }
        }
        return null;
    }
}
