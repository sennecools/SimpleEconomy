package com.simpleeconomy.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Helper class for playing economy-related sound effects
 */
public class SoundHelper {

    /**
     * Play a coin/purchase sound when buying something
     */
    public static void playPurchaseSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    /**
     * Play a sale notification sound when something is sold from your shop
     */
    public static void playSaleSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    /**
     * Play a payment received sound
     */
    public static void playPaymentReceivedSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    /**
     * Play an error sound
     */
    public static void playErrorSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    /**
     * Play a low stock warning sound
     */
    public static void playLowStockWarningSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.8f);
    }

    /**
     * Play a success/confirmation sound
     */
    public static void playSuccessSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.3f, 1.5f);
    }

    /**
     * Play the starting balance received sound
     */
    public static void playStartingBalanceSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    /**
     * Play the daily reward sound
     */
    public static void playDailyRewardSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    /**
     * Play a small coin drop sound for mob kills
     */
    public static void playMobDropSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 1.4f);
    }

    /**
     * Play coinflip win sound
     */
    public static void playCoinflipWinSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    /**
     * Play coinflip lose sound
     */
    public static void playCoinflipLoseSound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 0.8f);
    }
}
