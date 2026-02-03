package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record PurchaseItemPacket(UUID shopId, UUID itemId, int quantity) implements CustomPacketPayload {

    public static final Type<PurchaseItemPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "purchase_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseItemPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public PurchaseItemPacket decode(RegistryFriendlyByteBuf buf) {
                return new PurchaseItemPacket(buf.readUUID(), buf.readUUID(), buf.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, PurchaseItemPacket packet) {
                buf.writeUUID(packet.shopId);
                buf.writeUUID(packet.itemId);
                buf.writeVarInt(packet.quantity);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PurchaseItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShopSavedData data = ShopSavedData.get(player.getServer());
                Shop shop = data.getShop(packet.shopId);

                if (shop == null) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "Shop not found!"
                    ));
                    return;
                }

                // Can't buy from own shop
                if (shop.getOwnerUUID().equals(player.getUUID())) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "You can't buy from your own shop!"
                    ));
                    return;
                }

                ShopManager.PurchaseResult result = ShopManager.purchaseItem(player, packet.shopId, packet.itemId, packet.quantity);

                if (result.success()) {
                    // Give item to buyer
                    if (!player.getInventory().add(result.item())) {
                        player.drop(result.item(), false);
                    }

                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.PURCHASE,
                        "Purchased for " + EconomyManager.formatBalance(result.price()) + " coins!"
                    ));

                    // Notify seller if online
                    ServerPlayer seller = player.getServer().getPlayerList().getPlayer(shop.getOwnerUUID());
                    if (seller != null) {
                        double taxedAmount = result.price() - EconomyManager.calculateTax(result.price());
                        NetworkHandler.sendToPlayer(seller, new ShopNotificationPacket(
                            ShopNotificationPacket.NotificationType.SALE,
                            player.getName().getString() + " bought from your shop! +" +
                                EconomyManager.formatBalance(taxedAmount) + " coins"
                        ));
                        // Sync seller's balance
                        NetworkHandler.sendToPlayer(seller, new SyncBalancePacket(EconomyManager.getBalance(seller)));
                    }

                    // Refresh shops and balance for buyer
                    RequestShopsPacket.handle(new RequestShopsPacket(), context);
                } else {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        result.message()
                    ));
                }
            }
        });
    }
}
