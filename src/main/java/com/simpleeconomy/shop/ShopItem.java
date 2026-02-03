package com.simpleeconomy.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class ShopItem {

    private final UUID itemId;
    private ItemStack itemStack;
    private double price;
    private int stock;
    private int maxStock;
    private long totalSold;

    public ShopItem(ItemStack itemStack, double price, int stock) {
        this.itemId = UUID.randomUUID();
        this.itemStack = itemStack.copy();
        this.price = price;
        this.stock = stock;
        this.maxStock = stock;
        this.totalSold = 0;
    }

    private ShopItem(UUID itemId) {
        this.itemId = itemId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = Math.max(0, price);
    }

    public double getPricePerItem() {
        int count = itemStack.getCount();
        return count > 0 ? price / count : price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, stock);
    }

    public void addStock(int amount) {
        this.stock += amount;
        this.maxStock = Math.max(this.maxStock, this.stock);
    }

    public boolean removeStock(int amount) {
        if (stock >= amount) {
            stock -= amount;
            totalSold += amount;
            return true;
        }
        return false;
    }

    public int getMaxStock() {
        return maxStock;
    }

    public long getTotalSold() {
        return totalSold;
    }

    public boolean isInStock() {
        return stock > 0;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("itemId", itemId);
        tag.put("itemStack", itemStack.save(provider));
        tag.putDouble("price", price);
        tag.putInt("stock", stock);
        tag.putInt("maxStock", maxStock);
        tag.putLong("totalSold", totalSold);
        return tag;
    }

    public static ShopItem load(CompoundTag tag, HolderLookup.Provider provider) {
        ShopItem item = new ShopItem(tag.getUUID("itemId"));

        CompoundTag itemStackTag = tag.getCompound("itemStack");
        item.itemStack = ItemStack.parse(provider, itemStackTag).orElse(ItemStack.EMPTY);

        if (item.itemStack.isEmpty()) {
            return null;
        }

        item.price = tag.getDouble("price");
        item.stock = tag.getInt("stock");
        item.maxStock = tag.getInt("maxStock");
        item.totalSold = tag.getLong("totalSold");
        return item;
    }
}
