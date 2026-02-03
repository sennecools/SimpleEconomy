package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.gui.ShopBrowserScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenShopBrowserPacket() implements CustomPacketPayload {

    public static final Type<OpenShopBrowserPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "open_shop_browser"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopBrowserPacket> STREAM_CODEC =
        StreamCodec.unit(new OpenShopBrowserPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenShopBrowserPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new ShopBrowserScreen());
        });
    }
}
