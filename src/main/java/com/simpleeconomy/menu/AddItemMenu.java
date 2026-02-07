package com.simpleeconomy.menu;

import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.shop.Shop;
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
 * Menu for adding items to your shop with price selection
 */
public class AddItemMenu extends ChestMenu {
    private final Player player;
    private final SimpleContainer container;
    private Shop shop;
    private double selectedPrice = 10.0;
    private ItemStack itemToSell = ItemStack.EMPTY;
    private static final int ITEM_SLOT = 22; // Center slot for the item to sell

    // Price button slots
    private static final int PRICE_1 = 10;
    private static final int PRICE_5 = 11;
    private static final int PRICE_10 = 12;
    private static final int PRICE_50 = 14;
    private static final int PRICE_100 = 15;
    private static final int PRICE_500 = 16;

    private static final int PRICE_MINUS_10 = 28;
    private static final int PRICE_MINUS_1 = 29;
    private static final int PRICE_PLUS_1 = 33;
    private static final int PRICE_PLUS_10 = 34;

    private static final int CONFIRM_SLOT = 40;
    private static final int CANCEL_SLOT = 36;
    private static final int BACK_SLOT = 44;

    public AddItemMenu(int containerId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), 6);
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
        // Fill with glass panes first
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 54; i++) {
            container.setItem(i, glass.copy());
        }

        // Item slot - show item or placeholder
        if (itemToSell.isEmpty()) {
            ItemStack placeholder = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
            placeholder.set(DataComponents.CUSTOM_NAME, Component.literal("Place Item Here").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("Click with an item from").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
            lore.add(Component.literal("your inventory to add it").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
            placeholder.set(DataComponents.LORE, new ItemLore(lore));
            container.setItem(ITEM_SLOT, placeholder);
        } else {
            ItemStack display = itemToSell.copy();
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("Click to remove").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
            display.set(DataComponents.LORE, new ItemLore(lore));
            container.setItem(ITEM_SLOT, display);
        }

        // Price preset buttons (top row)
        container.setItem(PRICE_1, createPriceButton(1, Items.IRON_NUGGET));
        container.setItem(PRICE_5, createPriceButton(5, Items.IRON_INGOT));
        container.setItem(PRICE_10, createPriceButton(10, Items.GOLD_NUGGET));
        container.setItem(PRICE_50, createPriceButton(50, Items.GOLD_INGOT));
        container.setItem(PRICE_100, createPriceButton(100, Items.DIAMOND));
        container.setItem(PRICE_500, createPriceButton(500, Items.EMERALD));

        // Current price display
        ItemStack priceDisplay = new ItemStack(Items.SUNFLOWER);
        priceDisplay.set(DataComponents.CUSTOM_NAME, Component.literal("Price: " + EconomyManager.formatBalance(selectedPrice) + " " + ModConfig.getCurrencyName() + " each")
            .withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false).withBold(true)));
        List<Component> priceLore = new ArrayList<>();
        priceLore.add(Component.literal("Price per single item").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        priceLore.add(Component.literal("Use buttons to adjust").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        priceDisplay.set(DataComponents.LORE, new ItemLore(priceLore));
        container.setItem(31, priceDisplay);

        // Price adjustment buttons
        container.setItem(PRICE_MINUS_10, createAdjustButton(-10, "-10"));
        container.setItem(PRICE_MINUS_1, createAdjustButton(-1, "-1"));
        container.setItem(PRICE_PLUS_1, createAdjustButton(1, "+1"));
        container.setItem(PRICE_PLUS_10, createAdjustButton(10, "+10"));

        // Confirm button
        ItemStack confirm = new ItemStack(Items.LIME_CONCRETE);
        confirm.set(DataComponents.CUSTOM_NAME, Component.literal("Confirm - Add Item").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false).withBold(true)));
        List<Component> confirmLore = new ArrayList<>();
        if (itemToSell.isEmpty()) {
            confirmLore.add(Component.literal("Place an item first!").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        } else {
            confirmLore.add(Component.literal("Stock: " + itemToSell.getCount() + "x " + itemToSell.getHoverName().getString()).withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
            confirmLore.add(Component.literal("Price: " + EconomyManager.formatBalance(selectedPrice) + " " + ModConfig.getCurrencyName() + " each").withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false)));
        }
        confirm.set(DataComponents.LORE, new ItemLore(confirmLore));
        container.setItem(CONFIRM_SLOT, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Items.RED_CONCRETE);
        cancel.set(DataComponents.CUSTOM_NAME, Component.literal("Cancel").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        container.setItem(CANCEL_SLOT, cancel);

        // Back button
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("< Back").withStyle(Style.EMPTY.withColor(0xFFFF55).withItalic(false)));
        container.setItem(BACK_SLOT, back);
    }

    private ItemStack createPriceButton(double price, net.minecraft.world.item.Item item) {
        ItemStack button = new ItemStack(item);
        boolean selected = Math.abs(selectedPrice - price) < 0.01;
        int color = selected ? 0x55FF55 : 0xFFD700;
        button.set(DataComponents.CUSTOM_NAME, Component.literal((selected ? "> " : "") + EconomyManager.formatBalance(price) + " " + ModConfig.getCurrencyName())
            .withStyle(Style.EMPTY.withColor(color).withItalic(false)));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Click to set price").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        button.set(DataComponents.LORE, new ItemLore(lore));
        return button;
    }

    private ItemStack createAdjustButton(double adjustment, String label) {
        ItemStack button = new ItemStack(adjustment > 0 ? Items.LIME_DYE : Items.RED_DYE);
        button.set(DataComponents.CUSTOM_NAME, Component.literal(label)
            .withStyle(Style.EMPTY.withColor(adjustment > 0 ? 0x55FF55 : 0xFF5555).withItalic(false)));
        return button;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Always clear carried item to prevent desync
        setCarried(ItemStack.EMPTY);

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Handle player inventory clicks - allow normal interaction
        if (slotId >= 54) {
            // Get the item from player inventory
            int invSlot = slotId - 54;
            if (invSlot < 27) {
                invSlot += 9; // Main inventory
            } else {
                invSlot -= 27; // Hotbar
            }

            ItemStack clickedStack = player.getInventory().getItem(invSlot);

            if (!clickedStack.isEmpty() && itemToSell.isEmpty()) {
                // Pick up item to add to shop
                itemToSell = clickedStack.copy();
                player.getInventory().setItem(invSlot, ItemStack.EMPTY);
                refreshDisplay();
                serverPlayer.containerMenu.sendAllDataToRemote();
            }
            return;
        }

        // Item slot - click to remove item back to inventory
        if (slotId == ITEM_SLOT) {
            if (!itemToSell.isEmpty()) {
                if (player.getInventory().add(itemToSell)) {
                    itemToSell = ItemStack.EMPTY;
                    refreshDisplay();
                    serverPlayer.containerMenu.sendAllDataToRemote();
                }
            }
            return;
        }

        // Price preset buttons
        if (slotId == PRICE_1) { selectedPrice = 1; refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }
        if (slotId == PRICE_5) { selectedPrice = 5; refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }
        if (slotId == PRICE_10) { selectedPrice = 10; refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }
        if (slotId == PRICE_50) { selectedPrice = 50; refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }
        if (slotId == PRICE_100) { selectedPrice = 100; refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }
        if (slotId == PRICE_500) { selectedPrice = 500; refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }

        // Price adjustment buttons
        if (slotId == PRICE_MINUS_10) { selectedPrice = Math.max(0.01, selectedPrice - 10); refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }
        if (slotId == PRICE_MINUS_1) { selectedPrice = Math.max(0.01, selectedPrice - 1); refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }
        if (slotId == PRICE_PLUS_1) { selectedPrice += 1; refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }
        if (slotId == PRICE_PLUS_10) { selectedPrice += 10; refreshDisplay(); serverPlayer.containerMenu.sendAllDataToRemote(); return; }

        // Confirm
        if (slotId == CONFIRM_SLOT) {
            addItemToShop(serverPlayer);
            return;
        }

        // Cancel - return item and go back
        if (slotId == CANCEL_SLOT || slotId == BACK_SLOT) {
            returnItemToPlayer(serverPlayer);
            serverPlayer.closeContainer();
            serverPlayer.openMenu(new MyShopMenu.Provider());
            return;
        }

        // For any other click, resync
        serverPlayer.containerMenu.sendAllDataToRemote();
    }

    private void addItemToShop(ServerPlayer player) {
        if (shop == null) {
            player.sendSystemMessage(Component.literal("You don't have a shop!").withStyle(s -> s.withColor(0xFF5555)));
            return;
        }

        if (itemToSell.isEmpty()) {
            player.sendSystemMessage(Component.literal("Place an item first!").withStyle(s -> s.withColor(0xFF5555)));
            return;
        }

        if (shop.getItems().size() >= ShopManager.getMaxItemsPerShop()) {
            player.sendSystemMessage(Component.literal("Your shop is full! Max " + ShopManager.getMaxItemsPerShop() + " items.")
                .withStyle(s -> s.withColor(0xFF5555)));
            returnItemToPlayer(player);
            return;
        }

        // Add to shop
        if (ShopManager.addItemToShop(player, shop.getShopId(), itemToSell.copy(), selectedPrice)) {
            player.sendSystemMessage(Component.literal("[INFO] ").withStyle(s -> s.withColor(0xFFFF55).withBold(true))
                .append(Component.literal("Added " + itemToSell.getCount() + "x " + itemToSell.getHoverName().getString() +
                    " at " + EconomyManager.formatBalance(selectedPrice) + " " + ModConfig.getCurrencyName() + " each!")
                    .withStyle(s -> s.withColor(0xFFFF55).withBold(false))));
            itemToSell = ItemStack.EMPTY;
            player.closeContainer();
            player.openMenu(new MyShopMenu.Provider());
        } else {
            player.sendSystemMessage(Component.literal("Failed to add item!").withStyle(s -> s.withColor(0xFF5555)));
            returnItemToPlayer(player);
        }
    }

    private void returnItemToPlayer(ServerPlayer player) {
        if (!itemToSell.isEmpty()) {
            if (!player.getInventory().add(itemToSell)) {
                player.drop(itemToSell, false);
            }
            itemToSell = ItemStack.EMPTY;
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            returnItemToPlayer(serverPlayer);
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.getInventory().setChanged();
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-click from player inventory adds item
        if (index >= 54 && itemToSell.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            int invSlot = index - 54;
            if (invSlot < 27) {
                invSlot += 9;
            } else {
                invSlot -= 27;
            }

            ItemStack stack = player.getInventory().getItem(invSlot);
            if (!stack.isEmpty()) {
                itemToSell = stack.copy();
                player.getInventory().setItem(invSlot, ItemStack.EMPTY);
                refreshDisplay();
                serverPlayer.containerMenu.sendAllDataToRemote();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    public static class Provider implements net.minecraft.world.MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.literal("Add Item to Shop");
        }

        @Override
        public AddItemMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new AddItemMenu(containerId, playerInventory);
        }
    }
}
