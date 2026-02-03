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
    private int guiWidth = 200;
    private int guiHeight = 100;

    public CreateShopScreen(Screen parent) {
        super(Component.literal("Create Shop"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;

        nameBox = new EditBox(this.font, guiLeft + 10, guiTop + 35, guiWidth - 20, 20, Component.literal("Shop Name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("Enter shop name..."));
        this.addRenderableWidget(nameBox);

        createButton = Button.builder(Component.literal("Create"), btn -> {
            String name = nameBox.getValue().trim();
            if (!name.isEmpty()) {
                NetworkHandler.sendToServer(new CreateShopPacket(name));
                minecraft.setScreen(parent);
            }
        }).bounds(guiLeft + 10, guiTop + 65, 85, 20).build();
        this.addRenderableWidget(createButton);

        cancelButton = Button.builder(Component.literal("Cancel"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + guiWidth - 95, guiTop + 65, 85, 20).build();
        this.addRenderableWidget(cancelButton);

        setInitialFocus(nameBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xCC000000);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + guiWidth - 1, guiTop + guiHeight - 1, 0xCC222222);

        // Title
        graphics.drawCenteredString(this.font, "Create New Shop", guiLeft + guiWidth / 2, guiTop + 10, 0xFFFFFF);

        // Label
        graphics.drawString(this.font, "Shop Name:", guiLeft + 10, guiTop + 25, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && nameBox.isFocused()) { // Enter key
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
