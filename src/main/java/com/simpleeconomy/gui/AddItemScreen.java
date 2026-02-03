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
    private int guiWidth = 200;
    private int guiHeight = 180;

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

        priceBox = new EditBox(this.font, guiLeft + 60, guiTop + 130, 80, 16, Component.literal("Price"));
        priceBox.setMaxLength(10);
        priceBox.setValue("1");
        priceBox.setFilter(s -> s.isEmpty() || s.matches("\\d*\\.?\\d*"));
        this.addRenderableWidget(priceBox);

        addButton = Button.builder(Component.literal("Add"), btn -> {
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
        }).bounds(guiLeft + 10, guiTop + 150, 85, 20).build();
        this.addRenderableWidget(addButton);

        cancelButton = Button.builder(Component.literal("Cancel"), btn -> {
            minecraft.setScreen(parent);
        }).bounds(guiLeft + guiWidth - 95, guiTop + 150, 85, 20).build();
        this.addRenderableWidget(cancelButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xCC000000);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + guiWidth - 1, guiTop + guiHeight - 1, 0xCC222222);

        // Title
        graphics.drawCenteredString(this.font, "Add Item to Shop", guiLeft + guiWidth / 2, guiTop + 8, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Click an item from your inventory", guiLeft + guiWidth / 2, guiTop + 20, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Render inventory
        Inventory inv = minecraft.player.getInventory();
        int invStartX = guiLeft + 10;
        int invStartY = guiTop + 35;
        int slotSize = 20;

        // Main inventory (27 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;
                int x = invStartX + col * slotSize;
                int y = invStartY + row * slotSize;
                renderInventorySlot(graphics, inv.getItem(slot), slot, x, y, slotSize, mouseX, mouseY);
            }
        }

        // Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            int x = invStartX + col * slotSize;
            int y = invStartY + 68;
            renderInventorySlot(graphics, inv.getItem(col), col, x, y, slotSize, mouseX, mouseY);
        }

        // Price label
        graphics.drawString(this.font, "Price:", guiLeft + 10, guiTop + 132, 0xAAAAAA);

        // Selected item preview
        if (selectedSlot >= 0) {
            ItemStack selected = inv.getItem(selectedSlot);
            if (!selected.isEmpty()) {
                graphics.drawString(this.font, "Selected:", guiLeft + 10, guiTop + 115, 0x55FF55);
                graphics.renderItem(selected, guiLeft + 60, guiTop + 110);
                graphics.drawString(this.font, "x" + selected.getCount(), guiLeft + 80, guiTop + 115, 0xFFFFFF);
            }
        }

        // Tooltip for hovered inventory slot
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;
                int x = invStartX + col * slotSize;
                int y = invStartY + row * slotSize;
                if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                    ItemStack stack = inv.getItem(slot);
                    if (!stack.isEmpty()) {
                        graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                    }
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            int x = invStartX + col * slotSize;
            int y = invStartY + 68;
            if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
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

        // Background
        int bgColor = selected ? 0xFF446644 : (hovered ? 0xFF444444 : 0xFF333333);
        graphics.fill(x, y, x + size, y + size, bgColor);

        // Border
        int borderColor = selected ? 0xFF55FF55 : 0xFF555555;
        graphics.fill(x, y, x + size, y + 1, borderColor);
        graphics.fill(x, y + size - 1, x + size, y + size, borderColor);
        graphics.fill(x, y, x + 1, y + size, borderColor);
        graphics.fill(x + size - 1, y, x + size, y + size, borderColor);

        // Item
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 2, y + 2);
            graphics.renderItemDecorations(this.font, stack, x + 2, y + 2);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        Inventory inv = minecraft.player.getInventory();
        int invStartX = guiLeft + 10;
        int invStartY = guiTop + 35;
        int slotSize = 20;

        // Check main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;
                int x = invStartX + col * slotSize;
                int y = invStartY + row * slotSize;
                if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                    if (!inv.getItem(slot).isEmpty()) {
                        selectedSlot = slot;
                        return true;
                    }
                }
            }
        }

        // Check hotbar
        for (int col = 0; col < 9; col++) {
            int x = invStartX + col * slotSize;
            int y = invStartY + 68;
            if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
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
