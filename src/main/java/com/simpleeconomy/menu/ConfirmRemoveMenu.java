package com.simpleeconomy.menu;

import com.simpleeconomy.data.ShopSavedData;
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
 * 1-row confirmation menu for removing an item listing from your shop.
 * Layout: [Cancel] [glass] [glass] [glass] [Item Info] [glass] [glass] [glass] [Confirm]
 */
public class ConfirmRemoveMenu extends ChestMenu {
    private final Player player;
    private final SimpleContainer container;
    private final UUID shopId;
    private final int itemIndex;

    private static final int CANCEL_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int CONFIRM_SLOT = 8;

    public ConfirmRemoveMenu(int containerId, Inventory playerInventory, UUID shopId, int itemIndex) {
        super(MenuType.GENERIC_9x1, containerId, playerInventory, new SimpleContainer(9), 1);
        this.player = playerInventory.player;
        this.container = (SimpleContainer) this.getContainer();
        this.shopId = shopId;
        this.itemIndex = itemIndex;
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

    private void refreshDisplay() {
        // Fill with glass
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 9; i++) {
            container.setItem(i, glass.copy());
        }

        // Cancel
        ItemStack cancel = new ItemStack(Items.RED_CONCRETE);
        cancel.set(DataComponents.CUSTOM_NAME, Component.literal("Cancel").withStyle(Style.EMPTY.withColor(0xFF5555).withItalic(false)));
        container.setItem(CANCEL_SLOT, cancel);

        // Item info
        ShopItem shopItem = getShopItem();
        if (shopItem != null) {
            ItemStack info = shopItem.getItemStack().copy();
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("Remove from shop?").withStyle(Style.EMPTY.withColor(0xFFAA00).withItalic(false).withBold(true)));
            if (!shopItem.isInfiniteStock() && shopItem.getStock() > 0) {
                lore.add(Component.literal("Stock (" + shopItem.getStock() + "x) will be returned").withStyle(Style.EMPTY.withColor(0x888888).withItalic(false)));
            }
            info.set(DataComponents.LORE, new ItemLore(lore));
            container.setItem(INFO_SLOT, info);
        } else {
            ItemStack error = new ItemStack(Items.BARRIER);
            error.set(DataComponents.CUSTOM_NAME, Component.literal("Item not found!").withStyle(Style.EMPTY.withColor(0xFF5555)));
            container.setItem(INFO_SLOT, error);
        }

        // Confirm
        ItemStack confirm = new ItemStack(Items.LIME_CONCRETE);
        confirm.set(DataComponents.CUSTOM_NAME, Component.literal("Confirm Remove").withStyle(Style.EMPTY.withColor(0x55FF55).withItalic(false).withBold(true)));
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

        if (slotId == CANCEL_SLOT) {
            serverPlayer.closeContainer();
            serverPlayer.openMenu(new MyShopMenu.Provider());
        } else if (slotId == CONFIRM_SLOT) {
            executeRemove(serverPlayer);
        } else {
            serverPlayer.containerMenu.sendAllDataToRemote();
        }
    }

    private void executeRemove(ServerPlayer player) {
        Shop shop = getShop();
        if (shop == null || itemIndex >= shop.getItems().size()) {
            player.sendSystemMessage(Component.literal("Item no longer available!").withStyle(s -> s.withColor(0xFF5555)));
            player.closeContainer();
            player.openMenu(new MyShopMenu.Provider());
            return;
        }

        ShopItem shopItem = shop.getItems().get(itemIndex);

        // Return stock to player
        if (shopItem.getStock() > 0) {
            ItemStack toReturn = shopItem.getItemStack().copy();
            int remaining = shopItem.getStock();

            while (remaining > 0) {
                int toGive = Math.min(remaining, toReturn.getMaxStackSize());
                ItemStack stack = toReturn.copy();
                stack.setCount(toGive);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                remaining -= toGive;
            }
        }

        // Remove from shop
        shop.getItems().remove(itemIndex);
        ShopSavedData.get(player.getServer()).setDirty();
        player.sendSystemMessage(Component.literal("Item removed from shop!").withStyle(s -> s.withColor(0xFFAA00)));

        player.closeContainer();
        player.openMenu(new MyShopMenu.Provider());
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

        public Provider(UUID shopId, int itemIndex) {
            this.shopId = shopId;
            this.itemIndex = itemIndex;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Confirm Remove");
        }

        @Override
        public ConfirmRemoveMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ConfirmRemoveMenu(containerId, playerInventory, shopId, itemIndex);
        }
    }
}
