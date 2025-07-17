package net.bettercombat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.bettercombat.utils.AttributeModifierHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Shadow @Nullable protected Slot focusedSlot;

    @WrapOperation(method = "onMouseClick(I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesMouse(I)Z"))
    public boolean onMouseClick(KeyBinding instance, int code, Operation<Boolean> original) {
        return original.call(instance, code) && AttributeModifierHelper.checkNoTwoHanded();
    }

    @WrapOperation(method = "handleHotbarKeyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z", ordinal = 0))
    public boolean handleHotbarKeyPressed(KeyBinding instance, int keyCode, int scanCode, Operation<Boolean> original) {
        return original.call(instance, keyCode, scanCode) && AttributeModifierHelper.checkNoTwoHanded() && AttributeModifierHelper.checkOffhandPutAllow(this.focusedSlot.getStack());
    }
}
