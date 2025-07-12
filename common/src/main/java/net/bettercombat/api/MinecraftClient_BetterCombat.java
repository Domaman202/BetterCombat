package net.bettercombat.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.util.hit.HitResult.Type.ENTITY;

/**
 * Extension for `MinecraftClient`.
 * Example usage:
 * ((MinecraftClient_BetterCombat)MinecraftClient.getInstance()).getComboCount();
 */
public interface MinecraftClient_BetterCombat {
    int getComboCount$BetterCombat();
    boolean hasTargetsInReach$BetterCombat();
    @Nullable
    default Entity getCursorTarget$BetterCombat() {
        var client = (MinecraftClient)this;
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == ENTITY) {
            return ((EntityHitResult)client.crosshairTarget).getEntity();
        }
        return null;
    }

    int getUpswingTicks$BetterCombat();
    float getSwingProgress$BetterCombat();
    default boolean isWeaponSwingInProgress$BetterCombat() {
        return this.getSwingProgress$BetterCombat() < 1F;
    }
    void cancelUpswing$BetterCombat();
}
