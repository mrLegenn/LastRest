package com.mrlegenn.lastrest.network;

import com.mrlegenn.lastrest.data.GraveRecord;
import com.mrlegenn.lastrest.data.GraveSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("lastrest", "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static boolean registered;

    private NetworkHandler() {}

    public static void register() {
        if (registered) return;
        CHANNEL.messageBuilder(GraveSyncMessage.class, 0)
                .encoder(GraveSyncMessage::encode)
                .decoder(GraveSyncMessage::decode)
                .consumerMainThread(GraveSyncMessage::handle)
                .add();
        registered = true;
    }

    public static void syncPlayer(ServerPlayer player) {
        if (player.getServer() == null) return;
        ResourceLocation dimension = player.level().dimension().location();
        List<GraveSyncMessage.Marker> markers = markersFor(player.getServer(), dimension);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GraveSyncMessage(markers));
    }

    public static void syncDimension(MinecraftServer server, ResourceLocation dimension) {
        List<GraveSyncMessage.Marker> markers = markersFor(server, dimension);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension().location().equals(dimension)) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GraveSyncMessage(markers));
            }
        }
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player);
        }
    }

    private static List<GraveSyncMessage.Marker> markersFor(MinecraftServer server, ResourceLocation dimension) {
        List<GraveSyncMessage.Marker> markers = new ArrayList<>();
        for (GraveRecord grave : GraveSavedData.get(server).graves()) {
            if (grave.dimension().equals(dimension)) {
                markers.add(new GraveSyncMessage.Marker(grave.ownerName(), grave.position()));
            }
        }
        return markers;
    }
}
