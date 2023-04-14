package io.github.jerozgen.itemhunt.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface StatusEffectAddEvent {
    StimulusEvent<StatusEffectAddEvent> EVENT = StimulusEvent.create(StatusEffectAddEvent.class, ctx -> (entity, effect, source) -> {
        try {
            for (var listener : ctx.getListeners()) {
                var result = listener.onAddStatusEffect(entity, effect, source);
                if (result != ActionResult.PASS)
                    return result;
            }
        } catch (Throwable throwable) {
            ctx.handleException(throwable);
        }
        return ActionResult.PASS;
    });

    ActionResult onAddStatusEffect(Entity entity, StatusEffectInstance effect, @Nullable Entity source);
}
