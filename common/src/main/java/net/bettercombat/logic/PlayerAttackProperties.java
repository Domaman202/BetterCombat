package net.bettercombat.logic;

public interface PlayerAttackProperties {
    int getComboCount$BetterCombat();
    void setComboCount$BetterCombat(int comboCount);
    int getHitsCount$BetterCombat();
    void updateHitsCount$BetterCombat(int hitsCount, long lastHitTime);
    void resetHitsCount$BetterCombat();
}
