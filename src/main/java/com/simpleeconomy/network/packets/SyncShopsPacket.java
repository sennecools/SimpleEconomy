package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.gui.ClientShopData;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopCategory;
import com.simpleeconomy.shop.ShopItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SyncShopsPacket(List<ShopData> shops, Set<UUID> favorites) implements CustomPacketPayload {

    public static final Type<SyncShopsPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "sync_shops"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncShopsPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public SyncShopsPacket decode(RegistryFriendlyByteBuf buf) {
                int shopCount = buf.readVarInt();
                List<ShopData> shops = new ArrayList<>();
                for (int i = 0; i < shopCount; i++) {
                    shops.add(ShopData.decode(buf));
                }
                int favCount = buf.readVarInt();
                Set<UUID> favorites = new java.util.HashSet<>();
                for (int i = 0; i < favCount; i++) {
                    favorites.add(buf.readUUID());
                }
                return new SyncShopsPacket(shops, favorites);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, SyncShopsPacket packet) {
                buf.writeVarInt(packet.shops.size());
                for (ShopData shop : packet.shops) {
                    shop.encode(buf);
                }
                buf.writeVarInt(packet.favorites.size());
                for (UUID id : packet.favorites) {
                    buf.writeUUID(id);
                }
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncShopsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientShopData.setShops(packet.shops, packet.favorites);
        });
    }

    public record ShopData(
        UUID shopId,
        UUID ownerUUID,
        String ownerName,
        String shopName,
        String description,
        ShopCategory category,
        long totalSales,
        double totalRevenue,
        boolean featured,
        List<ItemData> items
    ) {
        public static ShopData fromShop(Shop shop) {
            List<ItemData> items = new ArrayList<>();
            for (ShopItem item : shop.getItems()) {
                items.add(ItemData.fromShopItem(item));
            }
            return new ShopData(
                shop.getShopId(),
                shop.getOwnerUUID(),
                shop.getOwnerName(),
                shop.getShopName(),
                shop.getDescription(),
                shop.getCategory(),
                shop.getTotalSales(),
                shop.getTotalRevenue(),
                shop.isFeatured(),
                items
            );
        }

        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeUUID(shopId);
            buf.writeUUID(ownerUUID);
            buf.writeUtf(ownerName);
            buf.writeUtf(shopName);
            buf.writeUtf(description);
            buf.writeUtf(category.name());
            buf.writeVarLong(totalSales);
            buf.writeDouble(totalRevenue);
            buf.writeBoolean(featured);
            buf.writeVarInt(items.size());
            for (ItemData item : items) {
                item.encode(buf);
            }
        }

        public static ShopData decode(RegistryFriendlyByteBuf buf) {
            UUID shopId = buf.readUUID();
            UUID ownerUUID = buf.readUUID();
            String ownerName = buf.readUtf();
            String shopName = buf.readUtf();
            String description = buf.readUtf();
            ShopCategory category = ShopCategory.valueOf(buf.readUtf());
            long totalSales = buf.readVarLong();
            double totalRevenue = buf.readDouble();
            boolean featured = buf.readBoolean();
            int itemCount = buf.readVarInt();
            List<ItemData> items = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                items.add(ItemData.decode(buf));
            }
            return new ShopData(shopId, ownerUUID, ownerName, shopName, description, category, totalSales, totalRevenue, featured, items);
        }
    }

    public record ItemData(
        UUID itemId,
        ItemStack itemStack,
        double price,
        int stock
    ) {
        public static ItemData fromShopItem(ShopItem item) {
            return new ItemData(item.getItemId(), item.getItemStack(), item.getPrice(), item.getStock());
        }

        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeUUID(itemId);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, itemStack);
            buf.writeDouble(price);
            buf.writeVarInt(stock);
        }

        public static ItemData decode(RegistryFriendlyByteBuf buf) {
            UUID itemId = buf.readUUID();
            ItemStack itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            double price = buf.readDouble();
            int stock = buf.readVarInt();
            return new ItemData(itemId, itemStack, price, stock);
        }
    }
}
