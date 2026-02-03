package com.simpleeconomy.data;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopCategory;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;
import java.util.stream.Collectors;

public class ShopSavedData extends SavedData {

    private static final String DATA_NAME = SimpleEconomy.MOD_ID + "_shops";

    private final Map<UUID, Shop> shops = new HashMap<>();
    private final Map<UUID, Set<UUID>> favoriteShops = new HashMap<>(); // playerUUID -> set of shopIds

    public ShopSavedData() {
    }

    public static ShopSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ShopSavedData data = new ShopSavedData();

        ListTag shopsTag = tag.getList("shops", Tag.TAG_COMPOUND);
        for (int i = 0; i < shopsTag.size(); i++) {
            try {
                Shop shop = Shop.load(shopsTag.getCompound(i), provider);
                data.shops.put(shop.getShopId(), shop);
            } catch (Exception e) {
                SimpleEconomy.LOGGER.warn("Failed to load shop: {}", e.getMessage());
            }
        }

        CompoundTag favoritesTag = tag.getCompound("favorites");
        for (String key : favoritesTag.getAllKeys()) {
            try {
                UUID playerUUID = UUID.fromString(key);
                Set<UUID> playerFavorites = new HashSet<>();
                ListTag favoritesList = favoritesTag.getList(key, Tag.TAG_COMPOUND);
                for (int i = 0; i < favoritesList.size(); i++) {
                    playerFavorites.add(favoritesList.getCompound(i).getUUID("shopId"));
                }
                data.favoriteShops.put(playerUUID, playerFavorites);
            } catch (IllegalArgumentException e) {
                SimpleEconomy.LOGGER.warn("Invalid UUID in favorites data: {}", key);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag shopsTag = new ListTag();
        for (Shop shop : shops.values()) {
            shopsTag.add(shop.save(provider));
        }
        tag.put("shops", shopsTag);

        CompoundTag favoritesTag = new CompoundTag();
        for (Map.Entry<UUID, Set<UUID>> entry : favoriteShops.entrySet()) {
            ListTag favoritesList = new ListTag();
            for (UUID shopId : entry.getValue()) {
                CompoundTag shopIdTag = new CompoundTag();
                shopIdTag.putUUID("shopId", shopId);
                favoritesList.add(shopIdTag);
            }
            favoritesTag.put(entry.getKey().toString(), favoritesList);
        }
        tag.put("favorites", favoritesTag);

        return tag;
    }

    public void addShop(Shop shop) {
        shops.put(shop.getShopId(), shop);
        setDirty();
    }

    public void removeShop(UUID shopId) {
        shops.remove(shopId);
        // Remove from all favorites
        for (Set<UUID> favorites : favoriteShops.values()) {
            favorites.remove(shopId);
        }
        setDirty();
    }

    public Shop getShop(UUID shopId) {
        return shops.get(shopId);
    }

    public List<Shop> getAllShops() {
        return new ArrayList<>(shops.values());
    }

    public List<Shop> getShopsByOwner(UUID ownerUUID) {
        return shops.values().stream()
            .filter(shop -> shop.getOwnerUUID().equals(ownerUUID))
            .collect(Collectors.toList());
    }

    public List<Shop> getShopsByCategory(ShopCategory category) {
        if (category == ShopCategory.GENERAL) {
            return getAllShops();
        }
        return shops.values().stream()
            .filter(shop -> shop.getCategory() == category)
            .collect(Collectors.toList());
    }

    public List<Shop> getFeaturedShops() {
        return shops.values().stream()
            .filter(Shop::isFeatured)
            .collect(Collectors.toList());
    }

    public List<Shop> searchShops(String query) {
        String lowerQuery = query.toLowerCase();
        return shops.values().stream()
            .filter(shop ->
                shop.getShopName().toLowerCase().contains(lowerQuery) ||
                shop.getOwnerName().toLowerCase().contains(lowerQuery) ||
                shop.getDescription().toLowerCase().contains(lowerQuery)
            )
            .collect(Collectors.toList());
    }

    public void addFavorite(UUID playerUUID, UUID shopId) {
        favoriteShops.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(shopId);
        setDirty();
    }

    public void removeFavorite(UUID playerUUID, UUID shopId) {
        Set<UUID> favorites = favoriteShops.get(playerUUID);
        if (favorites != null) {
            favorites.remove(shopId);
            setDirty();
        }
    }

    public boolean isFavorite(UUID playerUUID, UUID shopId) {
        Set<UUID> favorites = favoriteShops.get(playerUUID);
        return favorites != null && favorites.contains(shopId);
    }

    public List<Shop> getFavoriteShops(UUID playerUUID) {
        Set<UUID> favorites = favoriteShops.get(playerUUID);
        if (favorites == null) {
            return new ArrayList<>();
        }
        return favorites.stream()
            .map(shops::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static ShopSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            new Factory<>(ShopSavedData::new, ShopSavedData::load),
            DATA_NAME
        );
    }
}
