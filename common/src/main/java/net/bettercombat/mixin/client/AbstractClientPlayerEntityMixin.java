package net.bettercombat.mixin.client;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.api.PartKey;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.Platform;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.bettercombat.client.BetterCombatClientMod;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.client.animation.*;
import net.bettercombat.client.animation.modifier.HarshAdjustmentModifier;
import net.bettercombat.client.animation.modifier.TransmissionSpeedModifier;
import net.bettercombat.client.compat.FirstPersonAnimationCompatibility;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin extends PlayerEntity implements PlayerAttackAnimatable {
    @Unique
    private final AttackAnimationSubStack attackAnimation$BetterCombat = new AttackAnimationSubStack(this.createAttackAdjustment$BetterCombat());
    @Unique
    private final PoseSubStack mainHandBodyPose$BetterCombat = new PoseSubStack(this.createPoseAdjustment$BetterCombat(), true, true);
    @Unique
    private final PoseSubStack mainHandItemPose$BetterCombat = new PoseSubStack(null, false, true);
    @Unique
    private final PoseSubStack offHandBodyPose$BetterCombat = new PoseSubStack(null, true, false);
    @Unique
    private final PoseSubStack offHandItemPose$BetterCombat = new PoseSubStack(null, false, true);

    public AbstractClientPlayerEntityMixin(World world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @SuppressWarnings("removal")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void postInit(ClientWorld world, GameProfile profile, CallbackInfo ci) {
        var stack = ((IAnimatedPlayer) this).getAnimationStack();
        stack.addAnimLayer(1, this.offHandItemPose$BetterCombat.base);
        stack.addAnimLayer(2, this.offHandBodyPose$BetterCombat.base);
        stack.addAnimLayer(3, this.mainHandItemPose$BetterCombat.base);
        stack.addAnimLayer(4, this.mainHandBodyPose$BetterCombat.base);
        stack.addAnimLayer(2000, this.attackAnimation$BetterCombat.base);

        this.mainHandBodyPose$BetterCombat.configure = this::updateAnimationByCurrentActivity$BetterCombat;
        this.offHandBodyPose$BetterCombat.configure = this::updateAnimationByCurrentActivity$BetterCombat;
    }

    @Override
    public void updateAnimationsOnTick$BetterCombat() {
        var instance = (Object)this;
        var player = (PlayerEntity)instance;
        var isLeftHanded = this.isLeftHanded$BetterCombat();
        var hasActiveAttackAnimation = this.attackAnimation$BetterCombat.base.getAnimation() != null && this.attackAnimation$BetterCombat.base.getAnimation().isActive();
        var mainHandStack = player.getMainHandStack();
        // No pose during special activities

        if (player.handSwinging // Official mapping name: `isHandBusy`
                || player.isSwimming()
                || player.isUsingItem()
                || player.isClimbing()
                || player.isGliding()
                || Platform.isCastingSpell(player)
                || CrossbowItem.isCharged(mainHandStack)) {
            this.mainHandBodyPose$BetterCombat.setPose(null, isLeftHanded);
            this.mainHandItemPose$BetterCombat.setPose(null, isLeftHanded);
            this.offHandBodyPose$BetterCombat.setPose(null, isLeftHanded);
            this.offHandItemPose$BetterCombat.setPose(null, isLeftHanded);
            return;
        }

        // Restore auto body rotation upon swing - Fix issue #11

        if (hasActiveAttackAnimation) {
            this.turnHead(player.getYaw());
        }

        // Pose

        var betterCombatPlayer = (EntityPlayer_BetterCombat)player;

        KeyframeAnimation newMainHandPose = null;
        KeyframeAnimation newOffHandPose = null;
        if (MinecraftClient.getInstance().player == player) { // Logic on local player too for improved responsiveness
            var pose = PlayerAttackHelper.poseForPlayer(player);
            if (!pose.base().isEmpty()) {
                newMainHandPose = (KeyframeAnimation) PlayerAnimationRegistry.getAnimation(Identifier.of(pose.base()));
            }
            if (!pose.offHand().isEmpty()) {
                newOffHandPose = (KeyframeAnimation) PlayerAnimationRegistry.getAnimation(Identifier.of(pose.offHand()));
            }
        } else {
            if (betterCombatPlayer.getMainHandIdleAnimation$BetterCombat() != null && !betterCombatPlayer.getMainHandIdleAnimation$BetterCombat().isEmpty()) {
                newMainHandPose = (KeyframeAnimation) PlayerAnimationRegistry.getAnimation(Identifier.of(betterCombatPlayer.getMainHandIdleAnimation$BetterCombat()));
            }
            if (betterCombatPlayer.getOffHandIdleAnimation$BetterCombat() != null && !betterCombatPlayer.getOffHandIdleAnimation$BetterCombat().isEmpty()) {
                newOffHandPose = (KeyframeAnimation) PlayerAnimationRegistry.getAnimation(Identifier.of(betterCombatPlayer.getOffHandIdleAnimation$BetterCombat()));
            }
        }

        this.mainHandItemPose$BetterCombat.setPose(newMainHandPose, isLeftHanded);
        this.offHandItemPose$BetterCombat.setPose(newOffHandPose, isLeftHanded);

        if (!PlayerAttackHelper.isTwoHandedWielding(player)) {
            if (this.isWalking$BetterCombat() || this.isSneaking()) {
                newMainHandPose = null;
                newOffHandPose = null;
            }
        }
        this.mainHandBodyPose$BetterCombat.setPose(newMainHandPose, isLeftHanded);
        this.offHandBodyPose$BetterCombat.setPose(newOffHandPose, isLeftHanded);
    }

    @Override
    public void playAttackAnimation$BetterCombat(String name, AnimatedHand animatedHand, float length, float upswing) {
        try {
            KeyframeAnimation animation = (KeyframeAnimation) PlayerAnimationRegistry.getAnimation(Identifier.of(name));
            var copy = animation.mutableCopy();
            this.updateAnimationByCurrentActivity$BetterCombat(copy);
            copy.torso.fullyEnablePart(true);
            copy.head.pitch.setEnabled(false);
            var speed = ((float)animation.endTick) / length;
            var mirror = animatedHand.isOffHand();
            if(this.isLeftHanded$BetterCombat()) {
                mirror = !mirror;
            }

            var fadeIn = copy.beginTick;
            float upswingSpeed = speed / BetterCombatMod.config.getUpswingMultiplier();
            float downwindSpeed = (float) (speed *
                    MathHelper.lerp(Math.max(BetterCombatMod.config.getUpswingMultiplier() - 0.5, 0) / 0.5, // Choosing value :D
                    (1F - upswing),                     // Use this value at config `0.5`
                    upswing / (1F - upswing)));         // Use this value at config `1.0`
            this.attackAnimation$BetterCombat.speed.set(upswingSpeed,
                    List.of(
                            new TransmissionSpeedModifier.Gear(length * upswing, downwindSpeed),
                            new TransmissionSpeedModifier.Gear(length, speed)
                    ));
            this.attackAnimation$BetterCombat.mirror.setEnabled(mirror);

            var player = new CustomAnimationPlayer(copy.build(), 0);
            player.setFirstPersonMode(FirstPersonAnimationCompatibility.firstPersonMode());
            player.setFirstPersonConfiguration(this.firstPersonConfig$BetterCombat(animatedHand));
            this.attackAnimation$BetterCombat.base.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(fadeIn, Ease.INOUTSINE),
                    player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private AdjustmentModifierV2 createAttackAdjustment$BetterCombat() {
        var player = (PlayerEntity)this;
        return new AdjustmentModifierV2((partName) -> {
            // System.out.println("Player pitch: " + player.getPitch());
            float rotationX = 0;
            float rotationY = 0;
            float rotationZ = 0;
            float offsetX = 0;
            float offsetY = 0;
            float offsetZ = 0;

            if (FirstPersonMode.isFirstPersonPass()) {
                var pitch = player.getPitch();
                pitch = (float) Math.toRadians(pitch);
                if (partName == PartKey.BODY) {
                    rotationX -= pitch;
                    if (pitch < 0) {
                        var offset = Math.abs(Math.sin(pitch));
                        offsetY += offset * 0.5;
                        offsetZ -= offset;
                    }
                // else if (isArm(partName)) rotationX = pitch;
                } else return Optional.empty();
            } else {
                var pitch = player.getPitch();
                pitch = (float) Math.toRadians(pitch);
                if (partName == PartKey.BODY) rotationX -= pitch * 0.75F;
                else if (this.isArm$BetterCombat(partName)) rotationX += pitch * 0.25F;
                else if (this.isLeg$BetterCombat(partName)) rotationX -= pitch * 0.75;
                else return Optional.empty();
            }

            return Optional.of(new AdjustmentModifierV2.PartModifier(
                    new Vec3f(rotationX, rotationY, rotationZ),
                    new Vec3f(offsetX, offsetY, offsetZ))
            );
        });
    }

    @Unique
    private boolean isArm$BetterCombat(PartKey partName) {
        return partName == PartKey.RIGHT_ARM || partName == PartKey.LEFT_ARM;
    }

    @Unique
    private boolean isLeg$BetterCombat(PartKey partName) {
        return partName == PartKey.RIGHT_LEG || partName == PartKey.LEFT_LEG;
    }

    @Unique
    private AdjustmentModifier createPoseAdjustment$BetterCombat() {
        var player = (PlayerEntity)this;
        return new HarshAdjustmentModifier((partName) -> {
            float rotationX = 0;
            float rotationY = 0;
            float rotationZ = 0;
            float offsetX = 0;
            float offsetY = 0;
            float offsetZ = 0;

            if (!FirstPersonMode.isFirstPersonPass()) {
                if (this.isArm$BetterCombat(partName)) {
                    if (!this.mainHandItemPose$BetterCombat.lastAnimationUsesBodyChannel && player.isInSneakingPose()) {
                        offsetY += 3;
                    }
                } else return Optional.empty();
            }

            return Optional.of(new AdjustmentModifier.PartModifier(
                    new Vec3f(rotationX, rotationY, rotationZ),
                    new Vec3f(offsetX, offsetY, offsetZ))
            );
        });
    }

    @Unique
    private void updateAnimationByCurrentActivity$BetterCombat(KeyframeAnimation.AnimationBuilder animation) {
        var pose = getPose();
        switch (pose) {
            case STANDING, SLEEPING, GLIDING, SPIN_ATTACK, CROUCHING, LONG_JUMPING, DYING -> {}
            case SWIMMING -> {
                StateCollectionHelper.configure(animation.rightLeg, false, false);
                StateCollectionHelper.configure(animation.leftLeg, false, false);
            }
            default -> {}
        }
        if (this.isMounting$BetterCombat()) {
            StateCollectionHelper.configure(animation.rightLeg, false, false);
            StateCollectionHelper.configure(animation.leftLeg, false, false);
        } else {
            var legAnimationThreshold = BetterCombatClientMod.config.legAnimationThreshold;
            if (BetterCombatClientMod.config.legAnimationThreshold > 0) {
                var moving = this.isSprinting() || this.isWalking$BetterCombat();
//                var horizontalSpeed = this.getVelocity().horizontalLength();
//                System.out.println("Horizontal speed: " + horizontalSpeed);
                if (moving
                        // && horizontalSpeed > legAnimationThreshold
                        && this.getVelocity().horizontalLengthSquared() > (legAnimationThreshold * legAnimationThreshold)
                ) {
                    StateCollectionHelper.configure(animation.rightLeg, false, false);
                    StateCollectionHelper.configure(animation.leftLeg, false, false);
                }
            }
        }
    }


    @Unique
    private boolean isWalking$BetterCombat() {
        return !this.isDead() && (this.isSwimming() || this.getVelocity().horizontalLength() > 0.03);
    }

    @Unique
    private boolean isMounting$BetterCombat() {
        return this.getVehicle() != null;
    }

    @Unique
    public boolean isLeftHanded$BetterCombat() {
        return this.getMainArm() == Arm.LEFT;
    }

    // PlayerAttackAnimatable

    @Override
    public void stopAttackAnimation$BetterCombat(float length) {
        IAnimation currentAnimation = this.attackAnimation$BetterCombat.base.getAnimation();
        if (currentAnimation != null && currentAnimation instanceof KeyframeAnimationPlayer) {
            var fadeOut = Math.round(length);
            this.attackAnimation$BetterCombat.adjustmentModifier.fadeOut(fadeOut);
            this.attackAnimation$BetterCombat.base.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(fadeOut, Ease.INOUTSINE), null);
        }
    }

    // FirstPersonAnimator

    @Unique
    private FirstPersonConfiguration firstPersonConfig$BetterCombat(AnimatedHand animatedHand) {
        // boolean leftHanded = getMainArm() == Arm.LEFT;
        var showRightItem = true;
        var showLeftItem = BetterCombatClientMod.config.isShowingOtherHandFirstPerson || animatedHand == AnimatedHand.TWO_HANDED;
        var showRightArm = showRightItem && BetterCombatClientMod.config.isShowingArmsInFirstPerson;
        var showLeftArm = showLeftItem && BetterCombatClientMod.config.isShowingArmsInFirstPerson;

        var config = new FirstPersonConfiguration(showRightArm, showLeftArm, showRightItem, showLeftItem);
        // System.out.println("Animation config: " + config);
        return config;
    }
}
