package com.simpleeconomy.menu;

import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.util.HeadUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Server-side shop browser menu using vanilla chest rendering
 */
public class ShopBrowserMenu extends ChestMenu {
    private final Player player;
    private final SimpleContainer container;
    private List<Shop> shops = new ArrayList<>();
    private int page = 0;
    private static final int ROWS = 6;
    private static final int ITEMS_PER_PAGE = 45;

    public ShopBrowserMenu(int containerId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), ROWS);
        this.player = playerInventory.player;
        this.container = (SimpleContainer) this.getContainer();

        loadShops();
        refreshDisplay();
    }

    private void loadShops() {
        if (player instanceof ServerPlayer serverPlayer) {
            ShopSavedData data = ShopSavedData.get(serverPlayer.getServer());
            shops = new ArrayList<>(data.getAllShops());
            shops.sort(Comparator
                .comparing(Shop::isFeatured).reversed()
                .thenComparing(Comparator.comparingLong(Shop::getTotalSales).reversed()));
        }
    }

    private void refreshDisplay() {
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, glass.copy());
        }

        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, shops.size());

        for (int i = startIdx; i < endIdx; i++) {
            Shop shop = shops.get(i);
            int slot = i - startIdx;
            container.setItem(slot, createShopIcon(shop));
        }

        int navRow = 45;

        if (page > 0) {
            ItemStack prevBtn = new ItemStack(Items.ARROW);
            prevBtn.set(DataComponents.CUSTOM_NAME, Component.literal("< Previous Page").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
            container.setItem(navRow, prevBtn);
        }

        ItemStack info = new ItemStack(Items.BOOK);
        int maxPages = Math.max(1, (int) Math.ceil((double) shops.size() / ITEMS_PER_PAGE));
        info.set(DataComponents.CUSTOM_NAME, Component.literal("Page " + (page + 1) + "/" + maxPages).withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Total shops: " + shops.size()).withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        lore.add(Component.literal("Your balance: " + EconomyManager.formatBalance(getPlayerBalance()) + " " + ModConfig.getCurrencyName()).withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false)));
        info.set(DataComponents.LORE, new ItemLore(lore));
        container.setItem(navRow + 4, info);

        ItemStack myShop = new ItemStack(Items.CHEST);
        myShop.set(DataComponents.CUSTOM_NAME, Component.literal("My Shop").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false)));
        List<Component> myShopLore = new ArrayList<>();
        myShopLore.add(Component.literal("Click to manage your shop").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        myShop.set(DataComponents.LORE, new ItemLore(myShopLore));
        container.setItem(navRow + 6, myShop);

        if ((page + 1) * ITEMS_PER_PAGE < shops.size()) {
            ItemStack nextBtn = new ItemStack(Items.ARROW);
            nextBtn.set(DataComponents.CUSTOM_NAME, Component.literal("Next Page >").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
            container.setItem(navRow + 8, nextBtn);
        } else {
            ItemStack closeBtn = new ItemStack(Items.BARRIER);
            closeBtn.set(DataComponents.CUSTOM_NAME, Component.literal("Close").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
            container.setItem(navRow + 8, closeBtn);
        }
    }

    private ItemStack createShopIcon(Shop shop) {
        ItemStack icon;
        if (player instanceof ServerPlayer serverPlayer) {
            icon = HeadUtil.createPlayerHead(shop.getOwnerUUID(), shop.getOwnerName(), serverPlayer.getServer());
        } else {
            icon = HeadUtil.createPlayerHead(shop.getOwnerUUID(), shop.getOwnerName());
        }

        String namePrefix = shop.isFeatured() ? "\u2605 " : "";
        int nameColor = shop.isFeatured() ? 0xFFD700 : 0xFFFFFF;
        icon.set(DataComponents.CUSTOM_NAME, Component.literal(namePrefix + shop.getShopName())
            .withStyle(Style.EMPTY.withColor(nameColor).withItalic(false)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Owner: " + shop.getOwnerName()).withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
        lore.add(Component.literal("Items: " + shop.getItems().size()).withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        lore.add(Component.literal("Sales: " + shop.getTotalSales()).withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        lore.add(Component.literal("Revenue: " + EconomyManager.formatBalance(shop.getTotalRevenue()) + " " + ModConfig.getCurrencyName()).withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false)));
        if (!shop.getDescription().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.literal(shop.getDescription()).withStyle(Style.EMPTY.withColor(0x777777).withItalic(true)));
        }
        lore.add(Component.empty());
        lore.add(Component.literal("Click to view shop").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
        icon.set(DataComponents.LORE, new ItemLore(lore));

        return icon;
    }

    private double getPlayerBalance() {
        if (player instanceof ServerPlayer serverPlayer) {
            return EconomyManager.getBalance(serverPlayer);
        }
        return 0;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        setCarried(ItemStack.EMPTY);

        if (slotId < 0 || slotId >= 54) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        int navRow = 45;

        if (slotId == navRow && page > 0) {
            page--;
            refreshDisplay();
            serverPlayer.containerMenu.sendAllDataToRemote();
            return;
        }

        if (slotId == navRow + 8) {
            int maxPages = Math.max(1, (int) Math.ceil((double) shops.size() / ITEMS_PER_PAGE));
            if ((page + 1) * ITEMS_PER_PAGE < shops.size()) {
                page++;
                refreshDisplay();
                serverPlayer.containerMenu.sendAllDataToRemote();
            } else {
                serverPlayer.closeContainer();
            }
            return;
        }

        if (slotId == navRow + 6) {
            serverPlayer.closeContainer();
            serverPlayer.openMenu(new MyShopMenu.Provider());
            return;
        }

        int shopIdx = page * ITEMS_PER_PAGE + slotId;
        if (slotId < navRow && shopIdx < shops.size()) {
            Shop shop = shops.get(shopIdx);
            serverPlayer.closeContainer();
            serverPlayer.openMenu(new ShopViewMenu.Provider(shop.getShopId()));
            return;
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Force full inventory sync when menu is closed
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.getInventory().setChanged();
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }

    public static class Provider implements net.minecraft.world.MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.literal("Shop Browser");
        }

        @Override
        public ShopBrowserMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ShopBrowserMenu(containerId, playerInventory);
        }
    }
}
