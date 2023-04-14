package io.github.jerozgen.itemhunt.event;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface InventoryChangedEvent {
    StimulusEvent<InventoryChangedEvent> EVENT = StimulusEvent.create(InventoryChangedEvent.class, ctx -> (player, inventory, stack) -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onInventoryChanged(player, inventory, stack);
            }
        } catch (Throwable throwable) {
            ctx.handleException(throwable);
        }
    });

    void onInventoryChanged(ServerPlayerEntity player, PlayerInventory inventory, ItemStack stack);
}
