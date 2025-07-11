package net.bettercombat.logic;

public interface PlayerAttackProperties {
    int getComboCount();
    void setComboCount(int comboCount);
    int getHitsCount();
    void updateHitsCount(int hitsCount, long lastHitTime);
    void resetHitsCount();
}
