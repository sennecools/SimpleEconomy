package com.simpleeconomy.gui;

import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.network.packets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopViewScreen extends Screen {

    private final Screen parent;
    private final SyncShopsPacket.ShopData shop;
    private final boolean isOwner;

    private static final int ITEMS_PER_PAGE = 27;
    private static final int COLUMNS = 9;
    private static final int SLOT_SIZE = 18;

    private int page = 0;
    private int guiLeft;
    private int guiTop;
    private int guiWidth = 176;
    private int guiHeight = 166;

    private Button prevButton;
    private Button nextButton;
    private Button backButton;
    private Button addItemButton;
    private Button editShopButton;
    private Button deleteShopButton;
    private Button favoriteButton;

    public ShopViewScreen(Screen parent, SyncShopsPacket.ShopData shop) {
        super(Component.literal(shop.shopName()));
        this.parent = parent;
        this.shop = shop;
        this.isOwner = Minecraft.getInstance().player != null &&
            Minecraft.getInstance().player.getUUID().equals(shop.ownerUUID());
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;

        // Back button
        backButton = Button.builder(Component.literal("Back"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + 8, guiTop + guiHeight - 22, 35, 16).build();
        this.addRenderableWidget(backButton);

        // Navigation
        prevButton = Button.builder(Component.literal("<"), btn -> {
            if (page > 0) page--;
        }).bounds(guiLeft + 46, guiTop + guiHeight - 22, 16, 16).build();
        this.addRenderableWidget(prevButton);

        nextButton = Button.builder(Component.literal(">"), btn -> {
            int maxPages = getMaxPages();
            if (page < maxPages - 1) page++;
        }).bounds(guiLeft + guiWidth - 62, guiTop + guiHeight - 22, 16, 16).build();
        this.addRenderableWidget(nextButton);

        // Favorite button (for non-owners)
        if (!isOwner) {
            String favText = ClientShopData.isFavorite(shop.shopId()) ? "Unfav" : "Fav";
            favoriteButton = Button.builder(Component.literal(favText), btn -> {
                NetworkHandler.sendToServer(new ToggleFavoritePacket(shop.shopId()));
                minecraft.setScreen(parent);
            }).bounds(guiLeft + guiWidth - 43, guiTop + guiHeight - 22, 35, 16).build();
            this.addRenderableWidget(favoriteButton);
        }

        // Owner controls
        if (isOwner) {
            addItemButton = Button.builder(Component.literal("+ Add"), btn -> {
                minecraft.setScreen(new AddItemScreen(this, shop.shopId()));
            }).bounds(guiLeft + guiWidth / 2 - 20, guiTop + guiHeight - 22, 40, 16).build();
            this.addRenderableWidget(addItemButton);

            editShopButton = Button.builder(Component.literal("Edit"), btn -> {
                minecraft.setScreen(new EditShopScreen(parent, shop));
            }).bounds(guiLeft + guiWidth - 63, guiTop + guiHeight - 22, 30, 16).build();
            this.addRenderableWidget(editShopButton);

            deleteShopButton = Button.builder(Component.literal("X"), btn -> {
                minecraft.setScreen(new ConfirmDeleteScreen(parent, shop));
            }).bounds(guiLeft + guiWidth - 30, guiTop + guiHeight - 22, 20, 16).build();
            this.addRenderableWidget(deleteShopButton);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Container background
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFFC6C6C6);
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + 1, 0xFFFFFFFF);
        graphics.fill(guiLeft, guiTop + 1, guiLeft + 1, guiTop + guiHeight, 0xFFFFFFFF);
        graphics.fill(guiLeft, guiTop + guiHeight - 1, guiLeft + guiWidth, guiTop + guiHeight, 0xFF555555);
        graphics.fill(guiLeft + guiWidth - 1, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFF555555);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + guiWidth - 1, guiTop + 2, 0xFFDBDBDB);
        graphics.fill(guiLeft + 1, guiTop + 2, guiLeft + 2, guiTop + guiHeight - 1, 0xFFDBDBDB);

        // Title
        String title = shop.shopName();
        if (font.width(title) > guiWidth - 16) {
            title = font.plainSubstrByWidth(title, guiWidth - 20) + "...";
        }
        graphics.drawString(this.font, title, guiLeft + 8, guiTop + 6, 0x404040, false);

        // Owner and stats
        graphics.drawString(this.font, "by " + shop.ownerName(), guiLeft + 8, guiTop + 18, 0x606060, false);

        // Balance
        String balanceText = EconomyManager.formatBalance(ClientShopData.getBalance()) + " coins";
        graphics.drawString(this.font, balanceText, guiLeft + guiWidth - font.width(balanceText) - 8, guiTop + 6, 0x404040, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Items area
        int slotAreaX = guiLeft + 8;
        int slotAreaY = guiTop + 32;
        int slotAreaWidth = guiWidth - 16;
        int slotAreaHeight = 54;

        // Dark slot area
        graphics.fill(slotAreaX, slotAreaY, slotAreaX + slotAreaWidth, slotAreaY + slotAreaHeight, 0xFF8B8B8B);
        graphics.fill(slotAreaX, slotAreaY, slotAreaX + slotAreaWidth, slotAreaY + 1, 0xFF373737);
        graphics.fill(slotAreaX, slotAreaY, slotAreaX + 1, slotAreaY + slotAreaHeight, 0xFF373737);
        graphics.fill(slotAreaX + 1, slotAreaY + slotAreaHeight - 1, slotAreaX + slotAreaWidth, slotAreaY + slotAreaHeight, 0xFFFFFFFF);
        graphics.fill(slotAreaX + slotAreaWidth - 1, slotAreaY + 1, slotAreaX + slotAreaWidth, slotAreaY + slotAreaHeight - 1, 0xFFFFFFFF);

        // Render items
        List<SyncShopsPacket.ItemData> items = shop.items();
        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, items.size());

        int slotsStartX = slotAreaX + 1;
        int slotsStartY = slotAreaY + 1;

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = slotsStartX + col * SLOT_SIZE;
            int y = slotsStartY + row * SLOT_SIZE;

            SyncShopsPacket.ItemData item = items.get(i);
            renderItemSlot(graphics, item, x, y, mouseX, mouseY);
        }

        // Empty shop message
        if (items.isEmpty()) {
            if (isOwner) {
                graphics.drawCenteredString(this.font, "Your shop is empty!", guiLeft + guiWidth / 2, slotAreaY + 18, 0x404040);
                graphics.drawCenteredString(this.font, "Click '+ Add' to add items", guiLeft + guiWidth / 2, slotAreaY + 30, 0x606060);
            } else {
                graphics.drawCenteredString(this.font, "This shop is empty", guiLeft + guiWidth / 2, slotAreaY + 24, 0x606060);
            }
        }

        // Stats area
        int statsY = slotAreaY + slotAreaHeight + 4;
        graphics.drawString(this.font, "Items: " + items.size(), guiLeft + 8, statsY, 0x404040, false);
        graphics.drawString(this.font, "Sales: " + shop.totalSales(), guiLeft + 70, statsY, 0x404040, false);

        // Page indicator
        int maxPages = Math.max(1, getMaxPages());
        String pageText = "Page " + (page + 1) + "/" + maxPages;
        graphics.drawCenteredString(this.font, pageText, guiLeft + guiWidth / 2, guiTop + guiHeight - 34, 0x404040);

        // Tooltips
        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = slotsStartX + col * SLOT_SIZE;
            int y = slotsStartY + row * SLOT_SIZE;

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                SyncShopsPacket.ItemData item = items.get(i);
                renderItemTooltip(graphics, item, mouseX, mouseY);
            }
        }
    }

    private void renderItemSlot(GuiGraphics graphics, SyncShopsPacket.ItemData item, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
        boolean outOfStock = item.stock() <= 0;

        // Slot background
        int bgColor = outOfStock ? 0xFF5B3B3B : (hovered ? 0xFFAAAAAA : 0xFF8B8B8B);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

        // Item
        graphics.renderItem(item.itemStack(), x + 1, y + 1);
        graphics.renderItemDecorations(this.font, item.itemStack(), x + 1, y + 1);

        // Out of stock overlay
        if (outOfStock) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x80FF0000);
        }
    }

    private void renderItemTooltip(GuiGraphics graphics, SyncShopsPacket.ItemData item, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();

        // Item name from vanilla tooltip
        tooltip.addAll(Screen.getTooltipFromItem(minecraft, item.itemStack()));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Price: " + EconomyManager.formatBalance(item.price()) + " coins")
            .withStyle(style -> style.withColor(0xFFAA00)));

        if (item.itemStack().getCount() > 1) {
            double perItem = item.price() / item.itemStack().getCount();
            tooltip.add(Component.literal("(" + EconomyManager.formatBalance(perItem) + " per item)")
                .withStyle(style -> style.withColor(0xAA8800)));
        }

        tooltip.add(Component.literal("Stock: " + item.stock())
            .withStyle(style -> style.withColor(item.stock() > 0 ? 0x00AA00 : 0xAA0000)));

        tooltip.add(Component.empty());
        if (isOwner) {
            tooltip.add(Component.literal("Click to remove from shop").withStyle(style -> style.withColor(0x555555)));
        } else if (item.stock() > 0) {
            tooltip.add(Component.literal("Click to buy").withStyle(style -> style.withColor(0x555555)));
        } else {
            tooltip.add(Component.literal("Out of stock!").withStyle(style -> style.withColor(0xAA0000)));
        }

        graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        List<SyncShopsPacket.ItemData> items = shop.items();
        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, items.size());

        int slotAreaX = guiLeft + 8;
        int slotAreaY = guiTop + 32;
        int slotsStartX = slotAreaX + 1;
        int slotsStartY = slotAreaY + 1;

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = slotsStartX + col * SLOT_SIZE;
            int y = slotsStartY + row * SLOT_SIZE;

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                SyncShopsPacket.ItemData item = items.get(i);

                if (isOwner) {
                    NetworkHandler.sendToServer(new RemoveShopItemPacket(shop.shopId(), item.itemId()));
                    minecraft.setScreen(parent);
                } else if (item.stock() > 0) {
                    int quantity = 1;
                    NetworkHandler.sendToServer(new PurchaseItemPacket(shop.shopId(), item.itemId(), quantity));
                    minecraft.setScreen(parent);
                }
                return true;
            }
        }

        return false;
    }

    private int getMaxPages() {
        int totalItems = shop.items().size();
        return (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
