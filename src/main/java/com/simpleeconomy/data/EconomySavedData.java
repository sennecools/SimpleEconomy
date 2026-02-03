package com.simpleeconomy.data;

import com.simpleeconomy.SimpleEconomy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomySavedData extends SavedData {

    private static final String DATA_NAME = SimpleEconomy.MOD_ID + "_economy";

    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomySavedData() {
    }

    public static EconomySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        EconomySavedData data = new EconomySavedData();

        CompoundTag balancesTag = tag.getCompound("balances");
        for (String key : balancesTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                double balance = balancesTag.getDouble(key);
                data.balances.put(uuid, balance);
            } catch (IllegalArgumentException e) {
                SimpleEconomy.LOGGER.warn("Invalid UUID in economy data: {}", key);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag balancesTag = new CompoundTag();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            balancesTag.putDouble(entry.getKey().toString(), entry.getValue());
        }
        tag.put("balances", balancesTag);
        return tag;
    }

    public double getBalance(UUID playerUUID) {
        return balances.getOrDefault(playerUUID, 0.0);
    }

    public void setBalance(UUID playerUUID, double amount) {
        balances.put(playerUUID, amount);
        setDirty();
    }

    public Map<UUID, Double> getAllBalances() {
        return new HashMap<>(balances);
    }

    public static EconomySavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            new Factory<>(EconomySavedData::new, EconomySavedData::load),
            DATA_NAME
        );
    }
}
