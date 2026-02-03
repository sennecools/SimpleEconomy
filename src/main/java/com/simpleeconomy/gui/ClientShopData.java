package com.simpleeconomy.gui;

import com.simpleeconomy.network.packets.SyncShopsPacket;
import com.simpleeconomy.shop.ShopCategory;

import java.util.*;
import java.util.stream.Collectors;

public class ClientShopData {

    private static List<SyncShopsPacket.ShopData> shops = new ArrayList<>();
    private static Set<UUID> favorites = new HashSet<>();
    private static double balance = 0;
    private static String searchQuery = "";
    private static ShopCategory filterCategory = null;
    private static SortType sortType = SortType.FEATURED_FIRST;

    public static void setShops(List<SyncShopsPacket.ShopData> newShops, Set<UUID> newFavorites) {
        shops = new ArrayList<>(newShops);
        favorites = new HashSet<>(newFavorites);
    }

    public static void setBalance(double newBalance) {
        balance = newBalance;
    }

    public static double getBalance() {
        return balance;
    }

    public static List<SyncShopsPacket.ShopData> getShops() {
        return shops;
    }

    public static List<SyncShopsPacket.ShopData> getFilteredShops() {
        return shops.stream()
            .filter(shop -> {
                // Category filter
                if (filterCategory != null && filterCategory != ShopCategory.GENERAL) {
                    if (shop.category() != filterCategory) return false;
                }
                // Search filter
                if (!searchQuery.isEmpty()) {
                    String query = searchQuery.toLowerCase();
                    boolean matchesShop = shop.shopName().toLowerCase().contains(query) ||
                        shop.ownerName().toLowerCase().contains(query) ||
                        shop.description().toLowerCase().contains(query);
                    boolean matchesItem = shop.items().stream()
                        .anyMatch(item -> item.itemStack().getDisplayName().getString().toLowerCase().contains(query));
                    if (!matchesShop && !matchesItem) return false;
                }
                return true;
            })
            .sorted(getComparator())
            .collect(Collectors.toList());
    }

    private static Comparator<SyncShopsPacket.ShopData> getComparator() {
        return switch (sortType) {
            case NEWEST -> Comparator.comparingLong(SyncShopsPacket.ShopData::totalSales).reversed(); // Approximation
            case MOST_SALES -> Comparator.comparingLong(SyncShopsPacket.ShopData::totalSales).reversed();
            case ALPHABETICAL -> Comparator.comparing(SyncShopsPacket.ShopData::shopName, String.CASE_INSENSITIVE_ORDER);
            case FAVORITES_FIRST -> Comparator.comparing((SyncShopsPacket.ShopData shop) -> favorites.contains(shop.shopId()) ? 0 : 1)
                .thenComparing(SyncShopsPacket.ShopData::totalSales, Comparator.reverseOrder());
            case FEATURED_FIRST -> Comparator.comparing(SyncShopsPacket.ShopData::featured).reversed()
                .thenComparing(SyncShopsPacket.ShopData::totalSales, Comparator.reverseOrder());
        };
    }

    public static boolean isFavorite(UUID shopId) {
        return favorites.contains(shopId);
    }

    public static void setSearchQuery(String query) {
        searchQuery = query;
    }

    public static String getSearchQuery() {
        return searchQuery;
    }

    public static void setFilterCategory(ShopCategory category) {
        filterCategory = category;
    }

    public static ShopCategory getFilterCategory() {
        return filterCategory;
    }

    public static void setSortType(SortType type) {
        sortType = type;
    }

    public static SortType getSortType() {
        return sortType;
    }

    public static SyncShopsPacket.ShopData getShop(UUID shopId) {
        return shops.stream()
            .filter(shop -> shop.shopId().equals(shopId))
            .findFirst()
            .orElse(null);
    }

    public static List<SyncShopsPacket.ShopData> getMyShops(UUID playerUUID) {
        return shops.stream()
            .filter(shop -> shop.ownerUUID().equals(playerUUID))
            .collect(Collectors.toList());
    }

    public static void clear() {
        shops.clear();
        favorites.clear();
        balance = 0;
        searchQuery = "";
        filterCategory = null;
        sortType = SortType.FEATURED_FIRST;
    }

    public enum SortType {
        FEATURED_FIRST("Featured"),
        FAVORITES_FIRST("Favorites"),
        MOST_SALES("Most Sales"),
        NEWEST("Newest"),
        ALPHABETICAL("A-Z");

        private final String displayName;

        SortType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public SortType next() {
            SortType[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
