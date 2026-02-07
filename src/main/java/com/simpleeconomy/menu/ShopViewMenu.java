package com.simpleeconomy.menu;

import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopItem;
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
import java.util.UUID;

/**
 * Menu for viewing and buying from a shop
 */
public class ShopViewMenu extends ChestMenu {
    private final Player player;
    private final SimpleContainer container;
    private Shop shop;
    private UUID shopId;
    private int page = 0;
    private static final int ROWS = 6;
    private static final int ITEMS_PER_PAGE = 45;

    public ShopViewMenu(int containerId, Inventory playerInventory, UUID shopId) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), ROWS);
        this.player = playerInventory.player;
        this.shopId = shopId;
        this.container = (SimpleContainer) this.getContainer();

        loadShop();
        refreshDisplay();
    }

    private void loadShop() {
        if (player instanceof ServerPlayer serverPlayer) {
            ShopSavedData data = ShopSavedData.get(serverPlayer.getServer());
            shop = data.getShop(shopId);
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
            ItemStack error = new ItemStack(Items.BARRIER);
            error.set(DataComponents.CUSTOM_NAME, Component.literal("Shop not found!").withStyle(Style.EMPTY.withColor(0xFF5555)));
            container.setItem(22, error);
            return;
        }

        List<ShopItem> items = shop.getItems();
        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, items.size());

        // Display items
        for (int i = startIdx; i < endIdx; i++) {
            ShopItem shopItem = items.get(i);
            int slot = i - startIdx;
            container.setItem(slot, createItemIcon(shopItem));
        }

        // Navigation row
        int navRow = 45;

        // Back button
        ItemStack backBtn = new ItemStack(Items.ARROW);
        backBtn.set(DataComponents.CUSTOM_NAME, Component.literal("< Back to Browser").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
        container.setItem(navRow, backBtn);

        // Shop info
        ItemStack info = new ItemStack(Items.NAME_TAG);
        String featuredPrefix = shop.isFeatured() ? "\u2605 " : "";
        info.set(DataComponents.CUSTOM_NAME, Component.literal(featuredPrefix + shop.getShopName())
            .withStyle(Style.EMPTY.withColor(shop.isFeatured() ? 0xFFD700 : 0xFFFFFF).withItalic(false)));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Owner: " + shop.getOwnerName()).withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
        lore.add(Component.literal("Your balance: " + EconomyManager.formatBalance(getPlayerBalance()) + " " + ModConfig.getCurrencyName()).withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false)));
        if (!shop.getDescription().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.literal(shop.getDescription()).withStyle(Style.EMPTY.withColor(0x777777).withItalic(true)));
        }
        info.set(DataComponents.LORE, new ItemLore(lore));
        container.setItem(navRow + 4, info);

        // Page navigation
        int maxPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        if (page > 0) {
            ItemStack prevBtn = new ItemStack(Items.SPECTRAL_ARROW);
            prevBtn.set(DataComponents.CUSTOM_NAME, Component.literal("< Previous").withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
            container.setItem(navRow + 2, prevBtn);
        }
        if ((page + 1) * ITEMS_PER_PAGE < items.size()) {
            ItemStack nextBtn = new ItemStack(Items.SPECTRAL_ARROW);
            nextBtn.set(DataComponents.CUSTOM_NAME, Component.literal("Next >").withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
            container.setItem(navRow + 6, nextBtn);
        }

        // Close button
        ItemStack closeBtn = new ItemStack(Items.BARRIER);
        closeBtn.set(DataComponents.CUSTOM_NAME, Component.literal("Close").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        container.setItem(navRow + 8, closeBtn);
    }

    private ItemStack createItemIcon(ShopItem shopItem) {
        ItemStack icon = shopItem.getItemStack().copy();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Price: " + EconomyManager.formatBalance(shopItem.getPrice()) + " " + ModConfig.getCurrencyName() + " each")
            .withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false)));

        String stockText = shopItem.isInfiniteStock() ? "\u221E" : String.valueOf(shopItem.getStock());
        lore.add(Component.literal("Stock: " + stockText)
            .withStyle(Style.EMPTY.withColor(shopItem.isInStock() ? 0x55FF55 : 0xFF5555).withItalic(false)));

        // Check if player can afford
        double balance = getPlayerBalance();
        if (balance < shopItem.getPrice()) {
            lore.add(Component.empty());
            lore.add(Component.literal("Cannot afford!").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        } else if (!shopItem.isInStock()) {
            lore.add(Component.empty());
            lore.add(Component.literal("Out of stock!").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        } else {
            lore.add(Component.empty());
            lore.add(Component.literal("Left-click: Buy 1").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false)));
            lore.add(Component.literal("Right-click: Buy stack").withStyle(Style.EMPTY.withColor(0x55FFFF).withItalic(false)));
        }

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

        // Back button
        if (slotId == navRow) {
            serverPlayer.closeContainer();
            serverPlayer.openMenu(new ShopBrowserMenu.Provider());
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

        // Close
        if (slotId == navRow + 8) {
            serverPlayer.closeContainer();
            return;
        }

        // Item purchase - open confirmation menu
        if (shop == null) {
            serverPlayer.containerMenu.sendAllDataToRemote();
            return;
        }
        int itemIdx = page * ITEMS_PER_PAGE + slotId;
        if (slotId < navRow && itemIdx < shop.getItems().size()) {
            ShopItem shopItem = shop.getItems().get(itemIdx);
            if (!shopItem.isInStock()) {
                serverPlayer.sendSystemMessage(Component.literal("Out of stock!").withStyle(s -> s.withColor(0xFF5555)));
                serverPlayer.containerMenu.sendAllDataToRemote();
                return;
            }
            int initialQuantity;
            if (button == 1) {
                // Right-click: default to stack
                int maxStackSize = shopItem.getItemStack().getMaxStackSize();
                initialQuantity = shopItem.isInfiniteStock() ? maxStackSize : Math.min(shopItem.getStock(), maxStackSize);
            } else {
                initialQuantity = 1;
            }
            serverPlayer.closeContainer();
            serverPlayer.openMenu(new ConfirmBuyMenu.Provider(shopId, itemIdx, initialQuantity));
            return;
        }

        // For any other click, resync
        serverPlayer.containerMenu.sendAllDataToRemote();
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
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.getInventory().setChanged();
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }

    public static class Provider implements net.minecraft.world.MenuProvider {
        private final UUID shopId;

        public Provider(UUID shopId) {
            this.shopId = shopId;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Shop");
        }

        @Override
        public ShopViewMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ShopViewMenu(containerId, playerInventory, shopId);
        }
    }
}
