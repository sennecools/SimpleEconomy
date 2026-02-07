package com.simpleeconomy.data;

import com.simpleeconomy.SimpleEconomy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

/**
 * Stores per-player data like starting balance received flag and offline sale notifications
 */
public class PlayerDataSavedData extends SavedData {

    private static final String DATA_NAME = SimpleEconomy.MOD_ID + "_playerdata";

    // Players who have received their starting balance
    private final Set<UUID> receivedStartingBalance = new HashSet<>();

    // Pending offline sale notifications: playerUUID -> list of (saleCount, totalCoins)
    private final Map<UUID, OfflineSalesSummary> pendingOfflineSales = new HashMap<>();

    public PlayerDataSavedData() {
    }

    public static PlayerDataSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PlayerDataSavedData data = new PlayerDataSavedData();

        // Load starting balance flags
        ListTag startingBalanceList = tag.getList("receivedStartingBalance", Tag.TAG_COMPOUND);
        for (int i = 0; i < startingBalanceList.size(); i++) {
            CompoundTag entry = startingBalanceList.getCompound(i);
            data.receivedStartingBalance.add(entry.getUUID("uuid"));
        }

        // Load pending offline sales
        CompoundTag offlineSalesTag = tag.getCompound("pendingOfflineSales");
        for (String key : offlineSalesTag.getAllKeys()) {
            try {
                UUID playerUUID = UUID.fromString(key);
                CompoundTag salesTag = offlineSalesTag.getCompound(key);
                int saleCount = salesTag.getInt("saleCount");
                double totalCoins = salesTag.getDouble("totalCoins");
                data.pendingOfflineSales.put(playerUUID, new OfflineSalesSummary(saleCount, totalCoins));
            } catch (IllegalArgumentException e) {
                SimpleEconomy.LOGGER.warn("Invalid UUID in player data: {}", key);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        // Save starting balance flags
        ListTag startingBalanceList = new ListTag();
        for (UUID uuid : receivedStartingBalance) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            startingBalanceList.add(entry);
        }
        tag.put("receivedStartingBalance", startingBalanceList);

        // Save pending offline sales
        CompoundTag offlineSalesTag = new CompoundTag();
        for (Map.Entry<UUID, OfflineSalesSummary> entry : pendingOfflineSales.entrySet()) {
            CompoundTag salesTag = new CompoundTag();
            salesTag.putInt("saleCount", entry.getValue().saleCount());
            salesTag.putDouble("totalCoins", entry.getValue().totalCoins());
            offlineSalesTag.put(entry.getKey().toString(), salesTag);
        }
        tag.put("pendingOfflineSales", offlineSalesTag);

        return tag;
    }

    // Starting balance methods
    public boolean hasReceivedStartingBalance(UUID playerUUID) {
        return receivedStartingBalance.contains(playerUUID);
    }

    public void markStartingBalanceReceived(UUID playerUUID) {
        receivedStartingBalance.add(playerUUID);
        setDirty();
    }

    // Offline sales methods
    public void addOfflineSale(UUID sellerUUID, double amount) {
        OfflineSalesSummary existing = pendingOfflineSales.getOrDefault(sellerUUID, new OfflineSalesSummary(0, 0));
        pendingOfflineSales.put(sellerUUID, new OfflineSalesSummary(
            existing.saleCount() + 1,
            existing.totalCoins() + amount
        ));
        setDirty();
    }

    public OfflineSalesSummary getAndClearOfflineSales(UUID playerUUID) {
        OfflineSalesSummary summary = pendingOfflineSales.remove(playerUUID);
        if (summary != null) {
            setDirty();
        }
        return summary;
    }

    public boolean hasOfflineSales(UUID playerUUID) {
        return pendingOfflineSales.containsKey(playerUUID);
    }

    public static PlayerDataSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            new Factory<>(PlayerDataSavedData::new, PlayerDataSavedData::load),
            DATA_NAME
        );
    }

    public record OfflineSalesSummary(int saleCount, double totalCoins) {}
}
