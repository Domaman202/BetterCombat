package net.bettercombat.mixin;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.ItemStackNBTWeaponAttributes;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackNBTWeaponAttributes {
    @Unique
    private boolean hasInvalidAttributes$BetterCombat = false;
    @Unique
    private WeaponAttributes weaponAttributes$BetterCombat;

    @Override
    public boolean hasInvalidAttributes$BetterCombat() {
        return this.hasInvalidAttributes$BetterCombat;
    }

    @Override
    public void setInvalidAttributes$BetterCombat(boolean invalid) {
        this.hasInvalidAttributes$BetterCombat = invalid;
    }

    @Override
    public WeaponAttributes getWeaponAttributes$BetterCombat() {
        return this.weaponAttributes$BetterCombat;
    }

    @Override
    public void setWeaponAttributes$BetterCombat(WeaponAttributes weaponAttributes) {
        this.weaponAttributes$BetterCombat = weaponAttributes;
    }
}
