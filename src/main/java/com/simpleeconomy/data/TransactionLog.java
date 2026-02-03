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
import java.util.stream.Collectors;

public class TransactionLog extends SavedData {

    private static final String DATA_NAME = SimpleEconomy.MOD_ID + "_transactions";
    private static final int MAX_TRANSACTIONS_PER_PLAYER = 50;

    private final Map<UUID, List<Transaction>> playerTransactions = new HashMap<>();

    public TransactionLog() {
    }

    public static TransactionLog load(CompoundTag tag, HolderLookup.Provider provider) {
        TransactionLog log = new TransactionLog();

        CompoundTag transactionsTag = tag.getCompound("transactions");
        for (String key : transactionsTag.getAllKeys()) {
            try {
                UUID playerUUID = UUID.fromString(key);
                List<Transaction> transactions = new ArrayList<>();
                ListTag list = transactionsTag.getList(key, Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    transactions.add(Transaction.load(list.getCompound(i)));
                }
                log.playerTransactions.put(playerUUID, transactions);
            } catch (IllegalArgumentException e) {
                SimpleEconomy.LOGGER.warn("Invalid UUID in transaction log: {}", key);
            }
        }

        return log;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag transactionsTag = new CompoundTag();
        for (Map.Entry<UUID, List<Transaction>> entry : playerTransactions.entrySet()) {
            ListTag list = new ListTag();
            for (Transaction transaction : entry.getValue()) {
                list.add(transaction.save());
            }
            transactionsTag.put(entry.getKey().toString(), list);
        }
        tag.put("transactions", transactionsTag);
        return tag;
    }

    public void addTransaction(UUID playerUUID, Transaction transaction) {
        List<Transaction> transactions = playerTransactions.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        transactions.add(0, transaction); // Add at beginning (newest first)

        // Trim to max size
        while (transactions.size() > MAX_TRANSACTIONS_PER_PLAYER) {
            transactions.remove(transactions.size() - 1);
        }

        setDirty();
    }

    public List<Transaction> getTransactions(UUID playerUUID) {
        return new ArrayList<>(playerTransactions.getOrDefault(playerUUID, new ArrayList<>()));
    }

    public List<Transaction> getRecentTransactions(UUID playerUUID, int count) {
        List<Transaction> all = playerTransactions.getOrDefault(playerUUID, new ArrayList<>());
        return all.stream().limit(count).collect(Collectors.toList());
    }

    public static TransactionLog get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            new Factory<>(TransactionLog::new, TransactionLog::load),
            DATA_NAME
        );
    }

    public record Transaction(
        TransactionType type,
        double amount,
        String description,
        long timestamp,
        UUID otherParty // The other player involved, if any
    ) {
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type.name());
            tag.putDouble("amount", amount);
            tag.putString("description", description);
            tag.putLong("timestamp", timestamp);
            if (otherParty != null) {
                tag.putUUID("otherParty", otherParty);
            }
            return tag;
        }

        public static Transaction load(CompoundTag tag) {
            return new Transaction(
                TransactionType.valueOf(tag.getString("type")),
                tag.getDouble("amount"),
                tag.getString("description"),
                tag.getLong("timestamp"),
                tag.contains("otherParty") ? tag.getUUID("otherParty") : null
            );
        }

        public static Transaction purchase(double amount, String itemName, UUID shopOwner) {
            return new Transaction(
                TransactionType.PURCHASE,
                -amount,
                "Bought " + itemName,
                System.currentTimeMillis(),
                shopOwner
            );
        }

        public static Transaction sale(double amount, String itemName, UUID buyer) {
            return new Transaction(
                TransactionType.SALE,
                amount,
                "Sold " + itemName,
                System.currentTimeMillis(),
                buyer
            );
        }

        public static Transaction payment(double amount, UUID recipient, String recipientName) {
            return new Transaction(
                TransactionType.PAYMENT_SENT,
                -amount,
                "Paid " + recipientName,
                System.currentTimeMillis(),
                recipient
            );
        }

        public static Transaction received(double amount, UUID sender, String senderName) {
            return new Transaction(
                TransactionType.PAYMENT_RECEIVED,
                amount,
                "From " + senderName,
                System.currentTimeMillis(),
                sender
            );
        }

        public static Transaction adminAdd(double amount) {
            return new Transaction(
                TransactionType.ADMIN_ADD,
                amount,
                "Admin granted",
                System.currentTimeMillis(),
                null
            );
        }

        public static Transaction adminRemove(double amount) {
            return new Transaction(
                TransactionType.ADMIN_REMOVE,
                -amount,
                "Admin removed",
                System.currentTimeMillis(),
                null
            );
        }

        public static Transaction tax(double amount) {
            return new Transaction(
                TransactionType.TAX,
                -amount,
                "Transaction tax",
                System.currentTimeMillis(),
                null
            );
        }
    }

    public enum TransactionType {
        PURCHASE,
        SALE,
        PAYMENT_SENT,
        PAYMENT_RECEIVED,
        ADMIN_ADD,
        ADMIN_REMOVE,
        TAX
    }
}
