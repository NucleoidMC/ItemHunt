package io.github.jerozgen.itemhunt.mixin;

import io.github.jerozgen.itemhunt.event.StatusEffectAddEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    void onAddStatusEffect(StatusEffectInstance effect, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        try (var invokers = Stimuli.select().forEntity(this)) {
            var result = invokers.get(StatusEffectAddEvent.EVENT).onAddStatusEffect(this, effect, source);
            if (result == ActionResult.FAIL) {
                cir.setReturnValue(false);
            }
        }
    }

    LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }
}
