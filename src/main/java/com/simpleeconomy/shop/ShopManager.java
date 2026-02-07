package com.simpleeconomy.shop;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.economy.EconomyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ShopManager {

    private static final int MAX_SHOPS_PER_PLAYER = 1;
    private static final int MAX_ITEMS_PER_SHOP = 27; // 3 rows of 9

    public static Shop createShop(ServerPlayer player, String shopName) {
        MinecraftServer server = player.getServer();
        ShopSavedData data = ShopSavedData.get(server);

        List<Shop> existingShops = data.getShopsByOwner(player.getUUID());
        if (existingShops.size() >= MAX_SHOPS_PER_PLAYER) {
            return null;
        }

        Shop shop = new Shop(player.getUUID(), player.getName().getString(), shopName);
        data.addShop(shop);

        SimpleEconomy.LOGGER.info("Player {} created shop: {}", player.getName().getString(), shopName);
        return shop;
    }

    public static boolean deleteShop(ServerPlayer player, UUID shopId) {
        MinecraftServer server = player.getServer();
        ShopSavedData data = ShopSavedData.get(server);

        Shop shop = data.getShop(shopId);
        if (shop == null) {
            return false;
        }

        // Only owner or admin can delete
        if (!shop.getOwnerUUID().equals(player.getUUID()) && !player.hasPermissions(2)) {
            return false;
        }

        data.removeShop(shopId);
        SimpleEconomy.LOGGER.info("Shop deleted: {} by {}", shop.getShopName(), player.getName().getString());
        return true;
    }

    public static boolean addItemToShop(ServerPlayer player, UUID shopId, ItemStack itemStack, double price) {
        MinecraftServer server = player.getServer();
        ShopSavedData data = ShopSavedData.get(server);

        Shop shop = data.getShop(shopId);
        if (shop == null || !shop.getOwnerUUID().equals(player.getUUID())) {
            return false;
        }

        if (shop.getItems().size() >= MAX_ITEMS_PER_SHOP) {
            return false;
        }

        if (itemStack.isEmpty() || price <= 0) {
            return false;
        }

        // Stock = number of items that can be sold
        // Each purchase sells 1 item at the set price
        int stock = itemStack.getCount();
        ItemStack singleItem = itemStack.copy();
        singleItem.setCount(1);
        ShopItem shopItem = new ShopItem(singleItem, price, stock);
        shop.addItem(shopItem);
        data.setDirty();

        SimpleEconomy.LOGGER.info("Added item to shop: {} (stock: {}) at {} coins each",
            itemStack.getDisplayName().getString(), stock, price);

        return true;
    }

    public static boolean removeItemFromShop(ServerPlayer player, UUID shopId, UUID itemId) {
        MinecraftServer server = player.getServer();
        ShopSavedData data = ShopSavedData.get(server);

        Shop shop = data.getShop(shopId);
        if (shop == null || !shop.getOwnerUUID().equals(player.getUUID())) {
            return false;
        }

        shop.removeItem(itemId);
        data.setDirty();
        return true;
    }

    public static PurchaseResult purchaseItem(ServerPlayer buyer, UUID shopId, UUID itemId, int quantity) {
        MinecraftServer server = buyer.getServer();
        ShopSavedData data = ShopSavedData.get(server);

        Shop shop = data.getShop(shopId);
        if (shop == null) {
            return new PurchaseResult(false, "Shop not found", null, 0);
        }

        ShopItem item = shop.getItem(itemId);
        if (item == null) {
            return new PurchaseResult(false, "Item not found", null, 0);
        }

        if (item.getStock() < quantity) {
            return new PurchaseResult(false, "Not enough stock", null, 0);
        }

        double totalPrice = item.getPrice() * quantity;
        double tax = EconomyManager.calculateTax(totalPrice);
        double sellerReceives = totalPrice - tax;

        if (!EconomyManager.hasBalance(buyer, totalPrice)) {
            return new PurchaseResult(false, "Insufficient funds", null, 0);
        }

        // Process transaction
        EconomyManager.removeBalance(buyer, totalPrice);
        EconomyManager.addBalance(server, shop.getOwnerUUID(), sellerReceives);

        // Update stock
        item.removeStock(quantity);
        shop.addSale(totalPrice);
        data.setDirty();

        // Create item to give to buyer
        ItemStack purchasedItem = item.getItemStack().copy();
        purchasedItem.setCount(quantity * item.getItemStack().getCount());

        SimpleEconomy.LOGGER.info("Purchase: {} bought {}x {} from {} for {} coins",
            buyer.getName().getString(),
            quantity,
            purchasedItem.getDisplayName().getString(),
            shop.getOwnerName(),
            totalPrice
        );

        return new PurchaseResult(true, "Purchase successful", purchasedItem, totalPrice);
    }

    public static List<Shop> getShopsSorted(MinecraftServer server, SortType sortType) {
        ShopSavedData data = ShopSavedData.get(server);
        List<Shop> shops = data.getAllShops();

        Comparator<Shop> comparator = switch (sortType) {
            case NEWEST -> Comparator.comparingLong(Shop::getCreatedTime).reversed();
            case OLDEST -> Comparator.comparingLong(Shop::getCreatedTime);
            case MOST_SALES -> Comparator.comparingLong(Shop::getTotalSales).reversed();
            case ALPHABETICAL -> Comparator.comparing(Shop::getShopName, String.CASE_INSENSITIVE_ORDER);
            case FEATURED_FIRST -> Comparator.comparing(Shop::isFeatured).reversed()
                .thenComparing(Shop::getTotalSales, Comparator.reverseOrder());
        };

        return shops.stream().sorted(comparator).collect(Collectors.toList());
    }

    public static List<Shop> searchItems(MinecraftServer server, String query) {
        ShopSavedData data = ShopSavedData.get(server);
        String lowerQuery = query.toLowerCase();

        return data.getAllShops().stream()
            .filter(shop -> shop.getItems().stream()
                .anyMatch(item -> item.getItemStack().getDisplayName().getString().toLowerCase().contains(lowerQuery)))
            .collect(Collectors.toList());
    }

    public static int getMaxShopsPerPlayer() {
        return MAX_SHOPS_PER_PLAYER;
    }

    public static int getMaxItemsPerShop() {
        return MAX_ITEMS_PER_SHOP;
    }

    public enum SortType {
        NEWEST,
        OLDEST,
        MOST_SALES,
        ALPHABETICAL,
        FEATURED_FIRST
    }

    public record PurchaseResult(boolean success, String message, ItemStack item, double price) {
    }
}
