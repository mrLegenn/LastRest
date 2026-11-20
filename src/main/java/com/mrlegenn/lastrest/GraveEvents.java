package com.mrlegenn.lastrest;

import com.mrlegenn.lastrest.data.GraveRecord;
import com.mrlegenn.lastrest.data.GraveSavedData;
import com.mrlegenn.lastrest.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class GraveEvents {
    private static final long GRAVE_LIFETIME_TICKS = 30L * 60L * 20L;

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // Keep vanilla keepInventory behavior unchanged.
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        List<ItemStack> captured = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) captured.add(stack.copy());
        }
        if (captured.isEmpty()) return;

        BlockPos gravePos = findPlacement(level, player.blockPosition());
        player.getInventory().clearContent();

        level.setBlockAndUpdate(gravePos, LastRest.GRAVE_BLOCK.get().defaultBlockState());
        GraveRecord grave = new GraveRecord(
                player.getUUID(),
                player.getGameProfile().getName(),
                level.dimension().location(),
                gravePos,
                GRAVE_LIFETIME_TICKS,
                captured
        );
        GraveSavedData.get(player.getServer()).add(grave);
        NetworkHandler.syncDimension(player.getServer(), level.dimension().location());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            NetworkHandler.syncPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            NetworkHandler.syncPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            NetworkHandler.syncPlayer(player);
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        MinecraftServer server = level.getServer();
        boolean expired = GraveSavedData.get(server).tick(server);
        if (expired) NetworkHandler.syncAll(server);
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            GraveSavedData.get(level.getServer()).cleanupLoadedChunk(level, event.getChunk().getPos());
        }
    }

    private static BlockPos findPlacement(ServerLevel level, BlockPos origin) {
        List<BlockPos> candidates = List.of(
                origin, origin.above(), origin.north(), origin.south(),
                origin.east(), origin.west(), origin.north().above(),
                origin.south().above(), origin.east().above(), origin.west().above()
        );

        for (BlockPos pos : candidates) {
            if (!level.getBlockState(pos).canBeReplaced()) continue;
            if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return pos;
        }

        // Last resort: preserve the items in saved data even in unusual terrain.
        return origin;
    }
}
