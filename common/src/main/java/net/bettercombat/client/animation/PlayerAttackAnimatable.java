package net.bettercombat.client.animation;

import net.bettercombat.logic.AnimatedHand;

public interface PlayerAttackAnimatable {
    void updateAnimationsOnTick();
    void playAttackAnimation(String name, AnimatedHand hand, float length, float upswing);
    void stopAttackAnimation(float length);
}
