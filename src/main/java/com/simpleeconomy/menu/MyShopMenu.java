package com.simpleeconomy.menu;

import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopItem;
import com.simpleeconomy.shop.ShopManager;
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
import java.util.List;

/**
 * Menu for managing your own shop
 */
public class MyShopMenu extends ChestMenu {
    private final Player player;
    private final SimpleContainer container;
    private Shop shop;
    private int page = 0;
    private static final int ROWS = 6;
    private static final int ITEMS_PER_PAGE = 36; // 4 rows for items

    public MyShopMenu(int containerId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), ROWS);
        this.player = playerInventory.player;
        this.container = (SimpleContainer) this.getContainer();

        loadShop();
        refreshDisplay();
    }

    private void loadShop() {
        if (player instanceof ServerPlayer serverPlayer) {
            ShopSavedData data = ShopSavedData.get(serverPlayer.getServer());
            List<Shop> shops = data.getShopsByOwner(player.getUUID());
            shop = shops.isEmpty() ? null : shops.get(0);
        }
    }

    private void refreshDisplay() {
        // Fill with glass panes
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, glass.copy());
        }

        if (shop == null) {
            // No shop - show create option
            ItemStack create = new ItemStack(Items.EMERALD);
            create.set(DataComponents.CUSTOM_NAME, Component.literal("Create Shop").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false)));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("Click to create your shop!").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
            lore.add(Component.literal("Use /shop create <name> instead").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
            create.set(DataComponents.LORE, new ItemLore(lore));
            container.setItem(22, create);

            // Back button
            ItemStack back = new ItemStack(Items.ARROW);
            back.set(DataComponents.CUSTOM_NAME, Component.literal("< Back").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
            container.setItem(45, back);
            return;
        }

        List<ShopItem> items = shop.getItems();
        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, items.size());

        // Display items in first 4 rows
        for (int i = startIdx; i < endIdx; i++) {
            ShopItem shopItem = items.get(i);
            int slot = i - startIdx;
            container.setItem(slot, createItemIcon(shopItem, i));
        }

        // Row 5: Actions
        int actionRow = 36;

        // Add item button
        ItemStack addItem = new ItemStack(Items.LIME_CONCRETE);
        addItem.set(DataComponents.CUSTOM_NAME, Component.literal("+ Add Item").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false).withBold(true)));
        List<Component> addLore = new ArrayList<>();
        addLore.add(Component.literal("Click to add a new item").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        addLore.add(Component.literal("to your shop").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        addItem.set(DataComponents.LORE, new ItemLore(addLore));
        container.setItem(actionRow + 1, addItem);

        // Shop stats
        ItemStack stats = new ItemStack(Items.BOOK);
        stats.set(DataComponents.CUSTOM_NAME, Component.literal(shop.getShopName()).withStyle(Style.EMPTY.withColor(0xFFFFFF).withItalic(false)));
        List<Component> statsLore = new ArrayList<>();
        statsLore.add(Component.literal("Items: " + items.size() + "/" + ShopManager.getMaxItemsPerShop()).withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        statsLore.add(Component.literal("Total Sales: " + shop.getTotalSales()).withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false)));
        statsLore.add(Component.literal("Revenue: " + EconomyManager.formatBalance(shop.getTotalRevenue()) + " " + ModConfig.getCurrencyName()).withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false)));
        if (shop.isFeatured()) {
            statsLore.add(Component.literal("\u2605 FEATURED").withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false)));
        }
        stats.set(DataComponents.LORE, new ItemLore(statsLore));
        container.setItem(actionRow + 4, stats);

        // Row 6: Navigation
        int navRow = 45;

        // Back button
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("< Back to Browser").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
        container.setItem(navRow, back);

        // Page navigation
        int maxPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        if (page > 0) {
            ItemStack prev = new ItemStack(Items.SPECTRAL_ARROW);
            prev.set(DataComponents.CUSTOM_NAME, Component.literal("< Page " + page).withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
            container.setItem(navRow + 2, prev);
        }

        ItemStack pageInfo = new ItemStack(Items.PAPER);
        pageInfo.set(DataComponents.CUSTOM_NAME, Component.literal("Page " + (page + 1) + "/" + maxPages).withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
        container.setItem(navRow + 4, pageInfo);

        if ((page + 1) * ITEMS_PER_PAGE < items.size()) {
            ItemStack next = new ItemStack(Items.SPECTRAL_ARROW);
            next.set(DataComponents.CUSTOM_NAME, Component.literal("Page " + (page + 2) + " >").withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
            container.setItem(navRow + 6, next);
        }

        // Close
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponents.CUSTOM_NAME, Component.literal("Close").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        container.setItem(navRow + 8, close);
    }

    private ItemStack createItemIcon(ShopItem shopItem, int index) {
        ItemStack icon = shopItem.getItemStack().copy();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Price: " + EconomyManager.formatBalance(shopItem.getPrice()) + " " + ModConfig.getCurrencyName() + " each")
            .withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false)));

        if (shopItem.isInfiniteStock()) {
            lore.add(Component.literal("Stock: \u221E (infinite)")
                .withStyle(Style.EMPTY.withColor(0x55FFFF).withItalic(false)));
        } else {
            lore.add(Component.literal("Stock: " + shopItem.getStock())
                .withStyle(Style.EMPTY.withColor(shopItem.getStock() > 5 ? 0x55FF55 : (shopItem.getStock() > 0 ? 0xFFAA00 : 0xFF5555)).withItalic(false)));
        }

        lore.add(Component.empty());
        if (!shopItem.isInfiniteStock()) {
            lore.add(Component.literal("Left-click: Remove 1 from stock").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
            lore.add(Component.literal("Right-click: Restock from inventory").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false)));
        }
        lore.add(Component.literal("Shift+click: Remove listing").withStyle(Style.EMPTY.withColor(0xFFAA00).withItalic(false)));

        icon.set(DataComponents.LORE, new ItemLore(lore));
        return icon;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Always clear carried item to prevent desync
        setCarried(ItemStack.EMPTY);

        if (slotId < 0 || slotId >= 54) {
            if (player instanceof ServerPlayer sp) {
                sp.containerMenu.sendAllDataToRemote();
            }
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        int navRow = 45;
        int actionRow = 36;

        // Back button
        if (slotId == navRow) {
            serverPlayer.closeContainer();
            serverPlayer.openMenu(new ShopBrowserMenu.Provider());
            return;
        }

        // Close
        if (slotId == navRow + 8) {
            serverPlayer.closeContainer();
            return;
        }

        // Page navigation
        if (slotId == navRow + 2 && page > 0) {
            page--;
            refreshDisplay();
            serverPlayer.containerMenu.sendAllDataToRemote();
            return;
        }
        if (slotId == navRow + 6 && shop != null) {
            int maxPages = Math.max(1, (int) Math.ceil((double) shop.getItems().size() / ITEMS_PER_PAGE));
            if ((page + 1) * ITEMS_PER_PAGE < shop.getItems().size()) {
                page++;
                refreshDisplay();
                serverPlayer.containerMenu.sendAllDataToRemote();
            }
            return;
        }

        // No shop yet - info only
        if (shop == null) {
            if (slotId == 22) {
                serverPlayer.sendSystemMessage(Component.literal("Use /shop create <name> to create your shop!")
                    .withStyle(s -> s.withColor(0xFFFF55)));
            }
            serverPlayer.containerMenu.sendAllDataToRemote();
            return;
        }

        // Add item button (row 5, slot 1 = 37)
        if (slotId == 37) {
            serverPlayer.closeContainer();
            serverPlayer.openMenu(new AddItemMenu.Provider());
            return;
        }

        // Item operations (first 4 rows)
        if (slotId < ITEMS_PER_PAGE) {
            int itemIdx = page * ITEMS_PER_PAGE + slotId;
            if (itemIdx < shop.getItems().size()) {
                ShopItem shopItem = shop.getItems().get(itemIdx);

                if (clickType == ClickType.QUICK_MOVE) {
                    // Shift-click: Open remove confirmation
                    serverPlayer.closeContainer();
                    serverPlayer.openMenu(new ConfirmRemoveMenu.Provider(shop.getShopId(), itemIdx));
                } else if (button == 0) {
                    // Left-click: Take 1 from stock back to inventory
                    takeFromStock(serverPlayer, shopItem, 1);
                } else if (button == 1) {
                    // Right-click: Restock from inventory
                    restockItem(serverPlayer, shopItem);
                }
                return;
            }
        }

        // For any other click, resync
        serverPlayer.containerMenu.sendAllDataToRemote();
    }

    private void takeFromStock(ServerPlayer player, ShopItem shopItem, int amount) {
        if (shopItem.isInfiniteStock()) {
            player.sendSystemMessage(Component.literal("Cannot take from infinite stock!").withStyle(s -> s.withColor(0xFF5555)));
            return;
        }
        if (shopItem.getStock() < amount) {
            player.sendSystemMessage(Component.literal("No stock to take!").withStyle(s -> s.withColor(0xFF5555)));
            return;
        }

        ItemStack toGive = shopItem.getItemStack().copy();
        toGive.setCount(amount);

        if (player.getInventory().add(toGive)) {
            shopItem.setStock(shopItem.getStock() - amount);
            ShopSavedData.get(player.getServer()).setDirty();
            player.sendSystemMessage(Component.literal("Took " + amount + "x from stock.").withStyle(s -> s.withColor(0x55FF55)));
            refreshDisplay();
            player.containerMenu.sendAllDataToRemote();
        } else {
            player.sendSystemMessage(Component.literal("Inventory full!").withStyle(s -> s.withColor(0xFF5555)));
        }
    }

    private void restockItem(ServerPlayer player, ShopItem shopItem) {
        if (shopItem.isInfiniteStock()) {
            player.sendSystemMessage(Component.literal("Item already has infinite stock!").withStyle(s -> s.withColor(0xFF5555)));
            return;
        }
        ItemStack targetStack = shopItem.getItemStack();
        int added = 0;

        // Find matching items in player inventory and add to stock
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (!invStack.isEmpty() && ItemStack.isSameItemSameComponents(invStack, targetStack)) {
                int toAdd = invStack.getCount();
                shopItem.setStock(shopItem.getStock() + toAdd);
                added += toAdd;
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        if (added > 0) {
            ShopSavedData.get(player.getServer()).setDirty();
            player.sendSystemMessage(Component.literal("Restocked " + added + " items!").withStyle(s -> s.withColor(0x55FF55)));
            refreshDisplay();
            player.containerMenu.sendAllDataToRemote();
        } else {
            player.sendSystemMessage(Component.literal("No matching items in inventory to restock!").withStyle(s -> s.withColor(0xFF5555)));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.getInventory().setChanged();
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }

    public static class Provider implements net.minecraft.world.MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.literal("My Shop");
        }

        @Override
        public MyShopMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new MyShopMenu(containerId, playerInventory);
        }
    }
}
