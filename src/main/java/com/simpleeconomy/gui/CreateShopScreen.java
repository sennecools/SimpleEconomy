package com.simpleeconomy.gui;

import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.network.packets.CreateShopPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CreateShopScreen extends Screen {

    private final Screen parent;
    private EditBox nameBox;
    private Button createButton;
    private Button cancelButton;

    private int guiLeft;
    private int guiTop;
    private int guiWidth = 176;
    private int guiHeight = 80;

    public CreateShopScreen(Screen parent) {
        super(Component.literal("Create Shop"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;

        nameBox = new EditBox(this.font, guiLeft + 8, guiTop + 30, guiWidth - 16, 16, Component.literal("Shop Name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("Enter shop name..."));
        this.addRenderableWidget(nameBox);

        createButton = Button.builder(Component.literal("Create"), btn -> {
            String name = nameBox.getValue().trim();
            if (!name.isEmpty()) {
                NetworkHandler.sendToServer(new CreateShopPacket(name));
                minecraft.setScreen(parent);
            }
        }).bounds(guiLeft + 8, guiTop + 54, 76, 18).build();
        this.addRenderableWidget(createButton);

        cancelButton = Button.builder(Component.literal("Cancel"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + guiWidth - 84, guiTop + 54, 76, 18).build();
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
        graphics.drawString(this.font, "Create New Shop", guiLeft + 8, guiTop + 8, 0x404040, false);

        // Label
        graphics.drawString(this.font, "Shop Name:", guiLeft + 8, guiTop + 20, 0x404040, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && nameBox.isFocused()) {
            String name = nameBox.getValue().trim();
            if (!name.isEmpty()) {
                NetworkHandler.sendToServer(new CreateShopPacket(name));
                minecraft.setScreen(parent);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
