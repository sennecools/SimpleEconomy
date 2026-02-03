package com.simpleeconomy.network;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.network.packets.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {

    public static void register() {
        // Registration happens via event
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(SimpleEconomy.MOD_ID).versioned("1.0.0");

        // Server to Client packets
        registrar.playToClient(
            OpenShopBrowserPacket.TYPE,
            OpenShopBrowserPacket.STREAM_CODEC,
            OpenShopBrowserPacket::handle
        );

        registrar.playToClient(
            SyncShopsPacket.TYPE,
            SyncShopsPacket.STREAM_CODEC,
            SyncShopsPacket::handle
        );

        registrar.playToClient(
            SyncBalancePacket.TYPE,
            SyncBalancePacket.STREAM_CODEC,
            SyncBalancePacket::handle
        );

        registrar.playToClient(
            ShopNotificationPacket.TYPE,
            ShopNotificationPacket.STREAM_CODEC,
            ShopNotificationPacket::handle
        );

        // Client to Server packets
        registrar.playToServer(
            RequestShopsPacket.TYPE,
            RequestShopsPacket.STREAM_CODEC,
            RequestShopsPacket::handle
        );

        registrar.playToServer(
            CreateShopPacket.TYPE,
            CreateShopPacket.STREAM_CODEC,
            CreateShopPacket::handle
        );

        registrar.playToServer(
            DeleteShopPacket.TYPE,
            DeleteShopPacket.STREAM_CODEC,
            DeleteShopPacket::handle
        );

        registrar.playToServer(
            AddShopItemPacket.TYPE,
            AddShopItemPacket.STREAM_CODEC,
            AddShopItemPacket::handle
        );

        registrar.playToServer(
            RemoveShopItemPacket.TYPE,
            RemoveShopItemPacket.STREAM_CODEC,
            RemoveShopItemPacket::handle
        );

        registrar.playToServer(
            PurchaseItemPacket.TYPE,
            PurchaseItemPacket.STREAM_CODEC,
            PurchaseItemPacket::handle
        );

        registrar.playToServer(
            ToggleFavoritePacket.TYPE,
            ToggleFavoritePacket.STREAM_CODEC,
            ToggleFavoritePacket::handle
        );

        registrar.playToServer(
            UpdateShopPacket.TYPE,
            UpdateShopPacket.STREAM_CODEC,
            UpdateShopPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
