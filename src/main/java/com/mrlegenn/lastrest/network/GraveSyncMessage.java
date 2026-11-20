package com.mrlegenn.lastrest.network;

import com.mrlegenn.lastrest.client.ClientGraveData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GraveSyncMessage {
    public record Marker(String ownerName, BlockPos position) {}

    private final List<Marker> markers;

    public GraveSyncMessage(List<Marker> markers) {
        this.markers = List.copyOf(markers);
    }

    public static void encode(GraveSyncMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.markers.size());
        for (Marker marker : message.markers) {
            buffer.writeUtf(marker.ownerName(), 64);
            buffer.writeBlockPos(marker.position());
        }
    }

    public static GraveSyncMessage decode(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 10000);
        List<Marker> markers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            markers.add(new Marker(buffer.readUtf(64), buffer.readBlockPos()));
        }
        return new GraveSyncMessage(markers);
    }

    public static void handle(GraveSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientGraveData.set(message.markers))
        );
        context.setPacketHandled(true);
    }
}
