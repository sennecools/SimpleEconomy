package com.simpleeconomy.shop;

import net.minecraft.ChatFormatting;

public enum ShopCategory {
    GENERAL("General", ChatFormatting.WHITE, "All items"),
    TOOLS("Tools", ChatFormatting.YELLOW, "Tools and equipment"),
    WEAPONS("Weapons", ChatFormatting.RED, "Swords, bows, and more"),
    ARMOR("Armor", ChatFormatting.BLUE, "Protective gear"),
    FOOD("Food", ChatFormatting.GREEN, "Consumables and food"),
    BLOCKS("Blocks", ChatFormatting.GRAY, "Building materials"),
    REDSTONE("Redstone", ChatFormatting.DARK_RED, "Redstone components"),
    MATERIALS("Materials", ChatFormatting.GOLD, "Crafting materials"),
    MAGIC("Magic", ChatFormatting.DARK_PURPLE, "Enchanted items and potions"),
    MODDED("Modded", ChatFormatting.AQUA, "Items from mods");

    private final String displayName;
    private final ChatFormatting color;
    private final String description;

    ShopCategory(String displayName, ChatFormatting color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public static ShopCategory fromName(String name) {
        for (ShopCategory category : values()) {
            if (category.name().equalsIgnoreCase(name) || category.displayName.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return GENERAL;
    }
}
