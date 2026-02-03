package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.shop.ShopManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record DeleteShopPacket(UUID shopId) implements CustomPacketPayload {

    public static final Type<DeleteShopPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "delete_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteShopPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public DeleteShopPacket decode(RegistryFriendlyByteBuf buf) {
                return new DeleteShopPacket(buf.readUUID());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, DeleteShopPacket packet) {
                buf.writeUUID(packet.shopId);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteShopPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (ShopManager.deleteShop(player, packet.shopId)) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.INFO,
                        "Shop deleted successfully!"
                    ));
                    RequestShopsPacket.handle(new RequestShopsPacket(), context);
                } else {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "Could not delete shop!"
                    ));
                }
            }
        });
    }
}
