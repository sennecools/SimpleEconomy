package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopItem;
import com.simpleeconomy.shop.ShopManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record RemoveShopItemPacket(UUID shopId, UUID itemId) implements CustomPacketPayload {

    public static final Type<RemoveShopItemPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "remove_shop_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveShopItemPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public RemoveShopItemPacket decode(RegistryFriendlyByteBuf buf) {
                return new RemoveShopItemPacket(buf.readUUID(), buf.readUUID());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, RemoveShopItemPacket packet) {
                buf.writeUUID(packet.shopId);
                buf.writeUUID(packet.itemId);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoveShopItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShopSavedData data = ShopSavedData.get(player.getServer());
                Shop shop = data.getShop(packet.shopId);

                if (shop == null || !shop.getOwnerUUID().equals(player.getUUID())) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "You can only remove items from your own shop!"
                    ));
                    return;
                }

                ShopItem item = shop.getItem(packet.itemId);
                if (item == null) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "Item not found!"
                    ));
                    return;
                }

                // Return remaining stock to player
                if (item.getStock() > 0) {
                    ItemStack returnStack = item.getItemStack().copy();
                    returnStack.setCount(item.getStock() * item.getItemStack().getCount());
                    if (!player.getInventory().add(returnStack)) {
                        player.drop(returnStack, false);
                    }
                }

                if (ShopManager.removeItemFromShop(player, packet.shopId, packet.itemId)) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.INFO,
                        "Item removed from shop!"
                    ));
                    RequestShopsPacket.handle(new RequestShopsPacket(), context);
                }
            }
        });
    }
}
