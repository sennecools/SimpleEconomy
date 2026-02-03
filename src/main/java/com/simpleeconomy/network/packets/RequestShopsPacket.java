package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.data.ShopSavedData;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.shop.Shop;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record RequestShopsPacket() implements CustomPacketPayload {

    public static final Type<RequestShopsPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "request_shops"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestShopsPacket> STREAM_CODEC =
        StreamCodec.unit(new RequestShopsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestShopsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShopSavedData data = ShopSavedData.get(player.getServer());
                List<Shop> shops = data.getAllShops();

                List<SyncShopsPacket.ShopData> shopDataList = shops.stream()
                    .map(SyncShopsPacket.ShopData::fromShop)
                    .collect(Collectors.toList());

                Set<UUID> favorites = new HashSet<>(data.getFavoriteShops(player.getUUID()).stream()
                    .map(Shop::getShopId)
                    .collect(Collectors.toSet()));

                NetworkHandler.sendToPlayer(player, new SyncShopsPacket(shopDataList, favorites));
                NetworkHandler.sendToPlayer(player, new SyncBalancePacket(EconomyManager.getBalance(player)));
            }
        });
    }
}
