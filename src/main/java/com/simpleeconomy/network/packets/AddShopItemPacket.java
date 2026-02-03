package com.simpleeconomy.network.packets;

import com.simpleeconomy.SimpleEconomy;
import com.simpleeconomy.network.NetworkHandler;
import com.simpleeconomy.shop.ShopManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record AddShopItemPacket(UUID shopId, ItemStack itemStack, double price) implements CustomPacketPayload {

    public static final Type<AddShopItemPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleEconomy.MOD_ID, "add_shop_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddShopItemPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public AddShopItemPacket decode(RegistryFriendlyByteBuf buf) {
                return new AddShopItemPacket(
                    buf.readUUID(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readDouble()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, AddShopItemPacket packet) {
                buf.writeUUID(packet.shopId);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.itemStack);
                buf.writeDouble(packet.price);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AddShopItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Check if player actually has the item in their inventory
                ItemStack heldItem = packet.itemStack.copy();
                boolean hasItem = false;

                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (ItemStack.isSameItemSameComponents(invStack, heldItem) && invStack.getCount() >= heldItem.getCount()) {
                        hasItem = true;
                        // Remove item from inventory
                        invStack.shrink(heldItem.getCount());
                        break;
                    }
                }

                if (!hasItem) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "You don't have that item in your inventory!"
                    ));
                    return;
                }

                if (ShopManager.addItemToShop(player, packet.shopId, heldItem, packet.price)) {
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.INFO,
                        "Item added to shop for " + String.format("%.2f", packet.price) + " coins!"
                    ));
                    RequestShopsPacket.handle(new RequestShopsPacket(), context);
                } else {
                    // Return item to player if adding failed
                    player.getInventory().add(heldItem);
                    NetworkHandler.sendToPlayer(player, new ShopNotificationPacket(
                        ShopNotificationPacket.NotificationType.ERROR,
                        "Could not add item to shop!"
                    ));
                }
            }
        });
    }
}
