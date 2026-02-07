package com.simpleeconomy.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility for creating player head items
 */
public class HeadUtil {

    /**
     * Create a player head item for the given player UUID and name
     */
    public static ItemStack createPlayerHead(UUID playerUUID, String playerName, MinecraftServer server) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);

        // Create a GameProfile with the player's UUID and name
        GameProfile profile = new GameProfile(playerUUID, playerName);

        // Try to get the full profile with skin data from the server's cache
        if (server != null) {
            Optional<GameProfile> cachedProfile = server.getProfileCache().get(playerUUID);
            if (cachedProfile.isPresent()) {
                profile = cachedProfile.get();
            }
        }

        // Set the profile on the head
        head.set(DataComponents.PROFILE, new ResolvableProfile(profile));

        return head;
    }

    /**
     * Create a player head item (without server - won't have skin)
     */
    public static ItemStack createPlayerHead(UUID playerUUID, String playerName) {
        return createPlayerHead(playerUUID, playerName, null);
    }
}
