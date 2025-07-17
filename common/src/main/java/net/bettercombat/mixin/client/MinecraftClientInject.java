package net.bettercombat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.shedaniel.autoconfig.AutoConfig;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.Platform;
import net.bettercombat.PlatformClient;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.bettercombat.client.BetterCombatClientMod;
import net.bettercombat.client.Keybindings;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.client.collision.TargetFinder;
import net.bettercombat.config.ClientConfigWrapper;
import net.bettercombat.logic.*;
import net.bettercombat.network.Packets;
import net.bettercombat.utils.AttributeModifierHelper;
import net.bettercombat.utils.PatternMatching;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static net.minecraft.util.hit.HitResult.Type.BLOCK;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientInject implements MinecraftClient_BetterCombat {
    @Shadow public ClientWorld world;
    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow private int itemUseCooldown;

    @Shadow @Final public TextRenderer textRenderer;

    @Shadow protected int attackCooldown;

    @Shadow @Final public InGameHud inGameHud;

    @Shadow @Nullable public HitResult crosshairTarget;

    @Unique
    private MinecraftClient thisClient$BetterCombat() {
        return (MinecraftClient)((Object)this);
    }
    @Unique
    private boolean isHoldingAttackInput$BetterCombat = false;
    @Unique
    private boolean isHarvesting$BetterCombat = false;

//     Targeting the method where all the disconnection related logic is.
    @Inject(method = "disconnect",at = @At("TAIL"))
    private void disconnect_TAIL(Screen disconnectionScreen, boolean transferring, CallbackInfo ci) {
        BetterCombatClientMod.ENABLED = false;
    }

    // Press to attack
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void pre_doAttack(CallbackInfoReturnable<Boolean> info) {
        if (!BetterCombatClientMod.ENABLED) { return; }

        MinecraftClient client = this.thisClient$BetterCombat();
        WeaponAttributes attributes = WeaponRegistry.getAttributes(client.player.getMainHandStack());
        if (attributes != null && attributes.attacks() != null) {
            if (this.isTargetingMineableBlock$BetterCombat() || this.isHarvesting$BetterCombat) {
                this.isHarvesting$BetterCombat = true;
                return;
            }
            this.startUpswing$BetterCombat(attributes);
            info.setReturnValue(false);
            info.cancel();
        }
    }

    // Hold to attack
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void pre_handleBlockBreaking(boolean bl, CallbackInfo ci) {
        if (!BetterCombatClientMod.ENABLED) { return; }

        MinecraftClient client = this.thisClient$BetterCombat();
        WeaponAttributes attributes = WeaponRegistry.getAttributes(client.player.getMainHandStack());
        if (attributes != null && attributes.attacks() != null) {
            boolean isPressed = client.options.attackKey.isPressed();
            if(isPressed && !this.isHoldingAttackInput$BetterCombat) {
                if (this.isTargetingMineableBlock$BetterCombat() || this.isHarvesting$BetterCombat) {
                    this.isHarvesting$BetterCombat = true;
                    return;
                } else {
                    ci.cancel();
                }
            }

            if (BetterCombatClientMod.config.isHoldToAttackEnabled && isPressed) {
                this.isHoldingAttackInput$BetterCombat = true;
                this.startUpswing$BetterCombat(attributes);
                ci.cancel();
            } else {
                this.isHarvesting$BetterCombat = false;
                this.isHoldingAttackInput$BetterCombat = false;
            }
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void pre_doItemUse(CallbackInfo ci) {
        if (!BetterCombatClientMod.ENABLED) { return; }

        var hand = this.getCurrentHand$BetterCombat();
        if (hand == null) { return; }
        double upswingRate = hand.upswingRate();
        if (this.upswingTicks$BetterCombat > 0 || this.player.getAttackCooldownProgress(0) < (1.0 - upswingRate)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean isTargetingMineableBlock$BetterCombat() {
        if (!BetterCombatClientMod.config.isMiningWithWeaponsEnabled) {
            return false;
        }
        var regex = BetterCombatClientMod.config.mineWithWeaponBlacklist;
        if (regex != null && !regex.isEmpty()) {
            var itemStack = this.player.getMainHandStack();
            var id = Registries.ITEM.getId(itemStack.getItem()).toString();
            if (PatternMatching.matches(id, regex)) {
                return false;
            }
        }
        if (BetterCombatClientMod.config.isAttackInsteadOfMineWhenEnemiesCloseEnabled
                && this.hasTargetsInReach$BetterCombat()) {
            return false;
        }
        MinecraftClient client = this.thisClient$BetterCombat();
        HitResult crosshairTarget = client.crosshairTarget;
        if (crosshairTarget != null && crosshairTarget.getType() == BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) crosshairTarget;
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState clicked = this.world.getBlockState(pos);
            if (this.shouldSwingThruGrass$BetterCombat()) {
                if (!clicked.getCollisionShape(this.world, pos).isEmpty() || clicked.getHardness(this.world, pos) != 0.0F) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean shouldSwingThruGrass$BetterCombat() {
        if(!BetterCombatClientMod.config.isSwingThruGrassEnabled) {
            return false;
        }
        if (BetterCombatClientMod.config.isSwingThruGrassSmart
                && !this.hasTargetsInReach$BetterCombat()) {
            return false;
        }
        var regex = BetterCombatClientMod.config.swingThruGrassBlacklist;
        if (regex == null || regex.isEmpty()) {
            return true;
        }
        var itemStack = this.player.getMainHandStack();
        var id = Registries.ITEM.getId(itemStack.getItem()).toString();
        return !PatternMatching.matches(id, regex);
    }

    @Unique
    private ItemStack upswingStack$BetterCombat;
    @Unique
    private ItemStack lastAttackedWithItemStack$BetterCombat;
    @Unique
    private int upswingTicks$BetterCombat = 0;
    @Unique
    private int lastAttacked$BetterCombat = 1000;
    @Unique
    private float lastSwingDuration$BetterCombat = 0;
    @Unique
    private int comboReset$BetterCombat = 0;

    @Unique
    private void startUpswing$BetterCombat(WeaponAttributes attributes) {
        // Guard conditions

        if (this.player.isRiding()) {
            // isRiding is `isHandsBusy()` according to official mappings
            // Support for revival mod
            return;
        }

        var hand = this.getCurrentHand$BetterCombat();
        if (hand == null) { return; }
        float upswingRate = (float) hand.upswingRate();
        if (this.upswingTicks$BetterCombat > 0
                || this.attackCooldown > 0
                || this.player.isUsingItem()
                || this.player.getAttackCooldownProgress(0) < (1.0 - upswingRate)) {
//            double attackCooldownTicks = PlayerAttackHelper.getAttackCooldownTicksCapped(player) / PlayerAttackHelper.getDualWieldingAttackSpeedMultiplier(player);
//            var currentCD = Math.round(attackCooldownTicks * player.getAttackCooldownProgress(0));
//            System.out.println("Waiting for cooldown: " + currentCD + "/" + attackCooldownTicks);
            return;
        }

        // Starting upswing
        this.player.stopUsingItem();

        this.lastAttacked$BetterCombat = 0;
        this.upswingStack$BetterCombat = this.player.getMainHandStack();
        float attackCooldownTicksFloat = PlayerAttackHelper.getAttackCooldownTicksCapped(this.player); // `getAttackCooldownProgressPerTick` should be called `getAttackCooldownLengthTicks`
        int attackCooldownTicks = Math.round(attackCooldownTicksFloat);
        this.comboReset$BetterCombat = Math.round(attackCooldownTicksFloat * BetterCombatMod.config.combo_reset_rate);
        this.upswingTicks$BetterCombat = Math.max(Math.round(attackCooldownTicksFloat * upswingRate), 1); // At least 1 upswing ticks
        this.lastSwingDuration$BetterCombat = attackCooldownTicksFloat;
        this.itemUseCooldown = attackCooldownTicks; // Vanilla MinecraftClient property for compatibility
        this.setMiningCooldown$BetterCombat(attackCooldownTicks);
//        System.out.println("Starting upswingTicks: " + upswingTicks);
        String animationName = hand.attack().animation();
        boolean isOffHand = hand.isOffHand();
        var animatedHand = AnimatedHand.from(isOffHand, attributes.isTwoHanded());
        ((PlayerAttackAnimatable) this.player).playAttackAnimation$BetterCombat(animationName, animatedHand, attackCooldownTicksFloat, upswingRate);
        var packet = new Packets.AttackAnimation(this.player.getId(), animatedHand, animationName, attackCooldownTicksFloat, upswingRate);
        Platform.networkC2S_Send(packet);
        BetterCombatClientEvents.ATTACK_START.invoke(handler -> {
            handler.onPlayerAttackStart(this.player, hand);
        });
    }

    @Unique
    private void cancelSwingIfNeeded$BetterCombat() {
        if (this.upswingStack$BetterCombat != null && !this.areItemStackEqual$BetterCombat(this.player.getMainHandStack(), this.upswingStack$BetterCombat)) {
            this.cancelWeaponSwing$BetterCombat();
            return;
        }
    }

    @Unique
    private void attackFromUpswingIfNeeded$BetterCombat() {
        if (this.upswingTicks$BetterCombat > 0) {
            --this.upswingTicks$BetterCombat;
            if (this.upswingTicks$BetterCombat == 0) {
                this.performAttack$BetterCombat();
                this.upswingStack$BetterCombat = null;
            }
        }
    }

    private void resetComboIfNeeded() {
        // Combo timeout
        if(this.lastAttacked$BetterCombat > this.comboReset$BetterCombat && this.getComboCount$BetterCombat() > 0) {
            this.setComboCount$BetterCombat(0);
        }
        // Switching main-hand weapon
        if (!PlayerAttackHelper.shouldAttackWithOffHand(player, this.getComboCount$BetterCombat())) {
            if(player.getMainHandStack() == null
                    || (this.lastAttackedWithItemStack$BetterCombat != null && !this.lastAttackedWithItemStack$BetterCombat.getItem().equals(player.getMainHandStack().getItem()) ) ) {
                this.setComboCount$BetterCombat(0);
            }
        }
    }

    @Unique
    private List<Entity> targetsInReach$BetterCombat = null;

    @Unique
    private boolean shouldUpdateTargetsInReach$BetterCombat() {
        if(BetterCombatClientMod.config.isHighlightCrosshairEnabled
                || BetterCombatClientMod.config.isAttackInsteadOfMineWhenEnemiesCloseEnabled) {
            return this.targetsInReach$BetterCombat == null;
        }
        return false;
    }

    @Unique
    private void updateTargetsInReach$BetterCombat(List<Entity> targets) {
        this.targetsInReach$BetterCombat = targets;
    }

    @Unique
    private void updateTargetsIfNeeded$BetterCombat() {
        if (this.shouldUpdateTargetsInReach$BetterCombat()) {
            List<Entity> targets = List.of();
            var hand = PlayerAttackHelper.getCurrentAttack(player, this.getComboCount$BetterCombat());
            if (hand != null) {
                WeaponAttributes attributes = WeaponRegistry.getAttributes(hand.itemStack());
                var range = PlayerAttackHelper.getRangeForItem(player, hand.itemStack());
                if (attributes != null && attributes.attacks() != null) {
                    targets = TargetFinder.findAttackTargets(
                            player,
                            this.getCursorTarget$BetterCombat(),
                            hand.attack(),
                            range);
                }
            }
            this.updateTargetsInReach$BetterCombat(targets);
        }
    }

    @Inject(method = "tick",at = @At("HEAD"))
    private void pre_Tick(CallbackInfo ci) {
        if (player == null) {
            return;
        }
        this.targetsInReach$BetterCombat = null;
        this.lastAttacked$BetterCombat += 1;
        this.cancelSwingIfNeeded$BetterCombat();
        this.attackFromUpswingIfNeeded$BetterCombat();
        this.updateTargetsIfNeeded$BetterCombat();
        resetComboIfNeeded();
    }

    @Inject(method = "tick",at = @At("TAIL"))
    private void post_Tick(CallbackInfo ci) {
        if (player == null) {
            return;
        }
        if (Keybindings.toggleMineKeyBinding.wasPressed()) {
            BetterCombatClientMod.config.isMiningWithWeaponsEnabled = !BetterCombatClientMod.config.isMiningWithWeaponsEnabled;
            AutoConfig.getConfigHolder(ClientConfigWrapper.class).save();

            var message = I18n.translate(BetterCombatClientMod.config.isMiningWithWeaponsEnabled ?
                    "hud.bettercombat.mine_with_weapons_on" : "hud.bettercombat.mine_with_weapons_off");
            inGameHud.setOverlayMessage(Text.literal(message), false);
        }
    }

    @Unique
    private void performAttack$BetterCombat() {
        if (Keybindings.feintKeyBinding.isPressed()) {
            this.player.resetLastAttackedTicks();
            this.cancelWeaponSwing$BetterCombat();
            return;
        }

        var hand = getCurrentHand$BetterCombat();
        if (hand == null) { return; }
        var attack = hand.attack();
        var upswingRate = hand.upswingRate();
        if (this.player.getAttackCooldownProgress(0) < (1.0 - upswingRate)) {
            return;
        }
        // System.out.println("Attack with CD: " + client.player.getAttackCooldownProgress(0));

        var cursorTarget = getCursorTarget$BetterCombat();
        var range = PlayerAttackHelper.getRangeForItem(this.player, hand.itemStack());
        List<Entity> targets = TargetFinder.findAttackTargets(
                this.player,
                cursorTarget,
                attack,
                range);
        this.updateTargetsInReach$BetterCombat(targets);
        if (targets.isEmpty()) {
            PlatformClient.onEmptyLeftClick(this.player);

            if (this.crosshairTarget.getType() == BLOCK) {
                var blockHitResult = (BlockHitResult) this.crosshairTarget;
                var pos = blockHitResult.getBlockPos();
                var packet = new Packets.C2S_BlockHit(pos);
                Platform.networkC2S_Send(packet);
            }
        }

        var attackedCount = (int) targets.stream().filter(it -> it.isAttackable() && it instanceof LivingEntity living && living.getHealth() > 0).count();
        if (attackedCount == 0) {
            if (BetterCombatMod.config.reset_hits_after_miss) {
                ((PlayerAttackProperties) player).resetHitsCount$BetterCombat();
            }
        } else {
            ((PlayerAttackProperties) player).updateHitsCount$BetterCombat(attackedCount, System.currentTimeMillis());
            if (BetterCombatClientMod.config.hitInfoInChat) {
                player.sendMessage(Text.of("§r§6Нанесено ударов: §o§4" + ((PlayerAttackProperties) player).getHitsCount$BetterCombat()), false);
            }
        }

        // Mimic logic of:
        // ClientPlayerInteractionManager.attackEntity(PlayerEntity player, Entity target)
        var packet = new Packets.C2S_AttackRequest(getComboCount$BetterCombat(), this.player.isSneaking(), this.player.getInventory().getSelectedSlot(), cursorTarget, targets);
        Platform.networkC2S_Send(packet);
        for (var target: targets) {
            this.player.attack(target);
        }
        this.player.resetLastAttackedTicks();
        BetterCombatClientEvents.ATTACK_HIT.invoke(handler -> {
            handler.onPlayerAttackStart(this.player, hand, targets, cursorTarget);
        });

        this.setComboCount$BetterCombat(getComboCount$BetterCombat() + 1);
        if (!hand.isOffHand()) {
            this.lastAttackedWithItemStack$BetterCombat = hand.itemStack();
        }
    }

    @Unique
    private AttackHand getCurrentHand$BetterCombat() {
        return PlayerAttackHelper.getCurrentAttack(this.player, this.getComboCount$BetterCombat());
    }

    @Unique
    private void setComboCount$BetterCombat(int comboCount) {
        ((PlayerAttackProperties)player).setComboCount$BetterCombat(comboCount);
    }

    @Unique
    private static boolean areItemStackEqual$BetterCombat(ItemStack left, ItemStack right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return ItemStack.areEqual(left, right);
    }

    @Unique
    private void setMiningCooldown$BetterCombat(int ticks) {
        MinecraftClient client = this.thisClient$BetterCombat();
        ((MinecraftClientAccessor) client).setAttackCooldown(ticks); // This is actually the mining cooldown
    }

    @Unique
    private void cancelWeaponSwing$BetterCombat() {
        var downWind = (int)Math.round(PlayerAttackHelper.getAttackCooldownTicksCapped(this.player) * (1 - 0.5 * BetterCombatMod.config.upswing_multiplier));
        ((PlayerAttackAnimatable) this.player).stopAttackAnimation$BetterCombat(downWind);
        var packet = Packets.AttackAnimation.stop(this.player.getId(), downWind);
        Platform.networkC2S_Send(packet);
        this.upswingStack$BetterCombat = null;
        this.upswingTicks$BetterCombat = 0;
        this.itemUseCooldown = 0;
        this.setMiningCooldown$BetterCombat(0);
    }


    // SECTION: MinecraftClient_BetterCombat

    @Override
    public int getComboCount$BetterCombat() {
        return ((PlayerAttackProperties) this.player).getComboCount$BetterCombat();
    }

    @Override
    public boolean hasTargetsInReach$BetterCombat() {
        return this.targetsInReach$BetterCombat != null && !this.targetsInReach$BetterCombat.isEmpty();
    }

    @Override
    public float getSwingProgress$BetterCombat() {
        if (this.lastAttacked$BetterCombat > this.lastSwingDuration$BetterCombat || this.lastSwingDuration$BetterCombat <= 0) {
            return 1F;
        }
        return (float)this.lastAttacked$BetterCombat / this.lastSwingDuration$BetterCombat;
    }

    @Override
    public int getUpswingTicks$BetterCombat() {
        return this.upswingTicks$BetterCombat;
    }

    @Override
    public void cancelUpswing$BetterCombat() {
        if (this.upswingTicks$BetterCombat > 0) {
            this.cancelWeaponSwing$BetterCombat();
        }
    }

    //

    @WrapOperation(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
    public void handleInputEvents(ClientPlayNetworkHandler instance, Packet<?> packet, Operation<Void> original) {
        if (AttributeModifierHelper.checkNoTwoHanded()) {
            original.call(instance, packet);
        }
    }
}
