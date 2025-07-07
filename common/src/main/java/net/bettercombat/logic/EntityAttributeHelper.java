package net.bettercombat.logic;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;

public class EntityAttributeHelper {
    public static boolean itemHasRangeAttribute(ItemStack stack) {
        var attributeModifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributeModifiers != null) {
            for(var modifier: attributeModifiers.modifiers()) {
                if (modifier.attribute().value().equals(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE.value())) {
                    return true;
                }
            }
        }
        return false;
    }
}
