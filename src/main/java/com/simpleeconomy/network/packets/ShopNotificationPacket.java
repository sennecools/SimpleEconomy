package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShopNotificationPacket(NotificationType notificationType, String message) implements CustomPacketPayload {

    public static final Type<ShopNotificationPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "shop_notification"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopNotificationPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public ShopNotificationPacket decode(RegistryFriendlyByteBuf buf) {
                return new ShopNotificationPacket(
                    NotificationType.values()[buf.readVarInt()],
                    buf.readUtf()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ShopNotificationPacket packet) {
                buf.writeVarInt(packet.notificationType.ordinal());
                buf.writeUtf(packet.message);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShopNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(packet.message), false);
            }
        });
    }

    public enum NotificationType {
        SALE,           // Someone bought from your shop
        PURCHASE,       // You successfully purchased
        ERROR,          // Something went wrong
        INFO            // General information
    }
}
