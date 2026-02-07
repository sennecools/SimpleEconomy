package com.simpleeconomy.mixin;

import com.simpleeconomy.menu.ShopBrowserMenu;
import com.simpleeconomy.menu.ShopViewMenu;
import com.simpleeconomy.menu.MyShopMenu;
import com.simpleeconomy.menu.AddItemMenu;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ContainerClickMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        AbstractContainerMenu menu = player.containerMenu;

        // Check if this is one of our custom menus
        boolean isCustomMenu = menu instanceof ShopBrowserMenu ||
            menu instanceof ShopViewMenu ||
            menu instanceof MyShopMenu ||
            menu instanceof AddItemMenu;

        if (isCustomMenu && menu.containerId == packet.getContainerId()) {
            // Clear the carried item on server side
            menu.setCarried(ItemStack.EMPTY);

            // Call our clicked method directly
            menu.clicked(packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(), player);

            // Force full sync to client
            menu.sendAllDataToRemote();

            // Cancel the vanilla handling
            ci.cancel();
        }
    }
}
