package io.github.jerozgen.itemhunt.event;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
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

    void onInventoryChanged(ServerPlayer player, Inventory inventory, ItemStack stack);
}
