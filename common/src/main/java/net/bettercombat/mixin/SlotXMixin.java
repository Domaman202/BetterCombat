package net.bettercombat.mixin;

import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.screen.PlayerScreenHandler$1")
public class SlotXMixin extends Slot {
    public SlotXMixin(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public void setStack(ItemStack stack) {
        if (this.isEnabled()) {
            super.setStack(stack);
        }
    }

    @Override
    public ItemStack getStack() {
        if (this.isEnabled())
            return super.getStack();
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isEnabled() {
        var mainHandStack = ((PlayerInventory) this.inventory).getSelectedStack();
        var mainHandAttributes = WeaponRegistry.getAttributes(mainHandStack);
        return mainHandAttributes == null || !mainHandAttributes.isTwoHanded();
    }
}
