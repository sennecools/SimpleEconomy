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

    private static final int ITEMS_PER_PAGE = 21; // 7 columns x 3 rows
    private static final int COLUMNS = 7;
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_SPACING = 4;

    private int page = 0;
    private int guiLeft;
    private int guiTop;
    private int guiWidth = 256;
    private int guiHeight = 200;

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
        backButton = Button.builder(Component.literal("< Back"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + 8, guiTop + guiHeight - 24, 50, 20).build();
        this.addRenderableWidget(backButton);

        // Navigation
        prevButton = Button.builder(Component.literal("<"), btn -> {
            if (page > 0) page--;
        }).bounds(guiLeft + 62, guiTop + guiHeight - 24, 20, 20).build();
        this.addRenderableWidget(prevButton);

        nextButton = Button.builder(Component.literal(">"), btn -> {
            int maxPages = getMaxPages();
            if (page < maxPages - 1) page++;
        }).bounds(guiLeft + guiWidth - 28, guiTop + guiHeight - 24, 20, 20).build();
        this.addRenderableWidget(nextButton);

        // Favorite button (for non-owners)
        if (!isOwner) {
            String favText = ClientShopData.isFavorite(shop.shopId()) ? "Unfavorite" : "Favorite";
            favoriteButton = Button.builder(Component.literal(favText), btn -> {
                NetworkHandler.sendToServer(new ToggleFavoritePacket(shop.shopId()));
                minecraft.setScreen(parent);
            }).bounds(guiLeft + guiWidth - 70, guiTop + guiHeight - 24, 60, 20).build();
            this.addRenderableWidget(favoriteButton);
        }

        // Owner controls
        if (isOwner) {
            addItemButton = Button.builder(Component.literal("+"), btn -> {
                minecraft.setScreen(new AddItemScreen(this, shop.shopId()));
            }).bounds(guiLeft + guiWidth / 2 - 10, guiTop + guiHeight - 24, 20, 20).build();
            this.addRenderableWidget(addItemButton);

            editShopButton = Button.builder(Component.literal("Edit"), btn -> {
                minecraft.setScreen(new EditShopScreen(parent, shop));
            }).bounds(guiLeft + guiWidth - 90, guiTop + guiHeight - 24, 40, 20).build();
            this.addRenderableWidget(editShopButton);

            deleteShopButton = Button.builder(Component.literal("X"), btn -> {
                minecraft.setScreen(new ConfirmDeleteScreen(parent, shop));
            }).bounds(guiLeft + guiWidth - 45, guiTop + guiHeight - 24, 20, 20).build();
            this.addRenderableWidget(deleteShopButton);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xCC000000);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + guiWidth - 1, guiTop + guiHeight - 1, 0xCC222222);

        // Title
        graphics.drawCenteredString(this.font, shop.shopName(), guiLeft + guiWidth / 2, guiTop + 6, 0xFFFFFF);

        // Owner and stats
        graphics.drawString(this.font, "by " + shop.ownerName(), guiLeft + 8, guiTop + 20, 0xAAAAAA);
        graphics.drawString(this.font, "Sales: " + shop.totalSales(), guiLeft + 8, guiTop + 32, 0x55FF55);

        // Balance
        String balanceText = EconomyManager.formatBalance(ClientShopData.getBalance()) + " coins";
        graphics.drawString(this.font, balanceText, guiLeft + guiWidth - font.width(balanceText) - 8, guiTop + 6, 0xFFD700);

        // Description
        if (!shop.description().isEmpty()) {
            graphics.drawString(this.font, shop.description(), guiLeft + guiWidth / 2 - font.width(shop.description()) / 2, guiTop + 20, 0x888888);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        // Render items
        List<SyncShopsPacket.ItemData> items = shop.items();
        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, items.size());

        int itemAreaY = guiTop + 48;
        int itemAreaX = guiLeft + 8;

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = itemAreaX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = itemAreaY + row * (SLOT_SIZE + SLOT_SPACING);

            SyncShopsPacket.ItemData item = items.get(i);
            renderItemSlot(graphics, item, x, y, mouseX, mouseY);
        }

        // Empty shop message
        if (items.isEmpty()) {
            if (isOwner) {
                graphics.drawCenteredString(this.font, "Your shop is empty!", guiLeft + guiWidth / 2, guiTop + 80, 0xFF5555);
                graphics.drawCenteredString(this.font, "Click + to add items", guiLeft + guiWidth / 2, guiTop + 95, 0xAAAAAA);
            } else {
                graphics.drawCenteredString(this.font, "This shop is empty", guiLeft + guiWidth / 2, guiTop + 80, 0xAAAAAA);
            }
        }

        // Page indicator
        int maxPages = Math.max(1, getMaxPages());
        String pageText = "Page " + (page + 1) + "/" + maxPages;
        graphics.drawCenteredString(this.font, pageText, guiLeft + guiWidth / 2, guiTop + guiHeight - 36, 0xAAAAAA);

        // Tooltips
        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = itemAreaX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = itemAreaY + row * (SLOT_SIZE + SLOT_SPACING);

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                SyncShopsPacket.ItemData item = items.get(i);
                renderItemTooltip(graphics, item, mouseX, mouseY);
            }
        }
    }

    private void renderItemSlot(GuiGraphics graphics, SyncShopsPacket.ItemData item, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
        boolean outOfStock = item.stock() <= 0;

        // Background
        int bgColor = outOfStock ? 0xFF442222 : (hovered ? 0xFF444444 : 0xFF333333);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

        // Border
        int borderColor = outOfStock ? 0xFF882222 : 0xFF555555;
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, borderColor);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, borderColor);
        graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);

        // Item
        graphics.renderItem(item.itemStack(), x + 4, y + 4);
        graphics.renderItemDecorations(this.font, item.itemStack(), x + 4, y + 4);

        // Stock indicator
        if (outOfStock) {
            graphics.drawCenteredString(this.font, "X", x + SLOT_SIZE / 2, y + SLOT_SIZE / 2 - 4, 0xFF5555);
        }
    }

    private void renderItemTooltip(GuiGraphics graphics, SyncShopsPacket.ItemData item, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();

        // Item name from vanilla tooltip
        tooltip.addAll(Screen.getTooltipFromItem(minecraft, item.itemStack()));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Price: " + EconomyManager.formatBalance(item.price()) + " coins")
            .withStyle(style -> style.withColor(0xFFD700)));

        if (item.itemStack().getCount() > 1) {
            double perItem = item.price() / item.itemStack().getCount();
            tooltip.add(Component.literal("(" + EconomyManager.formatBalance(perItem) + " per item)")
                .withStyle(style -> style.withColor(0xAAAA00)));
        }

        tooltip.add(Component.literal("Stock: " + item.stock())
            .withStyle(style -> style.withColor(item.stock() > 0 ? 0x55FF55 : 0xFF5555)));

        tooltip.add(Component.empty());
        if (isOwner) {
            tooltip.add(Component.literal("Click to remove from shop").withStyle(style -> style.withColor(0x777777)));
        } else if (item.stock() > 0) {
            tooltip.add(Component.literal("Click to buy").withStyle(style -> style.withColor(0x777777)));
            tooltip.add(Component.literal("Shift+Click to buy stack").withStyle(style -> style.withColor(0x777777)));
        } else {
            tooltip.add(Component.literal("Out of stock!").withStyle(style -> style.withColor(0xFF5555)));
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

        int itemAreaY = guiTop + 48;
        int itemAreaX = guiLeft + 8;

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = itemAreaX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = itemAreaY + row * (SLOT_SIZE + SLOT_SPACING);

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                SyncShopsPacket.ItemData item = items.get(i);

                if (isOwner) {
                    // Remove item from shop
                    NetworkHandler.sendToServer(new RemoveShopItemPacket(shop.shopId(), item.itemId()));
                    minecraft.setScreen(parent);
                } else if (item.stock() > 0) {
                    // Purchase
                    int quantity = hasShiftDown() ? Math.min(item.stock(), 64 / item.itemStack().getCount()) : 1;
                    quantity = Math.max(1, quantity);
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
