package com.simpleeconomy.gui;

import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.network.packets.DeleteShopPacket;
import com.simpleeconomy.network.packets.SyncShopsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteScreen extends Screen {

    private final Screen parent;
    private final SyncShopsPacket.ShopData shop;

    private int guiLeft;
    private int guiTop;
    private int guiWidth = 176;
    private int guiHeight = 80;

    public ConfirmDeleteScreen(Screen parent, SyncShopsPacket.ShopData shop) {
        super(Component.literal("Delete Shop"));
        this.parent = parent;
        this.shop = shop;
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;

        // Delete button
        this.addRenderableWidget(Button.builder(Component.literal("Delete"), btn -> {
            NetworkHandler.sendToServer(new DeleteShopPacket(shop.shopId()));
            minecraft.setScreen(parent);
        }).bounds(guiLeft + 8, guiTop + 54, 76, 18).build());

        // Cancel button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + guiWidth - 84, guiTop + 54, 76, 18).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Container background (reddish tint)
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFFD6C6C6);
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + 1, 0xFFFFFFFF);
        graphics.fill(guiLeft, guiTop + 1, guiLeft + 1, guiTop + guiHeight, 0xFFFFFFFF);
        graphics.fill(guiLeft, guiTop + guiHeight - 1, guiLeft + guiWidth, guiTop + guiHeight, 0xFF555555);
        graphics.fill(guiLeft + guiWidth - 1, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFF555555);

        // Title
        graphics.drawString(this.font, "Delete Shop?", guiLeft + 8, guiTop + 6, 0xAA0000, false);

        // Warning text
        graphics.drawCenteredString(this.font, "Are you sure you want to delete", guiLeft + guiWidth / 2, guiTop + 20, 0x404040);

        String shopName = shop.shopName();
        if (font.width(shopName) > guiWidth - 20) {
            shopName = font.plainSubstrByWidth(shopName, guiWidth - 24) + "...";
        }
        graphics.drawCenteredString(this.font, "\"" + shopName + "\"?", guiLeft + guiWidth / 2, guiTop + 32, 0x404040);
        graphics.drawCenteredString(this.font, "This cannot be undone!", guiLeft + guiWidth / 2, guiTop + 44, 0xAA0000);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
