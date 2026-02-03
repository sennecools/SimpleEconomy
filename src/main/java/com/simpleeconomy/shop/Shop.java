package com.simpleeconomy.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Shop {

    private final UUID shopId;
    private UUID ownerUUID;
    private String ownerName;
    private String shopName;
    private String description;
    private final List<ShopItem> items;
    private long totalSales;
    private double totalRevenue;
    private long createdTime;
    private boolean featured;
    private ShopCategory category;

    public Shop(UUID ownerUUID, String ownerName, String shopName) {
        this.shopId = UUID.randomUUID();
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.shopName = shopName;
        this.description = "";
        this.items = new ArrayList<>();
        this.totalSales = 0;
        this.totalRevenue = 0;
        this.createdTime = System.currentTimeMillis();
        this.featured = false;
        this.category = ShopCategory.GENERAL;
    }

    private Shop(UUID shopId) {
        this.shopId = shopId;
        this.items = new ArrayList<>();
    }

    public UUID getShopId() {
        return shopId;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public void addItem(ShopItem item) {
        items.add(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void removeItem(UUID itemId) {
        items.removeIf(item -> item.getItemId().equals(itemId));
    }

    public ShopItem getItem(UUID itemId) {
        return items.stream()
            .filter(item -> item.getItemId().equals(itemId))
            .findFirst()
            .orElse(null);
    }

    public long getTotalSales() {
        return totalSales;
    }

    public void addSale(double amount) {
        this.totalSales++;
        this.totalRevenue += amount;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public void setCategory(ShopCategory category) {
        this.category = category;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("shopId", shopId);
        tag.putUUID("ownerUUID", ownerUUID);
        tag.putString("ownerName", ownerName);
        tag.putString("shopName", shopName);
        tag.putString("description", description);
        tag.putLong("totalSales", totalSales);
        tag.putDouble("totalRevenue", totalRevenue);
        tag.putLong("createdTime", createdTime);
        tag.putBoolean("featured", featured);
        tag.putString("category", category.name());

        ListTag itemsTag = new ListTag();
        for (ShopItem item : items) {
            itemsTag.add(item.save(provider));
        }
        tag.put("items", itemsTag);

        return tag;
    }

    public static Shop load(CompoundTag tag, HolderLookup.Provider provider) {
        Shop shop = new Shop(tag.getUUID("shopId"));
        shop.ownerUUID = tag.getUUID("ownerUUID");
        shop.ownerName = tag.getString("ownerName");
        shop.shopName = tag.getString("shopName");
        shop.description = tag.getString("description");
        shop.totalSales = tag.getLong("totalSales");
        shop.totalRevenue = tag.getDouble("totalRevenue");
        shop.createdTime = tag.getLong("createdTime");
        shop.featured = tag.getBoolean("featured");

        if (tag.contains("category")) {
            try {
                shop.category = ShopCategory.valueOf(tag.getString("category"));
            } catch (IllegalArgumentException e) {
                shop.category = ShopCategory.GENERAL;
            }
        }

        ListTag itemsTag = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); i++) {
            ShopItem item = ShopItem.load(itemsTag.getCompound(i), provider);
            if (item != null) {
                shop.items.add(item);
            }
        }

        return shop;
    }
}
