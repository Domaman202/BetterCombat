package net.bettercombat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.client.BetterCombatClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(InGameHud.class)
public abstract class InGameHudInject {
    @WrapOperation(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void renderCrosshair_WrapOperation(DrawContext instance, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
        if (BetterCombatClientMod.config.isHighlightCrosshairEnabled
            && ((MinecraftClient_BetterCombat) MinecraftClient.getInstance()).hasTargetsInReach$BetterCombat()) {
            float alpha = 0.5F;

            var color = BetterCombatClientMod.config.hudHighlightColor;
            float red = ((float) ((color >> 16) & 0xFF)) / 255F;
            float green = ((float) ((color >> 8) & 0xFF)) / 255F;
            float blue = ((float) (color & 0xFF)) / 255F;

            int colorARGB = ColorHelper.fromFloats(alpha, red, green, blue);

            instance.drawGuiTexture(pipeline, sprite, x, y, width, height, colorARGB);
        } else {
            original.call(instance, pipeline, sprite, x, y, width, height);
        }
    }

    @WrapOperation(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIIIIII)V"))
    private void renderCrosshair_WrapOperation(DrawContext instance, RenderPipeline pipeline, Identifier sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int width, int height, Operation<Void> original) {
        if (BetterCombatClientMod.config.isHighlightCrosshairEnabled
                && ((MinecraftClient_BetterCombat) MinecraftClient.getInstance()).hasTargetsInReach$BetterCombat()) {
            float alpha = 0.5F;

            var color = BetterCombatClientMod.config.hudHighlightColor;
            float red = ((float) ((color >> 16) & 0xFF)) / 255F;
            float green = ((float) ((color >> 8) & 0xFF)) / 255F;
            float blue = ((float) (color & 0xFF)) / 255F;

            int colorARGB = ColorHelper.fromFloats(alpha, red, green, blue);

            instance.drawGuiTexture(pipeline, sprite, textureWidth, textureHeight, u, v, x, y, width, height, colorARGB);
        } else {
            original.call(instance, pipeline, sprite, textureWidth, textureHeight, u, v, x, y, width, height);
        }
    }
}