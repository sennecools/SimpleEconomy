package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.shop.Shop;
import com.simpleeconomy.shop.ShopCategory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record UpdateShopPacket(UUID shopId, String shopName, String description, ShopCategory category) implements CustomPacketPayload {

    public static final Type<UpdateShopPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "update_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateShopPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public UpdateShopPacket decode(RegistryFriendlyByteBuf buf) {
                return new UpdateShopPacket(
                    buf.readUUID(),
                    buf.readUtf(32),
                    buf.readUtf(256),
                    ShopCategory.valueOf(buf.readUtf())
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, UpdateShopPacket packet) {
                buf.writeUUID(packet.shopId);
                buf.writeUtf(packet.shopName, 32);
                buf.writeUtf(packet.description, 256);
                buf.writeUtf(packet.category.name());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateShopPacket packet, IPayloadContext context) {
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

                if (!shop.getOwnerUUID().equals(player.getUUID()) && !player.hasPermissions(2)) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "You can only edit your own shop!"
                    ));
                    return;
                }

                shop.setShopName(packet.shopName);
                shop.setDescription(packet.description);
                shop.setCategory(packet.category);
                data.setDirty();

                NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                    ShopNotificationPacket.NotificationType.INFO,
                    "Shop updated!"
                ));

                RequestShopsPacket.handle(new RequestShopsPacket(), context);
            }
        });
    }
}
