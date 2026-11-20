package com.mrlegenn.lastrest.data;

import com.mrlegenn.lastrest.LastRest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.chunk.ChunkPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GraveSavedData extends SavedData {
    private static final String DATA_NAME = "lastrest_graves";

    private final List<GraveRecord> graves = new ArrayList<>();
    private final List<PendingCleanup> pendingCleanup = new ArrayList<>();

    public static GraveSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                GraveSavedData::load, GraveSavedData::new, DATA_NAME
        );
    }

    public static GraveSavedData load(CompoundTag tag) {
        GraveSavedData data = new GraveSavedData();
        ListTag graveList = tag.getList("Graves", Tag.TAG_COMPOUND);
        for (int i = 0; i < graveList.size(); i++) {
            CompoundTag entry = graveList.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension == null || !entry.hasUUID("Owner")) continue;

            List<ItemStack> items = new ArrayList<>();
            ListTag itemList = entry.getList("Items", Tag.TAG_COMPOUND);
            for (int j = 0; j < itemList.size(); j++) {
                ItemStack stack = ItemStack.of(itemList.getCompound(j));
                if (!stack.isEmpty()) items.add(stack);
            }

            data.graves.add(new GraveRecord(
                    entry.getUUID("Owner"),
                    entry.getString("OwnerName"),
                    dimension,
                    BlockPos.of(entry.getLong("Position")),
                    Math.max(0L, entry.getLong("RemainingTicks")),
                    items
            ));
        }

        ListTag cleanupList = tag.getList("PendingCleanup", Tag.TAG_COMPOUND);
        for (int i = 0; i < cleanupList.size(); i++) {
            CompoundTag entry = cleanupList.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension != null) {
                data.pendingCleanup.add(new PendingCleanup(dimension, BlockPos.of(entry.getLong("Position"))));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag graveList = new ListTag();
        for (GraveRecord grave : graves) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", grave.owner());
            entry.putString("OwnerName", grave.ownerName());
            entry.putString("Dimension", grave.dimension().toString());
            entry.putLong("Position", grave.position().asLong());
            entry.putLong("RemainingTicks", grave.remainingTicks());

            ListTag itemList = new ListTag();
            for (ItemStack stack : grave.items()) {
                if (!stack.isEmpty()) itemList.add(stack.save(new CompoundTag()));
            }
            entry.put("Items", itemList);
            graveList.add(entry);
        }
        tag.put("Graves", graveList);

        ListTag cleanupList = new ListTag();
        for (PendingCleanup cleanup : pendingCleanup) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", cleanup.dimension().toString());
            entry.putLong("Position", cleanup.position().asLong());
            cleanupList.add(entry);
        }
        tag.put("PendingCleanup", cleanupList);
        return tag;
    }

    public List<GraveRecord> graves() {
        return new ArrayList<>(graves);
    }

    public void add(GraveRecord grave) {
        graves.add(grave);
        setDirty();
    }

    public void remove(GraveRecord grave) {
        graves.remove(grave);
        setDirty();
    }

    public GraveRecord find(ResourceLocation dimension, BlockPos position) {
        for (GraveRecord grave : graves) {
            if (grave.dimension().equals(dimension) && grave.position().equals(position)) return grave;
        }
        return null;
    }

    /**
     * Advances every grave once per server tick from the overworld tick hook.
     * Returns true when at least one grave expired, so clients can refresh the HUD.
     */
    public boolean tick(MinecraftServer server) {
        boolean changed = false;
        boolean expiredAny = false;
        Iterator<GraveRecord> iterator = graves.iterator();
        while (iterator.hasNext()) {
            GraveRecord grave = iterator.next();
            long next = grave.remainingTicks() - 1L;
            grave.setRemainingTicks(next);
            changed = true;

            if (next <= 0L) {
                removeOrScheduleCleanup(server, grave.dimension(), grave.position());
                iterator.remove();
                expiredAny = true;
            }
        }
        if (changed) setDirty();
        return expiredAny;
    }

    public void cleanupLoadedChunk(ServerLevel level, ChunkPos chunkPos) {
        Iterator<PendingCleanup> iterator = pendingCleanup.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            PendingCleanup cleanup = iterator.next();
            BlockPos pos = cleanup.position();
            if (!cleanup.dimension().equals(level.dimension().location())
                    || (pos.getX() >> 4) != chunkPos.x
                    || (pos.getZ() >> 4) != chunkPos.z) continue;

            if (level.getBlockState(pos).is(LastRest.GRAVE_BLOCK.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            iterator.remove();
            changed = true;
        }
        if (changed) setDirty();
    }

    private void removeOrScheduleCleanup(MinecraftServer server, ResourceLocation dimension, BlockPos pos) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
        ServerLevel level = server.getLevel(key);
        if (level == null) {
            pendingCleanup.add(new PendingCleanup(dimension, pos));
            return;
        }

        if (level.hasChunkAt(pos)) {
            if (level.getBlockState(pos).is(LastRest.GRAVE_BLOCK.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        } else {
            pendingCleanup.add(new PendingCleanup(dimension, pos));
        }
    }

    private record PendingCleanup(ResourceLocation dimension, BlockPos position) {}
}
