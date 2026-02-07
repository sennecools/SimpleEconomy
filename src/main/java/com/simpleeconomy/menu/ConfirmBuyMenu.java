package com.simpleeconomy.menu;

import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.PlayerDataSavedData;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.data.TransactionLog;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopItem;
import com.simpleeconomy.util.SoundHelper;
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
 * 1-row confirmation menu for buying items with quantity adjustment.
 * Layout: [Cancel] [glass] [-5] [-1] [Item Info] [+1] [+5] [glass] [Confirm]
 */
public class ConfirmBuyMenu extends ChestMenu {
    private final Player player;
    private final SimpleContainer container;
    private final UUID shopId;
    private final int itemIndex;
    private int quantity;

    private static final int CANCEL_SLOT = 0;
    private static final int MINUS_5_SLOT = 2;
    private static final int MINUS_1_SLOT = 3;
    private static final int INFO_SLOT = 4;
    private static final int PLUS_1_SLOT = 5;
    private static final int PLUS_5_SLOT = 6;
    private static final int CONFIRM_SLOT = 8;

    public ConfirmBuyMenu(int containerId, Inventory playerInventory, UUID shopId, int itemIndex, int initialQuantity) {
        super(MenuType.GENERIC_9x1, containerId, playerInventory, new SimpleContainer(9), 1);
        this.player = playerInventory.player;
        this.container = (SimpleContainer) this.getContainer();
        this.shopId = shopId;
        this.itemIndex = itemIndex;
        this.quantity = Math.max(1, initialQuantity);
        clampQuantity();
        refreshDisplay();
    }

    private Shop getShop() {
        if (player instanceof ServerPlayer sp) {
            return ShopSavedData.get(sp.getServer()).getShop(shopId);
        }
        return null;
    }

    private ShopItem getShopItem() {
        Shop shop = getShop();
        if (shop != null && itemIndex < shop.getItems().size()) {
            return shop.getItems().get(itemIndex);
        }
        return null;
    }

    private int getMaxQuantity() {
        ShopItem item = getShopItem();
        if (item == null) return 1;

        int maxStack = item.getItemStack().getMaxStackSize();
        int maxStock = item.isInfiniteStock() ? maxStack : Math.min(item.getStock(), maxStack);

        if (player instanceof ServerPlayer sp) {
            double balance = EconomyManager.getBalance(sp);
            int maxAfford = item.getPrice() > 0 ? (int) (balance / item.getPrice()) : maxStack;
            return Math.max(1, Math.min(maxStock, maxAfford));
        }
        return Math.max(1, maxStock);
    }

    private void clampQuantity() {
        quantity = Math.max(1, Math.min(quantity, getMaxQuantity()));
    }

    private void refreshDisplay() {
        // Fill with glass
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 9; i++) {
            container.setItem(i, glass.copy());
        }

        ShopItem shopItem = getShopItem();
        if (shopItem == null) {
            ItemStack error = new ItemStack(Items.BARRIER);
            error.set(DataComponents.CUSTOM_NAME, Component.literal("Item not available!").withStyle(Style.EMPTY.withColor(0xFF5555)));
            container.setItem(INFO_SLOT, error);
            return;
        }

        double totalCost = shopItem.getPrice() * quantity;
        String currency = ModConfig.getCurrencyName();

