package net.bettercombat.client.animation;

import net.bettercombat.logic.AnimatedHand;

public interface PlayerAttackAnimatable {
    void updateAnimationsOnTick$BetterCombat();
    void playAttackAnimation$BetterCombat(String name, AnimatedHand hand, float length, float upswing);
    void stopAttackAnimation$BetterCombat(float length);
}
