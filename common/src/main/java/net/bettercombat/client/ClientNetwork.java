package net.bettercombat.client;

import net.bettercombat.BetterCombatMod;
import net.bettercombat.Platform;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.WeaponRegistry;
import net.bettercombat.network.Packets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ClientNetwork {
    public static void handleWeaponRegistrySync(Packets.WeaponRegistrySync packet) {
        WeaponRegistry.decodeRegistry(packet);
    }

    public static void handleConfigSync(Packets.ConfigSync packet) {
        BetterCombatMod.LOGGER.info("Received config sync packet");
        BetterCombatMod.config = packet.deserialized();
        BetterCombatClientMod.ENABLED = true;
    }

    public static void handleAttackAnimation(Packets.AttackAnimation packet) {
        var client = MinecraftClient.getInstance();
        client.execute(() -> {
            var entity = client.world.getEntityById(packet.playerId());
            if (entity instanceof PlayerEntity player
                    // Avoid local playback, unless replay mod is loaded
                    && (player != client.player || Platform.isModLoaded("replaymod")) ) {
                if (packet.animationName().equals(Packets.AttackAnimation.StopSymbol)) {
                    ((PlayerAttackAnimatable) entity).stopAttackAnimation(packet.length());
                } else {
                    ((PlayerAttackAnimatable) entity).playAttackAnimation(packet.animationName(), packet.animatedHand(), packet.length(), packet.upswing());
                }
            }
        });
    }

    public static void handleAttackSound(Packets.AttackSound packet) {
        var client = MinecraftClient.getInstance();
        client.execute(() -> {
            try {
                if (BetterCombatClientMod.config.weaponSwingSoundVolume == 0) {
                    return;
                }

                var soundEvent = Registries.SOUND_EVENT.get(Identifier.of(packet.soundId()));
                var configVolume = BetterCombatClientMod.config.weaponSwingSoundVolume;
                var volume = packet.volume() * ((float) Math.min(Math.max(configVolume, 0), 100) / 100F);
                client.world.playSoundClient(
                        packet.x(),
                        packet.y(),
                        packet.z(),
                        soundEvent,
                        SoundCategory.PLAYERS,
                        volume,
                        packet.pitch(),
                        true);
            } catch (Exception e) {
                BetterCombatMod.LOGGER.trace("Play sound error", e);
            }
        });
    }
}
