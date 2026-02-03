package com.simpleeconomy.gui;

import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.network.packets.AddShopItemPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class AddItemScreen extends Screen {

    private final Screen parent;
    private final UUID shopId;

    private EditBox priceBox;
    private Button addButton;
    private Button cancelButton;

    private int selectedSlot = -1;

    private int guiLeft;
    private int guiTop;
    private int guiWidth = 176;
    private int guiHeight = 166;

    public AddItemScreen(Screen parent, UUID shopId) {
        super(Component.literal("Add Item to Shop"));
        this.parent = parent;
        this.shopId = shopId;
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;

        priceBox = new EditBox(this.font, guiLeft + 50, guiTop + 112, 60, 14, Component.literal("Price"));
        priceBox.setMaxLength(10);
        priceBox.setValue("1");
        priceBox.setFilter(s -> s.isEmpty() || s.matches("\\d*\\.?\\d*"));
        this.addRenderableWidget(priceBox);

        addButton = Button.builder(Component.literal("Add Item"), btn -> {
            if (selectedSlot >= 0) {
                try {
                    double price = Double.parseDouble(priceBox.getValue());
                    if (price > 0) {
                        Inventory inv = minecraft.player.getInventory();
                        ItemStack stack = inv.getItem(selectedSlot);
                        if (!stack.isEmpty()) {
                            NetworkHandler.sendToServer(new AddShopItemPacket(shopId, stack.copy(), price));
                            minecraft.setScreen(parent);
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }).bounds(guiLeft + 8, guiTop + 140, 76, 18).build();
        this.addRenderableWidget(addButton);

        cancelButton = Button.builder(Component.literal("Cancel"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + guiWidth - 84, guiTop + 140, 76, 18).build();
        this.addRenderableWidget(cancelButton);
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
        graphics.drawString(this.font, "Add Item to Shop", guiLeft + 8, guiTop + 6, 0x404040, false);
        graphics.drawString(this.font, "Select an item from your inventory", guiLeft + 8, guiTop + 18, 0x606060, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Inventory area background
        int invAreaX = guiLeft + 8;
        int invAreaY = guiTop + 32;
        int invAreaWidth = 162;
        int invAreaHeight = 72;

        graphics.fill(invAreaX, invAreaY, invAreaX + invAreaWidth, invAreaY + invAreaHeight, 0xFF8B8B8B);
        graphics.fill(invAreaX, invAreaY, invAreaX + invAreaWidth, invAreaY + 1, 0xFF373737);
        graphics.fill(invAreaX, invAreaY, invAreaX + 1, invAreaY + invAreaHeight, 0xFF373737);
        graphics.fill(invAreaX + 1, invAreaY + invAreaHeight - 1, invAreaX + invAreaWidth, invAreaY + invAreaHeight, 0xFFFFFFFF);
        graphics.fill(invAreaX + invAreaWidth - 1, invAreaY + 1, invAreaX + invAreaWidth, invAreaY + invAreaHeight - 1, 0xFFFFFFFF);

        // Render inventory
        Inventory inv = minecraft.player.getInventory();
        int slotSize = 18;
        int slotsX = invAreaX + 1;
        int slotsY = invAreaY + 1;

        // Main inventory (27 slots - 3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;
                int x = slotsX + col * slotSize;
                int y = slotsY + row * slotSize;
                renderInventorySlot(graphics, inv.getItem(slot), slot, x, y, slotSize, mouseX, mouseY);
            }
        }

        // Hotbar (9 slots)
        int hotbarY = slotsY + 58;
        for (int col = 0; col < 9; col++) {
            int x = slotsX + col * slotSize;
            renderInventorySlot(graphics, inv.getItem(col), col, x, hotbarY, slotSize, mouseX, mouseY);
        }

        // Price label
        graphics.drawString(this.font, "Price:", guiLeft + 8, guiTop + 114, 0x404040, false);

        // Selected item preview
        if (selectedSlot >= 0) {
            ItemStack selected = inv.getItem(selectedSlot);
            if (!selected.isEmpty()) {
                graphics.drawString(this.font, "Selected:", guiLeft + 120, guiTop + 114, 0x404040, false);
                graphics.renderItem(selected, guiLeft + 152, guiTop + 110);
            }
        }

        // Tooltips for inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;
                int x = slotsX + col * slotSize;
                int y = slotsY + row * slotSize;
                if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                    ItemStack stack = inv.getItem(slot);
                    if (!stack.isEmpty()) {
                        graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                    }
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            int x = slotsX + col * slotSize;
            if (mouseX >= x && mouseX < x + slotSize && mouseY >= hotbarY && mouseY < hotbarY + slotSize) {
                ItemStack stack = inv.getItem(col);
                if (!stack.isEmpty()) {
                    graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
        }
    }

    private void renderInventorySlot(GuiGraphics graphics, ItemStack stack, int slot, int x, int y, int size, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        boolean selected = slot == selectedSlot;

        // Slot background
        int bgColor = selected ? 0xFF5B8B5B : (hovered ? 0xFFAAAAAA : 0xFF8B8B8B);
        graphics.fill(x, y, x + size, y + size, bgColor);

        // Item
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }

        // Selection border
        if (selected) {
            graphics.fill(x, y, x + size, y + 1, 0xFF00AA00);
            graphics.fill(x, y + size - 1, x + size, y + size, 0xFF00AA00);
            graphics.fill(x, y, x + 1, y + size, 0xFF00AA00);
            graphics.fill(x + size - 1, y, x + size, y + size, 0xFF00AA00);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        Inventory inv = minecraft.player.getInventory();
        int invAreaX = guiLeft + 8;
        int invAreaY = guiTop + 32;
        int slotSize = 18;
        int slotsX = invAreaX + 1;
        int slotsY = invAreaY + 1;

        // Check main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;
                int x = slotsX + col * slotSize;
                int y = slotsY + row * slotSize;
                if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                    if (!inv.getItem(slot).isEmpty()) {
                        selectedSlot = slot;
                        return true;
                    }
                }
            }
        }

        // Check hotbar
        int hotbarY = slotsY + 58;
        for (int col = 0; col < 9; col++) {
            int x = slotsX + col * slotSize;
            if (mouseX >= x && mouseX < x + slotSize && mouseY >= hotbarY && mouseY < hotbarY + slotSize) {
                if (!inv.getItem(col).isEmpty()) {
                    selectedSlot = col;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
