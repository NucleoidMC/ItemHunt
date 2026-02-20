package io.github.jerozgen.itemhunt.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface StatusEffectAddEvent {
    StimulusEvent<StatusEffectAddEvent> EVENT = StimulusEvent.create(StatusEffectAddEvent.class, ctx -> (entity, effect, source) -> {
        try {
            for (var listener : ctx.getListeners()) {
                var result = listener.onAddStatusEffect(entity, effect, source);
                if (result != InteractionResult.PASS)
                    return result;
            }
        } catch (Throwable throwable) {
            ctx.handleException(throwable);
        }
        return InteractionResult.PASS;
    });

    InteractionResult onAddStatusEffect(Entity entity, MobEffectInstance effect, @Nullable Entity source);
}
