package io.github.jerozgen.itemhunt.mixin;

import io.github.jerozgen.itemhunt.event.StatusEffectAddEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    void onAddStatusEffect(MobEffectInstance effect, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        try (var invokers = Stimuli.select().forEntity(this)) {
            var result = invokers.get(StatusEffectAddEvent.EVENT).onAddStatusEffect(this, effect, source);
            if (result == InteractionResult.FAIL) {
                cir.setReturnValue(false);
            }
        }
    }

    LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }
}
