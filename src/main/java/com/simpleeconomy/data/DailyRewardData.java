package com.simpleeconomy.data;

import com.simpleeconomy.SimpleEconomy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyRewardData extends SavedData {

    private static final String DATA_NAME = SimpleEconomy.MOD_ID + "_daily_rewards";

    // Player UUID -> their reward data
    private final Map<UUID, PlayerRewardInfo> playerData = new HashMap<>();

    public DailyRewardData() {
    }

    public static DailyRewardData load(CompoundTag tag, HolderLookup.Provider provider) {
        DailyRewardData data = new DailyRewardData();

        CompoundTag playersTag = tag.getCompound("players");
        for (String key : playersTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag playerTag = playersTag.getCompound(key);

                int streak = playerTag.getInt("streak");
                long lastClaimDay = playerTag.getLong("lastClaimDay");
                long lastInterestDay = playerTag.getLong("lastInterestDay");

                data.playerData.put(uuid, new PlayerRewardInfo(streak, lastClaimDay, lastInterestDay));
            } catch (IllegalArgumentException e) {
                SimpleEconomy.LOGGER.warn("Invalid UUID in daily reward data: {}", key);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag playersTag = new CompoundTag();

        for (Map.Entry<UUID, PlayerRewardInfo> entry : playerData.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putInt("streak", entry.getValue().streak);
            playerTag.putLong("lastClaimDay", entry.getValue().lastClaimDay);
            playerTag.putLong("lastInterestDay", entry.getValue().lastInterestDay);
            playersTag.put(entry.getKey().toString(), playerTag);
        }

        tag.put("players", playersTag);
        return tag;
    }

    public PlayerRewardInfo getPlayerInfo(UUID playerUUID) {
        return playerData.computeIfAbsent(playerUUID, k -> new PlayerRewardInfo(0, 0, 0));
    }

    public void updatePlayerInfo(UUID playerUUID, PlayerRewardInfo info) {
        playerData.put(playerUUID, info);
        setDirty();
    }

    public static long getCurrentDay() {
        return LocalDate.now(ZoneId.systemDefault()).toEpochDay();
    }

    public static long getCurrentWeek() {
        // Week number since epoch
        return getCurrentDay() / 7;
    }

    public static DailyRewardData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            new Factory<>(DailyRewardData::new, DailyRewardData::load),
            DATA_NAME
        );
    }

    public static class PlayerRewardInfo {
        public int streak;
        public long lastClaimDay;
        public long lastInterestDay;

        public PlayerRewardInfo(int streak, long lastClaimDay, long lastInterestDay) {
            this.streak = streak;
            this.lastClaimDay = lastClaimDay;
            this.lastInterestDay = lastInterestDay;
        }
    }
}
