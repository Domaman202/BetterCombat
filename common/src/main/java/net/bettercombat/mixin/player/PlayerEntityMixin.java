package net.bettercombat.mixin.player;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements PlayerAttackProperties, EntityPlayer_BetterCombat {
    @Unique
    private int comboCount$BetterCombat = 0;
    @Unique
    private int hitsCount$BetterCombat = 0;
    @Unique
    private long lastHitTime$BetterCombat = 0;

    @Override
    public int getComboCount$BetterCombat() {
        return this.comboCount$BetterCombat;
    }

    @Override
    public void setComboCount$BetterCombat(int comboCount) {
        this.comboCount$BetterCombat = comboCount;
    }

    @Override
    public int getHitsCount$BetterCombat() {
        return this.hitsCount$BetterCombat;
    }

    @Override
    public void updateHitsCount$BetterCombat(int hitsCount, long lastHitTime) {
        if (lastHitTime - this.lastHitTime$BetterCombat > BetterCombatMod.config.hits_reset_time)
            this.hitsCount$BetterCombat = hitsCount;
        else this.hitsCount$BetterCombat += hitsCount;
        this.lastHitTime$BetterCombat = lastHitTime;
    }

    @Override
    public void resetHitsCount$BetterCombat() {
        this.hitsCount$BetterCombat = 0;
    }

    @Unique
    private static final TrackedData<String> BETTER_COMBAT_MAIN_IDLE_ANIMATION = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.STRING);
    @Unique
    private static final TrackedData<String> BETTER_COMBAT_OFF_IDLE_ANIMATION = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.STRING);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initDataTracker_TAIL_SpellEngine_SyncEffects(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(BETTER_COMBAT_MAIN_IDLE_ANIMATION, "");
        builder.add(BETTER_COMBAT_OFF_IDLE_ANIMATION, "");
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void post_Tick(CallbackInfo ci) {
        var instance = (Object)this;
        var player = ((PlayerEntity)instance);

        if (player.getWorld().isClient()) {
            ((PlayerAttackAnimatable) this).updateAnimationsOnTick$BetterCombat();
        } else {
            var pose = PlayerAttackHelper.poseForPlayer(player);
            player.getDataTracker().set(BETTER_COMBAT_MAIN_IDLE_ANIMATION, pose.base());
            player.getDataTracker().set(BETTER_COMBAT_OFF_IDLE_ANIMATION, pose.offHand());
        }
        this.updateDualWieldingSpeedBoost$BetterCombat();
    }

    @Override
    public String getMainHandIdleAnimation$BetterCombat() {
        return ((PlayerEntity) ((Object)this)).getDataTracker().get(BETTER_COMBAT_MAIN_IDLE_ANIMATION);
    }

    @Override
    public String getOffHandIdleAnimation$BetterCombat() {
        return ((PlayerEntity) ((Object)this)).getDataTracker().get(BETTER_COMBAT_OFF_IDLE_ANIMATION);
    }

    // FEATURE: Disable sweeping for attributed weapons

    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 3)
    private boolean disableSweeping(boolean value) {
        if (BetterCombatMod.config.allow_vanilla_sweeping) {
            return value;
        }

        var player = ((PlayerEntity) ((Object)this));
        var currentHand = PlayerAttackHelper.getCurrentAttack(player, this.comboCount$BetterCombat);
        if (currentHand != null) {
            // Disable sweeping
            return false;
        }
        return value;
    }

    // FEATURE: Dual wielding

    @Unique
    private Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> dualWieldingAttributeMap$BetterCombat;
    @Unique
    private static final Identifier dualWieldingSpeedModifierId$BetterCombat = Identifier.of(BetterCombatMod.ID, "dual_wield");


    // FIXME: Replace with high level multiplied Mixin, WrapOperation player.getAttributes(...)
    @Unique
    private void updateDualWieldingSpeedBoost$BetterCombat() {
        var player = ((PlayerEntity) ((Object)this));
        var newState = PlayerAttackHelper.isDualWielding(player);
        var currentState = this.dualWieldingAttributeMap$BetterCombat != null;
        if (newState != currentState) {
            if(newState) {
                // Just started dual wielding
                // Adding speed boost modifier
                this.dualWieldingAttributeMap$BetterCombat = HashMultimap.create();
                double multiplier = BetterCombatMod.config.dual_wielding_attack_speed_multiplier - 1;
                this.dualWieldingAttributeMap$BetterCombat.put(
                        EntityAttributes.ATTACK_SPEED,
                        new EntityAttributeModifier(
                                dualWieldingSpeedModifierId$BetterCombat,
                                multiplier,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                player.getAttributes().addTemporaryModifiers(this.dualWieldingAttributeMap$BetterCombat);
            } else {
                // Just stopped dual wielding
                // Removing speed boost modifier
                if (this.dualWieldingAttributeMap$BetterCombat != null) { // Safety first... Who knows...
                    player.getAttributes().removeModifiers(this.dualWieldingAttributeMap$BetterCombat);
                    this.dualWieldingAttributeMap$BetterCombat = null;
                }
            }
        }
    }

    @ModifyArg(method = "attack", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;"),
            index = 0)
    public Hand getHand(Hand hand) {
        var player = ((PlayerEntity) ((Object)this) );
        var currentHand = PlayerAttackHelper.getCurrentAttack(player, this.comboCount$BetterCombat);
        if (currentHand != null) {
            return currentHand.isOffHand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        } else {
            return Hand.MAIN_HAND;
        }
    }

    @Unique
    private AttackHand lastAttack$BetterCombat;

    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    public ItemStack getMainHandStack_Redirect(PlayerEntity instance) {
        // DUAL WIELDING LOGIC
        // Here we return the off-hand stack as fake main-hand, purpose:
        // - Getting enchants
        // - Getting itemstack to be damaged
        if (this.comboCount$BetterCombat < 0) {
            // Vanilla behaviour
            return instance.getMainHandStack();
        }
        var hand = PlayerAttackHelper.getCurrentAttack(instance, this.comboCount$BetterCombat);
        if (hand == null) {
            var isOffHand = PlayerAttackHelper.shouldAttackWithOffHand(instance, this.comboCount$BetterCombat);
            if (isOffHand) {
                return ItemStack.EMPTY;
            } else {
                return instance.getMainHandStack();
            }
        }
        this.lastAttack$BetterCombat = hand;
        return hand.itemStack();
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;setStackInHand(Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)V"))
    public void setStackInHand_Redirect(PlayerEntity instance, Hand handArg, ItemStack itemStack) {
        // DUAL WIELDING LOGIC
        // In case item got destroyed due to durability loss
        // We empty the correct hand
        if (this.comboCount$BetterCombat < 0) {
            // Vanilla behaviour
            instance.setStackInHand(handArg, itemStack);
        }
        // `handArg` argument is always `MAIN`, we can ignore it
        AttackHand hand = this.lastAttack$BetterCombat;
        if (hand == null) {
            hand = PlayerAttackHelper.getCurrentAttack(instance, this.comboCount$BetterCombat);
        }
        if (hand == null) {
            instance.setStackInHand(handArg, itemStack);
            return;
        }
        var redirectedHand = hand.isOffHand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        instance.setStackInHand(redirectedHand, itemStack);
    }

    // SECTION: BetterCombatPlayer

    @Unique
    @Nullable
    public AttackHand getCurrentAttack$BetterCombat() {
        if (this.comboCount$BetterCombat < 0)
            return null;
        var player = ((PlayerEntity) ((Object)this));
        return PlayerAttackHelper.getCurrentAttack(player, this.comboCount$BetterCombat);
    }
}