        // Cancel
        ItemStack cancel = new ItemStack(Items.RED_CONCRETE);
        cancel.set(DataComponents.CUSTOM_NAME, Component.literal("Cancel").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        container.setItem(CANCEL_SLOT, cancel);

        // -5
        ItemStack minus5 = new ItemStack(Items.RED_DYE);
        minus5.set(DataComponents.CUSTOM_NAME, Component.literal("-5").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        container.setItem(MINUS_5_SLOT, minus5);

        // -1
        ItemStack minus1 = new ItemStack(Items.RED_DYE);
        minus1.set(DataComponents.CUSTOM_NAME, Component.literal("-1").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        container.setItem(MINUS_1_SLOT, minus1);

        // Item info
        ItemStack info = shopItem.getItemStack().copy();
        info.setCount(Math.min(quantity, info.getMaxStackSize()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Buy " + quantity + "x for " + EconomyManager.formatBalance(totalCost) + " " + currency)
            .withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false).withBold(true)));
        lore.add(Component.literal("Price: " + EconomyManager.formatBalance(shopItem.getPrice()) + " " + currency + " each")
            .withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
        info.set(DataComponents.LORE, new ItemLore(lore));
        container.setItem(INFO_SLOT, info);

        // +1
        ItemStack plus1 = new ItemStack(Items.LIME_DYE);
        plus1.set(DataComponents.CUSTOM_NAME, Component.literal("+1").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false)));
        container.setItem(PLUS_1_SLOT, plus1);

        // +5
        ItemStack plus5 = new ItemStack(Items.LIME_DYE);
        plus5.set(DataComponents.CUSTOM_NAME, Component.literal("+5").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false)));
        container.setItem(PLUS_5_SLOT, plus5);

        // Confirm
        ItemStack confirm = new ItemStack(Items.LIME_CONCRETE);
        confirm.set(DataComponents.CUSTOM_NAME, Component.literal("Confirm Purchase").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false).withBold(true)));
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(Component.literal(quantity + "x " + shopItem.getItemStack().getHoverName().getString())
            .withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false)));
        confirmLore.add(Component.literal("Total: " + EconomyManager.formatBalance(totalCost) + " " + currency)
            .withStyle(Style.EMPTY.withColor(0xFFD700).withItalic(false)));
        confirm.set(DataComponents.LORE, new ItemLore(confirmLore));
        container.setItem(CONFIRM_SLOT, confirm);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        setCarried(ItemStack.EMPTY);

        if (slotId < 0 || slotId >= 9) {
            if (player instanceof ServerPlayer sp) {
                sp.containerMenu.sendAllDataToRemote();
            }
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        switch (slotId) {
            case CANCEL_SLOT -> {
                serverPlayer.closeContainer();
                serverPlayer.openMenu(new ShopViewMenu.Provider(shopId));
            }
            case MINUS_5_SLOT -> {
                quantity = Math.max(1, quantity - 5);
                clampQuantity();
                refreshDisplay();
                serverPlayer.containerMenu.sendAllDataToRemote();
            }
            case MINUS_1_SLOT -> {
                quantity = Math.max(1, quantity - 1);
                clampQuantity();
                refreshDisplay();
                serverPlayer.containerMenu.sendAllDataToRemote();
            }
            case PLUS_1_SLOT -> {
                quantity++;
                clampQuantity();
                refreshDisplay();
                serverPlayer.containerMenu.sendAllDataToRemote();
            }
            case PLUS_5_SLOT -> {
                quantity += 5;
                clampQuantity();
                refreshDisplay();
                serverPlayer.containerMenu.sendAllDataToRemote();
            }
            case CONFIRM_SLOT -> executePurchase(serverPlayer);
            default -> serverPlayer.containerMenu.sendAllDataToRemote();
        }
    }

    private void executePurchase(ServerPlayer buyer) {
        Shop shop = getShop();
        ShopItem shopItem = getShopItem();
        if (shop == null || shopItem == null) {
            buyer.sendSystemMessage(Component.literal("Item no longer available!").withStyle(s -> s.withColor(0xFF5555)));
            buyer.closeContainer();
            return;
        }

        if (!shopItem.isInfiniteStock() && shopItem.getStock() < quantity) {
            buyer.sendSystemMessage(Component.literal("Not enough stock!").withStyle(s -> s.withColor(0xFF5555)));
            buyer.closeContainer();
            buyer.openMenu(new ShopViewMenu.Provider(shopId));
            return;
        }

        double totalCost = shopItem.getPrice() * quantity;
        if (!EconomyManager.hasBalance(buyer, totalCost)) {
            buyer.sendSystemMessage(Component.literal("Not enough " + ModConfig.getCurrencyName() + "! Need " + EconomyManager.formatBalance(totalCost))
                .withStyle(s -> s.withColor(0xFF5555)));
            buyer.closeContainer();
            buyer.openMenu(new ShopViewMenu.Provider(shopId));
            return;
        }

        // Check inventory space
        ItemStack toGive = shopItem.getItemStack().copy();
        toGive.setCount(quantity);
        if (!buyer.getInventory().canPlaceItem(0, toGive) && buyer.getInventory().getFreeSlot() == -1) {
            buyer.sendSystemMessage(Component.literal("Your inventory is full!").withStyle(s -> s.withColor(0xFF5555)));
            buyer.closeContainer();
            buyer.openMenu(new ShopViewMenu.Provider(shopId));
            return;
        }

        // Process purchase
        ShopSavedData shopData = ShopSavedData.get(buyer.getServer());
        EconomyManager.removeBalance(buyer, totalCost);

        double tax = EconomyManager.calculateTax(totalCost);
        double sellerAmount = totalCost - tax;

        EconomyManager.addBalance(buyer.getServer(), shop.getOwnerUUID(), sellerAmount);

        shopItem.removeStock(quantity);
        shop.addSale(totalCost);
        shopData.setDirty();

        buyer.getInventory().add(toGive);

        // Log
        String itemName = quantity + "x " + shopItem.getItemStack().getHoverName().getString();
        TransactionLog log = TransactionLog.get(buyer.getServer());
        log.addTransaction(buyer.getUUID(), TransactionLog.Transaction.purchase(totalCost, itemName, shop.getOwnerUUID()));
        log.addTransaction(shop.getOwnerUUID(), TransactionLog.Transaction.sale(sellerAmount, itemName, buyer.getUUID()));

        String currency = ModConfig.getCurrencyName();

        // Notify buyer
        buyer.sendSystemMessage(Component.literal("[BUY] ").withStyle(s -> s.withColor(0x55FFFF).withBold(true))
            .append(Component.literal("Bought " + quantity + "x " + shopItem.getItemStack().getHoverName().getString() +
                " for " + EconomyManager.formatBalance(totalCost) + " " + currency).withStyle(s -> s.withColor(0x55FFFF).withBold(false))));
        SoundHelper.playPurchaseSound(buyer);

        // Notify seller if online
        ServerPlayer seller = buyer.getServer().getPlayerList().getPlayer(shop.getOwnerUUID());
        if (seller != null) {
            seller.sendSystemMessage(Component.literal("[SALE] ").withStyle(s -> s.withColor(0x55FF55).withBold(true))
                .append(Component.literal(buyer.getName().getString() + " bought " + quantity + "x " +
                    shopItem.getItemStack().getHoverName().getString() + " (+" + EconomyManager.formatBalance(sellerAmount) + " " + currency + ")")
                    .withStyle(s -> s.withColor(0x55FF55).withBold(false))));
            SoundHelper.playSaleSound(seller);
        } else {
            PlayerDataSavedData playerData = PlayerDataSavedData.get(buyer.getServer());
            playerData.addOfflineSale(shop.getOwnerUUID(), sellerAmount);
        }

        // Back to shop view
        buyer.closeContainer();
        buyer.openMenu(new ShopViewMenu.Provider(shopId));
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
        if (player instanceof ServerPlayer sp) {
            sp.inventoryMenu.broadcastChanges();
            sp.getInventory().setChanged();
            sp.inventoryMenu.sendAllDataToRemote();
        }
    }

    public static class Provider implements net.minecraft.world.MenuProvider {
        private final UUID shopId;
        private final int itemIndex;
        private final int initialQuantity;

        public Provider(UUID shopId, int itemIndex, int initialQuantity) {
            this.shopId = shopId;
            this.itemIndex = itemIndex;
            this.initialQuantity = initialQuantity;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Confirm Purchase");
        }

        @Override
        public ConfirmBuyMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ConfirmBuyMenu(containerId, playerInventory, shopId, itemIndex, initialQuantity);
        }
    }
}
