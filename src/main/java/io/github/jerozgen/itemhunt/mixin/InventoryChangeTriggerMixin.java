package io.github.jerozgen.itemhunt.mixin;

import io.github.jerozgen.itemhunt.event.InventoryChangedEvent;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(InventoryChangeTrigger.class)
public class InventoryChangeTriggerMixin {
    @Inject(at = @At("HEAD"), method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V")
    void onTrigger(ServerPlayer player, Inventory inventory, ItemStack stack, CallbackInfo ci) {
        try (var invokers = Stimuli.select().forEntity(player)) {
            invokers.get(InventoryChangedEvent.EVENT).onInventoryChanged(player, inventory, stack);
        }
    }
}
