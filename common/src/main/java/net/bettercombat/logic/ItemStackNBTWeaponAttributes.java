package net.bettercombat.logic;

import net.bettercombat.api.WeaponAttributes;
import org.jetbrains.annotations.Nullable;

public interface ItemStackNBTWeaponAttributes {
    boolean hasInvalidAttributes$BetterCombat();
    void setInvalidAttributes$BetterCombat(boolean invalid);
    @Nullable
    WeaponAttributes getWeaponAttributes$BetterCombat();
    void setWeaponAttributes$BetterCombat(@Nullable WeaponAttributes weaponAttributes);
}
