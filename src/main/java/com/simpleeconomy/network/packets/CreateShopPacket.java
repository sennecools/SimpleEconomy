package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CreateShopPacket(String shopName) implements CustomPacketPayload {

    public static final Type<CreateShopPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "create_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateShopPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public CreateShopPacket decode(RegistryFriendlyByteBuf buf) {
                return new CreateShopPacket(buf.readUtf(32));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, CreateShopPacket packet) {
                buf.writeUtf(packet.shopName, 32);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreateShopPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Shop shop = ShopManager.createShop(player, packet.shopName);
                if (shop != null) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.INFO,
                        "Shop '" + packet.shopName + "' created successfully!"
                    ));
                    // Refresh shops list
                    RequestShopsPacket.handle(new RequestShopsPacket(), context);
                } else {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "Could not create shop! You may have reached the maximum limit."
                    ));
                }
            }
        });
    }
}
