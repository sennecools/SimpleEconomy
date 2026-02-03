package com.simpleeconomy.gui;

import com.mojang.authlib.GameProfile;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.network.packets.CreateShopPacket;
import com.simpleeconomy.network.packets.RequestShopsPacket;
import com.simpleeconomy.network.packets.SyncShopsPacket;
import com.simpleeconomy.network.packets.ToggleFavoritePacket;
import com.simpleeconomy.shop.ShopCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

public class ShopBrowserScreen extends Screen {

    private static final int SHOPS_PER_PAGE = 21; // 7 columns x 3 rows
    private static final int COLUMNS = 7;
    private static final int SHOP_SIZE = 24;
    private static final int SHOP_SPACING = 4;

    private int page = 0;
    private EditBox searchBox;
    private Button prevButton;
    private Button nextButton;
    private Button sortButton;
    private Button categoryButton;
    private Button createShopButton;

    private int guiLeft;
    private int guiTop;
    private int guiWidth = 256;
    private int guiHeight = 200;

    public ShopBrowserScreen() {
        super(Component.literal("Shop Browser"));
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;

        // Request shop data from server
        NetworkHandler.sendToServer(new RequestShopsPacket());

        // Search box
        searchBox = new EditBox(this.font, guiLeft + 8, guiTop + 20, 150, 16, Component.literal("Search..."));
        searchBox.setMaxLength(50);
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
        }).bounds(guiLeft + 162, guiTop + 19, 60, 18).build();
        this.addRenderableWidget(sortButton);

        // Category filter button
        String catName = ClientShopData.getFilterCategory() == null ? "All" : ClientShopData.getFilterCategory().getDisplayName();
        categoryButton = Button.builder(Component.literal(catName), btn -> {
            ShopCategory current = ClientShopData.getFilterCategory();
            ShopCategory[] categories = ShopCategory.values();
            if (current == null) {
                ClientShopData.setFilterCategory(categories[0]);
            } else {
                int nextIdx = (current.ordinal() + 1) % (categories.length + 1);
                if (nextIdx == categories.length) {
                    ClientShopData.setFilterCategory(null);
                } else {
                    ClientShopData.setFilterCategory(categories[nextIdx]);
                }
            }
            ShopCategory newCat = ClientShopData.getFilterCategory();
            btn.setMessage(Component.literal(newCat == null ? "All" : newCat.getDisplayName()));
            page = 0;
        }).bounds(guiLeft + 224, guiTop + 19, 24, 18).build();
        this.addRenderableWidget(categoryButton);

        // Navigation buttons
        prevButton = Button.builder(Component.literal("<"), btn -> {
            if (page > 0) page--;
        }).bounds(guiLeft + 8, guiTop + guiHeight - 24, 20, 20).build();
        this.addRenderableWidget(prevButton);

        nextButton = Button.builder(Component.literal(">"), btn -> {
            int maxPages = getMaxPages();
            if (page < maxPages - 1) page++;
        }).bounds(guiLeft + guiWidth - 28, guiTop + guiHeight - 24, 20, 20).build();
        this.addRenderableWidget(nextButton);

        // Create shop button (+)
        createShopButton = Button.builder(Component.literal("+"), btn -> {
            minecraft.setScreen(new CreateShopScreen(this));
        }).bounds(guiLeft + guiWidth / 2 - 10, guiTop + guiHeight - 24, 20, 20).build();
        this.addRenderableWidget(createShopButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xCC000000);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + guiWidth - 1, guiTop + guiHeight - 1, 0xCC222222);

        // Title
        graphics.drawCenteredString(this.font, "Marketplace", guiLeft + guiWidth / 2, guiTop + 6, 0xFFFFFF);

        // Balance display
        String balanceText = EconomyManager.formatBalance(ClientShopData.getBalance()) + " coins";
        graphics.drawString(this.font, balanceText, guiLeft + guiWidth - font.width(balanceText) - 8, guiTop + 6, 0xFFD700);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Render shops
        List<SyncShopsPacket.ShopData> shops = ClientShopData.getFilteredShops();
        int startIdx = page * SHOPS_PER_PAGE;
        int endIdx = Math.min(startIdx + SHOPS_PER_PAGE, shops.size());

        int shopAreaY = guiTop + 42;
        int shopAreaX = guiLeft + 8;

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = shopAreaX + col * (SHOP_SIZE + SHOP_SPACING);
            int y = shopAreaY + row * (SHOP_SIZE + SHOP_SPACING + 12);

            SyncShopsPacket.ShopData shop = shops.get(i);
            renderShopEntry(graphics, shop, x, y, mouseX, mouseY);
        }

        // Page indicator
        int maxPages = Math.max(1, getMaxPages());
        String pageText = "Page " + (page + 1) + "/" + maxPages;
        graphics.drawCenteredString(this.font, pageText, guiLeft + guiWidth / 2, guiTop + guiHeight - 18, 0xAAAAAA);

        // No shops message
        if (shops.isEmpty()) {
            graphics.drawCenteredString(this.font, "No shops found!", guiLeft + guiWidth / 2, guiTop + 80, 0xFF5555);
            graphics.drawCenteredString(this.font, "Click + to create one", guiLeft + guiWidth / 2, guiTop + 95, 0xAAAAAA);
        }

        // Tooltips
        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = shopAreaX + col * (SHOP_SIZE + SHOP_SPACING);
            int y = shopAreaY + row * (SHOP_SIZE + SHOP_SPACING + 12);

            if (mouseX >= x && mouseX < x + SHOP_SIZE && mouseY >= y && mouseY < y + SHOP_SIZE) {
                SyncShopsPacket.ShopData shop = shops.get(i);
                renderShopTooltip(graphics, shop, mouseX, mouseY);
            }
        }
    }

    private void renderShopEntry(GuiGraphics graphics, SyncShopsPacket.ShopData shop, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + SHOP_SIZE && mouseY >= y && mouseY < y + SHOP_SIZE;

        // Background
        int bgColor = hovered ? 0xFF444444 : 0xFF333333;
        if (ClientShopData.isFavorite(shop.shopId())) {
            bgColor = hovered ? 0xFF554400 : 0xFF443300;
        }
        if (shop.featured()) {
            bgColor = hovered ? 0xFF004444 : 0xFF003333;
        }
        graphics.fill(x, y, x + SHOP_SIZE, y + SHOP_SIZE, bgColor);

        // Border
        int borderColor = shop.featured() ? 0xFF00AAAA : (ClientShopData.isFavorite(shop.shopId()) ? 0xFFFFAA00 : 0xFF555555);
        graphics.fill(x, y, x + SHOP_SIZE, y + 1, borderColor);
        graphics.fill(x, y + SHOP_SIZE - 1, x + SHOP_SIZE, y + SHOP_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + SHOP_SIZE, borderColor);
        graphics.fill(x + SHOP_SIZE - 1, y, x + SHOP_SIZE, y + SHOP_SIZE, borderColor);

        // Player head
        renderPlayerHead(graphics, shop.ownerUUID(), shop.ownerName(), x + 4, y + 4, 16);

        // Shop name below
        String name = shop.shopName();
        if (name.length() > 6) {
            name = name.substring(0, 5) + "..";
        }
        graphics.drawCenteredString(this.font, name, x + SHOP_SIZE / 2, y + SHOP_SIZE + 2, 0xCCCCCC);
    }

    private void renderPlayerHead(GuiGraphics graphics, UUID playerUUID, String playerName, int x, int y, int size) {
        try {
            GameProfile profile = new GameProfile(playerUUID, playerName);
            PlayerSkin skin = minecraft.getSkinManager().getInsecureSkin(profile);
            graphics.blit(x, y, size, size, 8, 8, 8, 8, 64, 64, skin.texture());
            // Hat layer
            graphics.blit(x, y, size, size, 40, 8, 8, 8, 64, 64, skin.texture());
        } catch (Exception e) {
            // Fallback: draw a placeholder
            graphics.fill(x, y, x + size, y + size, 0xFF8B4513);
        }
    }

    private void renderShopTooltip(GuiGraphics graphics, SyncShopsPacket.ShopData shop, int mouseX, int mouseY) {
        List<Component> tooltip = new java.util.ArrayList<>();
        tooltip.add(Component.literal(shop.shopName()).withStyle(style -> style.withBold(true)));
        tooltip.add(Component.literal("by " + shop.ownerName()).withStyle(style -> style.withColor(0xAAAAAA)));

        if (!shop.description().isEmpty()) {
            tooltip.add(Component.literal(shop.description()).withStyle(style -> style.withColor(0x888888).withItalic(true)));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Items: " + shop.items().size()).withStyle(style -> style.withColor(0xFFFF55)));
        tooltip.add(Component.literal("Sales: " + shop.totalSales()).withStyle(style -> style.withColor(0x55FF55)));

        if (shop.featured()) {
            tooltip.add(Component.literal("FEATURED").withStyle(style -> style.withColor(0x55FFFF).withBold(true)));
        }
        if (ClientShopData.isFavorite(shop.shopId())) {
            tooltip.add(Component.literal("Favorited").withStyle(style -> style.withColor(0xFFAA00)));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Click to view shop").withStyle(style -> style.withColor(0x777777)));
        tooltip.add(Component.literal("Shift+Click to favorite").withStyle(style -> style.withColor(0x777777)));

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

        int shopAreaY = guiTop + 42;
        int shopAreaX = guiLeft + 8;

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLUMNS;
            int row = localIdx / COLUMNS;

            int x = shopAreaX + col * (SHOP_SIZE + SHOP_SPACING);
            int y = shopAreaY + row * (SHOP_SIZE + SHOP_SPACING + 12);

            if (mouseX >= x && mouseX < x + SHOP_SIZE && mouseY >= y && mouseY < y + SHOP_SIZE) {
                SyncShopsPacket.ShopData shop = shops.get(i);

                if (hasShiftDown()) {
                    // Toggle favorite
                    NetworkHandler.sendToServer(new ToggleFavoritePacket(shop.shopId()));
                } else {
                    // Open shop view
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
