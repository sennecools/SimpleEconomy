package com.simpleeconomy.gui;

import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.network.packets.RequestShopsPacket;
import com.simpleeconomy.network.packets.SyncShopsPacket;
import com.simpleeconomy.network.packets.ToggleFavoritePacket;
import com.simpleeconomy.shop.ShopCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

public class ShopBrowserScreen extends Screen {

    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int SHOPS_PER_PAGE = 21;
    private static final int COLUMNS = 7;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 2;

    private int page = 0;
    private EditBox searchBox;
    private Button prevButton;
    private Button nextButton;
    private Button sortButton;
    private Button categoryButton;
    private Button createShopButton;

    private int guiLeft;
    private int guiTop;
    private int guiWidth = 176;
    private int guiHeight = 166;

    public ShopBrowserScreen() {
        super(Component.literal("Marketplace"));
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;

        NetworkHandler.sendToServer(new RequestShopsPacket());

        // Search box
        searchBox = new EditBox(this.font, guiLeft + 8, guiTop + 18, 100, 12, Component.literal("Search..."));
        searchBox.setMaxLength(50);
        searchBox.setBordered(true);
        searchBox.setValue(ClientShopData.getSearchQuery());
        searchBox.setResponder(text -> {
            ClientShopData.setSearchQuery(text);
            page = 0;
        });
        this.addRenderableWidget(searchBox);

        // Sort button
        sortButton = Button.builder(Component.literal(ClientShopData.getSortType().getDisplayName()), btn -> {
            ClientShopData.setSortType(ClientShopData.getSortType().next());
            btn.setMessage(Component.literal(ClientShopData.getSortType().getDisplayName()));
        }).bounds(guiLeft + 112, guiTop + 16, 56, 14).build();
        this.addRenderableWidget(sortButton);

        // Navigation buttons
        prevButton = Button.builder(Component.literal("<"), btn -> {
            if (page > 0) page--;
        }).bounds(guiLeft + 8, guiTop + guiHeight - 22, 20, 16).build();
        this.addRenderableWidget(prevButton);

        nextButton = Button.builder(Component.literal(">"), btn -> {
            int maxPages = getMaxPages();
            if (page < maxPages - 1) page++;
        }).bounds(guiLeft + guiWidth - 28, guiTop + guiHeight - 22, 20, 16).build();
        this.addRenderableWidget(nextButton);

        // Create shop button
        createShopButton = Button.builder(Component.literal("+ New Shop"), btn -> {
            minecraft.setScreen(new CreateShopScreen(this));
        }).bounds(guiLeft + guiWidth / 2 - 30, guiTop + guiHeight - 22, 60, 16).build();
        this.addRenderableWidget(createShopButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Darken background
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Draw container-style background
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFFC6C6C6);
        // Top border
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + 1, 0xFFFFFFFF);
        graphics.fill(guiLeft, guiTop + 1, guiLeft + 1, guiTop + guiHeight, 0xFFFFFFFF);
        // Bottom border (dark)
        graphics.fill(guiLeft, guiTop + guiHeight - 1, guiLeft + guiWidth, guiTop + guiHeight, 0xFF555555);
        graphics.fill(guiLeft + guiWidth - 1, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFF555555);
        // Inner shadow
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + guiWidth - 1, guiTop + 2, 0xFFDBDBDB);
        graphics.fill(guiLeft + 1, guiTop + 2, guiLeft + 2, guiTop + guiHeight - 1, 0xFFDBDBDB);

        // Title
        graphics.drawString(this.font, "Marketplace", guiLeft + 8, guiTop + 6, 0x404040, false);

        // Balance
        String balanceText = EconomyManager.formatBalance(ClientShopData.getBalance()) + " coins";
        graphics.drawString(this.font, balanceText, guiLeft + guiWidth - font.width(balanceText) - 8, guiTop + 6, 0x404040, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Render shop slots area
        int slotAreaX = guiLeft + 8;
        int slotAreaY = guiTop + 34;
        int slotAreaWidth = guiWidth - 16;
        int slotAreaHeight = 80;

        // Dark slot area background
        graphics.fill(slotAreaX, slotAreaY, slotAreaX + slotAreaWidth, slotAreaY + slotAreaHeight, 0xFF8B8B8B);
        graphics.fill(slotAreaX, slotAreaY, slotAreaX + slotAreaWidth, slotAreaY + 1, 0xFF373737);
        graphics.fill(slotAreaX, slotAreaY, slotAreaX + 1, slotAreaY + slotAreaHeight, 0xFF373737);
        graphics.fill(slotAreaX + 1, slotAreaY + slotAreaHeight - 1, slotAreaX + slotAreaWidth, slotAreaY + slotAreaHeight, 0xFFFFFFFF);
        graphics.fill(slotAreaX + slotAreaWidth - 1, slotAreaY + 1, slotAreaX + slotAreaWidth, slotAreaY + slotAreaHeight - 1, 0xFFFFFFFF);

        // Render shops
        List<SyncShopsPacket.ShopData> shops = ClientShopData.getFilteredShops();
        int startIdx = page * SHOPS_PER_PAGE;
        int endIdx = Math.min(startIdx + SHOPS_PER_PAGE, shops.size());

        int slotsStartX = slotAreaX + 4;
        int slotsStartY = slotAreaY + 4;

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = slotsStartX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = slotsStartY + row * (SLOT_SIZE + SLOT_SPACING + 10);

            SyncShopsPacket.ShopData shop = shops.get(i);
            renderShopSlot(graphics, shop, x, y, mouseX, mouseY);
        }

        // Page indicator
        int maxPages = Math.max(1, getMaxPages());
        String pageText = "Page " + (page + 1) + "/" + maxPages;
        graphics.drawCenteredString(this.font, pageText, guiLeft + guiWidth / 2, guiTop + guiHeight - 34, 0x404040);

        // No shops message
        if (shops.isEmpty()) {
            graphics.drawCenteredString(this.font, "No shops found!", guiLeft + guiWidth / 2, slotAreaY + 30, 0x404040);
            graphics.drawCenteredString(this.font, "Click '+ New Shop' to create one", guiLeft + guiWidth / 2, slotAreaY + 42, 0x606060);
        }

        // Tooltips
        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = slotsStartX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = slotsStartY + row * (SLOT_SIZE + SLOT_SPACING + 10);

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                SyncShopsPacket.ShopData shop = shops.get(i);
                renderShopTooltip(graphics, shop, mouseX, mouseY);
            }
        }
    }

    private void renderShopSlot(GuiGraphics graphics, SyncShopsPacket.ShopData shop, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

        // Slot background (vanilla style)
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, 0xFF373737);
        graphics.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFFFF);
        graphics.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1, 0xFFFFFFFF);

        // Inner slot
        int innerColor = hovered ? 0xFFAAAAAA : 0xFF8B8B8B;
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, innerColor);

        // Render player head as item
        ItemStack headStack = new ItemStack(Items.PLAYER_HEAD);
        graphics.renderItem(headStack, x + 1, y + 1);

        // Favorite indicator
        if (ClientShopData.isFavorite(shop.shopId())) {
            graphics.fill(x + SLOT_SIZE - 4, y + 1, x + SLOT_SIZE - 1, y + 4, 0xFFFFAA00);
        }

        // Featured indicator
        if (shop.featured()) {
            graphics.fill(x + 1, y + 1, x + 4, y + 4, 0xFF00AAAA);
        }

        // Shop name below
        String name = shop.shopName();
        if (font.width(name) > SLOT_SIZE + 4) {
            name = font.plainSubstrByWidth(name, SLOT_SIZE) + "..";
        }
        graphics.drawCenteredString(this.font, name, x + SLOT_SIZE / 2, y + SLOT_SIZE + 1, 0x404040);
    }

    private void renderShopTooltip(GuiGraphics graphics, SyncShopsPacket.ShopData shop, int mouseX, int mouseY) {
        List<Component> tooltip = new java.util.ArrayList<>();
        tooltip.add(Component.literal(shop.shopName()).withStyle(style -> style.withBold(true)));
        tooltip.add(Component.literal("Owner: " + shop.ownerName()).withStyle(style -> style.withColor(0x555555)));

        if (!shop.description().isEmpty()) {
            tooltip.add(Component.literal(shop.description()).withStyle(style -> style.withColor(0x555555).withItalic(true)));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Items: " + shop.items().size()).withStyle(style -> style.withColor(0xAA5500)));
        tooltip.add(Component.literal("Sales: " + shop.totalSales()).withStyle(style -> style.withColor(0x00AA00)));

        if (shop.featured()) {
            tooltip.add(Component.literal("FEATURED").withStyle(style -> style.withColor(0x00AAAA).withBold(true)));
        }
        if (ClientShopData.isFavorite(shop.shopId())) {
            tooltip.add(Component.literal("Favorited").withStyle(style -> style.withColor(0xFFAA00)));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Click to view").withStyle(style -> style.withColor(0x555555)));
        tooltip.add(Component.literal("Shift+Click to favorite").withStyle(style -> style.withColor(0x555555)));

        graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        List<SyncShopsPacket.ShopData> shops = ClientShopData.getFilteredShops();
        int startIdx = page * SHOPS_PER_PAGE;
        int endIdx = Math.min(startIdx + SHOPS_PER_PAGE, shops.size());

        int slotAreaX = guiLeft + 8;
        int slotAreaY = guiTop + 34;
        int slotsStartX = slotAreaX + 4;
        int slotsStartY = slotAreaY + 4;

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = slotsStartX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = slotsStartY + row * (SLOT_SIZE + SLOT_SPACING + 10);

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                SyncShopsPacket.ShopData shop = shops.get(i);

                if (hasShiftDown()) {
                    NetworkHandler.sendToServer(new ToggleFavoritePacket(shop.shopId()));
                } else {
                    minecraft.setScreen(new ShopViewScreen(this, shop));
                }
                return true;
            }
        }

        return false;
    }

    private int getMaxPages() {
        int totalShops = ClientShopData.getFilteredShops().size();
        return (int) Math.ceil((double) totalShops / SHOPS_PER_PAGE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
