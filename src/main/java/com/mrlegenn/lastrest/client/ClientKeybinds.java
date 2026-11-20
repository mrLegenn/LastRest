package com.mrlegenn.lastrest.client;

import com.mrlegenn.lastrest.LastRest;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = LastRest.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientKeybinds {
    public static KeyMapping NEXT_PAGE;
    public static KeyMapping PREVIOUS_PAGE;

    private ClientKeybinds() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        NEXT_PAGE = new KeyMapping("key.lastrest.next_page", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_PAGE_DOWN, "key.categories.lastrest");
        PREVIOUS_PAGE = new KeyMapping("key.lastrest.previous_page", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_PAGE_UP, "key.categories.lastrest");
        event.register(NEXT_PAGE);
        event.register(PREVIOUS_PAGE);
    }
}
