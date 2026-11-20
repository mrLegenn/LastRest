package com.mrlegenn.lastrest.client;

import com.mrlegenn.lastrest.LastRest;
import com.mrlegenn.lastrest.network.GraveSyncMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = LastRest.MOD_ID, value = Dist.CLIENT)
public final class ClientHud {
    private static final int ROWS_PER_PAGE = 8;

    private ClientHud() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ClientKeybinds.NEXT_PAGE != null) {
            while (ClientKeybinds.NEXT_PAGE.consumeClick()) ClientGraveData.nextPage();
        }
        if (ClientKeybinds.PREVIOUS_PAGE != null) {
            while (ClientKeybinds.PREVIOUS_PAGE.consumeClick()) ClientGraveData.previousPage();
        }
    }

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        List<GraveSyncMessage.Marker> markers = new ArrayList<>(ClientGraveData.markers());
        if (markers.isEmpty()) return;

        markers.sort(Comparator.comparingDouble(marker ->
                minecraft.player.distanceToSqr(
                        marker.position().getX() + 0.5,
                        marker.position().getY() + 0.5,
                        marker.position().getZ() + 0.5
                )));

        int total = markers.size();
        int pages = Math.max(1, (total + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        int page = Math.min(ClientGraveData.page(), pages - 1);
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(total, start + ROWS_PER_PAGE);
        int rowCount = end - start;

        int left = 8;
        int top = 8;
        int width = Math.min(310, minecraft.getWindow().getGuiScaledWidth() - 16);
        int height = 24 + rowCount * 12 + 16;
        var graphics = event.getGuiGraphics();

        graphics.fill(left, top, left + width, top + height, 0xA9000000);
        graphics.fill(left, top, left + width, top + 2, 0xFF9A9A9A);
        graphics.drawString(minecraft.font, "LastRest  |  Lápides: " + total,
                left + 7, top + 6, 0xFFFFFFFF, false);

        for (int i = start; i < end; i++) {
            GraveSyncMessage.Marker marker = markers.get(i);
            double dx = marker.position().getX() + 0.5 - minecraft.player.getX();
            double dz = marker.position().getZ() + 0.5 - minecraft.player.getZ();
            double distance = Math.sqrt(dx * dx
                    + Math.pow(marker.position().getY() + 0.5 - minecraft.player.getY(), 2)
                    + dz * dz);

            double bearing = Math.toDegrees(Math.atan2(-dx, dz));
            float difference = Mth.wrapDegrees((float) (bearing - minecraft.player.getYRot()));
            String arrow;
            if (Math.abs(difference) <= 30.0F) arrow = "↑";
            else if (Math.abs(difference) >= 150.0F) arrow = "↓";
            else if (difference < 0.0F) arrow = "→";
            else arrow = "←";

            String row = arrow + "  " + marker.ownerName() + "  •  " + Math.round(distance) + " m";
            graphics.drawString(minecraft.font, row, left + 7,
                    top + 22 + (i - start) * 12, 0xFFEFEFEF, false);
        }

        graphics.drawString(minecraft.font,
                "Página " + (page + 1) + "/" + pages + "  |  Page Up / Page Down",
                left + 7, top + height - 12, 0xFFB8B8B8, false);
    }
}
