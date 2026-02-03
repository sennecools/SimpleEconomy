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
    private int guiWidth = 176;
    private int guiHeight = 120;

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
        nameBox = new EditBox(this.font, guiLeft + 8, guiTop + 28, guiWidth - 16, 14, Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(shop.shopName());
        this.addRenderableWidget(nameBox);

        // Description
        descriptionBox = new EditBox(this.font, guiLeft + 8, guiTop + 56, guiWidth - 16, 14, Component.literal("Description"));
        descriptionBox.setMaxLength(256);
        descriptionBox.setValue(shop.description());
        this.addRenderableWidget(descriptionBox);

        // Category
        categoryButton = Button.builder(Component.literal(selectedCategory.getDisplayName()), btn -> {
            ShopCategory[] categories = ShopCategory.values();
            int nextIdx = (selectedCategory.ordinal() + 1) % categories.length;
            selectedCategory = categories[nextIdx];
            btn.setMessage(Component.literal(selectedCategory.getDisplayName()));
        }).bounds(guiLeft + 60, guiTop + 76, 80, 16).build();
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
        }).bounds(guiLeft + 8, guiTop + 96, 76, 18).build();
        this.addRenderableWidget(saveButton);

        // Cancel
        cancelButton = Button.builder(Component.literal("Cancel"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + guiWidth - 84, guiTop + 96, 76, 18).build();
        this.addRenderableWidget(cancelButton);

        setInitialFocus(nameBox);
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

        // Title
        graphics.drawString(this.font, "Edit Shop", guiLeft + 8, guiTop + 6, 0x404040, false);

        // Labels
        graphics.drawString(this.font, "Name:", guiLeft + 8, guiTop + 18, 0x404040, false);
        graphics.drawString(this.font, "Description:", guiLeft + 8, guiTop + 46, 0x404040, false);
        graphics.drawString(this.font, "Category:", guiLeft + 8, guiTop + 78, 0x404040, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
