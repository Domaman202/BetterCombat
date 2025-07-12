package net.bettercombat.logic;

public interface PlayerAttackProperties {
    // Combo Count

    int getComboCount$BetterCombat();
    void setComboCount$BetterCombat(int comboCount);

    // Hits Count

    int getHitsCount$BetterCombat();
    void updateHitsCount$BetterCombat(int hitsCount, long lastHitTime);
    void resetHitsCount$BetterCombat();
}
