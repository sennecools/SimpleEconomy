package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.gui.ClientShopData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncBalancePacket(double balance) implements CustomPacketPayload {

    public static final Type<SyncBalancePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "sync_balance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBalancePacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public SyncBalancePacket decode(RegistryFriendlyByteBuf buf) {
                return new SyncBalancePacket(buf.readDouble());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, SyncBalancePacket packet) {
                buf.writeDouble(packet.balance);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncBalancePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientShopData.setBalance(packet.balance);
        });
    }
}
