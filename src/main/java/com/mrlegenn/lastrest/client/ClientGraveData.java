package com.mrlegenn.lastrest.client;

import com.mrlegenn.lastrest.network.GraveSyncMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ClientGraveData {
    private static List<GraveSyncMessage.Marker> markers = List.of();
    private static int page;

    private ClientGraveData() {}

    public static void set(List<GraveSyncMessage.Marker> newMarkers) {
        markers = List.copyOf(newMarkers);
        page = Math.max(0, Math.min(page, Math.max(0, (markers.size() - 1) / 8)));
    }

    public static List<GraveSyncMessage.Marker> markers() {
        return markers;
    }

    public static int page() {
        return page;
    }

    public static void nextPage() {
        int pages = Math.max(1, (markers.size() + 7) / 8);
        page = (page + 1) % pages;
    }

    public static void previousPage() {
        int pages = Math.max(1, (markers.size() + 7) / 8);
        page = (page - 1 + pages) % pages;
    }
}
