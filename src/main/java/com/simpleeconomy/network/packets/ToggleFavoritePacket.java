package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.network.NetworkHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record ToggleFavoritePacket(UUID shopId) implements CustomPacketPayload {

    public static final Type<ToggleFavoritePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "toggle_favorite"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleFavoritePacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public ToggleFavoritePacket decode(RegistryFriendlyByteBuf buf) {
                return new ToggleFavoritePacket(buf.readUUID());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ToggleFavoritePacket packet) {
                buf.writeUUID(packet.shopId);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleFavoritePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShopSavedData data = ShopSavedData.get(player.getServer());

                if (data.isFavorite(player.getUUID(), packet.shopId)) {
                    data.removeFavorite(player.getUUID(), packet.shopId);
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.INFO,
                        "Shop removed from favorites"
                    ));
                } else {
                    data.addFavorite(player.getUUID(), packet.shopId);
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.INFO,
                        "Shop added to favorites!"
                    ));
                }

                RequestShopsPacket.handle(new RequestShopsPacket(), context);
            }
        });
    }
}
