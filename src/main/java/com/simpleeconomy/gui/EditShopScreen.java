package com.simpleeconomy.gui;

import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.network.packets.SyncShopsPacket;
import com.simpleeconomy.network.packets.UpdateShopPacket;
import com.simpleeconomy.shop.ShopCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EditShopScreen extends Screen {

    private final Screen parent;
    private final SyncShopsPacket.ShopData shop;

    private EditBox nameBox;
    private EditBox descriptionBox;
    private Button categoryButton;
    private Button saveButton;
    private Button cancelButton;

    private ShopCategory selectedCategory;

    private int guiLeft;
    private int guiTop;
    private int guiWidth = 220;
    private int guiHeight = 150;

    public EditShopScreen(Screen parent, SyncShopsPacket.ShopData shop) {
        super(Component.literal("Edit Shop"));
        this.parent = parent;
        this.shop = shop;
        this.selectedCategory = shop.category();
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;

        // Name
        nameBox = new EditBox(this.font, guiLeft + 10, guiTop + 35, guiWidth - 20, 16, Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(shop.shopName());
        this.addRenderableWidget(nameBox);

        // Description
        descriptionBox = new EditBox(this.font, guiLeft + 10, guiTop + 65, guiWidth - 20, 16, Component.literal("Description"));
        descriptionBox.setMaxLength(256);
        descriptionBox.setValue(shop.description());
        this.addRenderableWidget(descriptionBox);

        // Category
        categoryButton = Button.builder(Component.literal(selectedCategory.getDisplayName()), btn -> {
            ShopCategory[] categories = ShopCategory.values();
            int nextIdx = (selectedCategory.ordinal() + 1) % categories.length;
            selectedCategory = categories[nextIdx];
            btn.setMessage(Component.literal(selectedCategory.getDisplayName()));
        }).bounds(guiLeft + 70, guiTop + 90, 100, 20).build();
        this.addRenderableWidget(categoryButton);

        // Save
        saveButton = Button.builder(Component.literal("Save"), btn -> {
            String name = nameBox.getValue().trim();
            if (!name.isEmpty()) {
                NetworkHandler.sendToServer(new UpdateShopPacket(
                    shop.shopId(),
                    name,
                    descriptionBox.getValue().trim(),
                    selectedCategory
                ));
                minecraft.setScreen(parent);
            }
        }).bounds(guiLeft + 10, guiTop + 120, 95, 20).build();
        this.addRenderableWidget(saveButton);

        // Cancel
        cancelButton = Button.builder(Component.literal("Cancel"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + guiWidth - 105, guiTop + 120, 95, 20).build();
        this.addRenderableWidget(cancelButton);

        setInitialFocus(nameBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xCC000000);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + guiWidth - 1, guiTop + guiHeight - 1, 0xCC222222);

        // Title
        graphics.drawCenteredString(this.font, "Edit Shop", guiLeft + guiWidth / 2, guiTop + 8, 0xFFFFFF);

        // Labels
        graphics.drawString(this.font, "Name:", guiLeft + 10, guiTop + 25, 0xAAAAAA);
        graphics.drawString(this.font, "Description:", guiLeft + 10, guiTop + 55, 0xAAAAAA);
        graphics.drawString(this.font, "Category:", guiLeft + 10, guiTop + 95, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
