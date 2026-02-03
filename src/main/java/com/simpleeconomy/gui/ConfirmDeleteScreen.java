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
    private int guiWidth = 200;
    private int guiHeight = 100;

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
        }).bounds(guiLeft + 10, guiTop + 65, 85, 20).build());

        // Cancel button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + guiWidth - 95, guiTop + 65, 85, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xCC000000);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + guiWidth - 1, guiTop + guiHeight - 1, 0xCC442222);

        // Title
        graphics.drawCenteredString(this.font, "Delete Shop?", guiLeft + guiWidth / 2, guiTop + 10, 0xFF5555);

        // Warning text
        graphics.drawCenteredString(this.font, "Are you sure you want to delete", guiLeft + guiWidth / 2, guiTop + 28, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "\"" + shop.shopName() + "\"?", guiLeft + guiWidth / 2, guiTop + 40, 0xFFD700);
        graphics.drawCenteredString(this.font, "This cannot be undone!", guiLeft + guiWidth / 2, guiTop + 52, 0xFF5555);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
