package com.mrlegenn.lastrest.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GraveRecord {
    private final UUID owner;
    private final String ownerName;
    private final ResourceLocation dimension;
    private final BlockPos position;
    private long remainingTicks;
    private final List<ItemStack> items;

    public GraveRecord(UUID owner, String ownerName, ResourceLocation dimension,
                       BlockPos position, long remainingTicks, List<ItemStack> items) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.dimension = dimension;
        this.position = position.immutable();
        this.remainingTicks = remainingTicks;
        this.items = new ArrayList<>(items);
    }

    public UUID owner() { return owner; }
    public String ownerName() { return ownerName; }
    public ResourceLocation dimension() { return dimension; }
    public BlockPos position() { return position; }
    public long remainingTicks() { return remainingTicks; }
    public void setRemainingTicks(long value) { remainingTicks = value; }
    public List<ItemStack> items() { return items; }
}
