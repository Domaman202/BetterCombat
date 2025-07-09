package net.bettercombat.fabric.client;

import net.bettercombat.client.ClientNetwork;
import net.bettercombat.fabric.network.FabricServerNetwork;
import net.bettercombat.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class FabricClientNetwork {
    public static void init() {
        ClientConfigurationNetworking.registerGlobalReceiver(Packets.WeaponRegistrySync.PACKET_ID, (packet, context) -> {
            ClientNetwork.handleWeaponRegistrySync(packet);
            System.out.println("Send Ack!");
            context.responseSender().sendPacket(new Packets.Ack(FabricServerNetwork.WeaponRegistrySyncTask.name));
        });

        ClientConfigurationNetworking.registerGlobalReceiver(Packets.ConfigSync.PACKET_ID, (packet, context) -> {
            ClientNetwork.handleConfigSync(packet);
            System.out.println("Send Ack!");
            context.responseSender().sendPacket(new Packets.Ack(FabricServerNetwork.ConfigurationTask.name));
        });

        ClientPlayNetworking.registerGlobalReceiver(Packets.AttackAnimation.PACKET_ID, (packet, context) -> {
            MinecraftClient.getInstance().player.sendMessage(Text.of("PACKET ATTACK ANIMATION!!!"));
            ClientNetwork.handleAttackAnimation(packet);
        });

        ClientPlayNetworking.registerGlobalReceiver(Packets.AttackSound.PACKET_ID, (packet, context) -> {
            MinecraftClient.getInstance().player.sendMessage(Text.of("PACKET ATTACK SOUND!!!"));
            ClientNetwork.handleAttackSound(packet);
        });
    }
}
