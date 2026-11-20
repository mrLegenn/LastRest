package com.mrlegenn.lastrest.block;

import com.mrlegenn.lastrest.LastRest;
import com.mrlegenn.lastrest.data.GraveRecord;
import com.mrlegenn.lastrest.data.GraveSavedData;
import com.mrlegenn.lastrest.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;

public class GraveBlock extends Block {
    public GraveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || player.getServer() == null) {
            return InteractionResult.CONSUME;
        }

        GraveSavedData data = GraveSavedData.get(player.getServer());
        GraveRecord grave = data.find(level.dimension().location(), pos);
        if (grave == null) {
            player.displayClientMessage(Component.literal("Esta lápide não contém mais itens."), true);
            level.removeBlock(pos, false);
            return InteractionResult.CONSUME;
        }

        int recovered = recoverIntoInventory(serverPlayer, grave);
        if (recovered == 0) {
            player.displayClientMessage(Component.literal("Inventário cheio. Libere espaço para recuperar os itens."), true);
            return InteractionResult.CONSUME;
        }

        boolean empty = grave.items().stream().allMatch(ItemStack::isEmpty);
        if (empty) {
            data.remove(grave);
            level.removeBlock(pos, false);
            player.displayClientMessage(Component.literal("Você recuperou todos os itens desta lápide."), true);
        } else {
            data.setDirty();
            player.displayClientMessage(Component.literal("Itens recuperados: " + recovered + ". Ainda há itens na lápide."), true);
        }

        NetworkHandler.syncDimension(player.getServer(), level.dimension().location());
        return InteractionResult.CONSUME;
    }

    private static int recoverIntoInventory(ServerPlayer player, GraveRecord grave) {
        int moved = 0;

        // First pass: merge into compatible stacks, including armor and offhand slots.
        for (ItemStack stored : grave.items()) {
            if (stored.isEmpty()) continue;

            for (int slot = 0; slot < player.getInventory().getContainerSize() && !stored.isEmpty(); slot++) {
                ItemStack current = player.getInventory().getItem(slot);
                if (current.isEmpty() || !ItemStack.isSameItemSameTags(current, stored)) continue;

                int limit = Math.min(current.getMaxStackSize(), player.getInventory().getMaxStackSize());
                int space = limit - current.getCount();
                if (space <= 0) continue;

                int amount = Math.min(space, stored.getCount());
                current.grow(amount);
                stored.shrink(amount);
                moved += amount;
            }
        }

        // Second pass: put remaining items in empty main-inventory slots (0-35).
        for (ItemStack stored : grave.items()) {
            if (stored.isEmpty()) continue;

            for (int slot = 0; slot < 36 && !stored.isEmpty(); slot++) {
                if (!player.getInventory().getItem(slot).isEmpty()) continue;

                int amount = Math.min(stored.getCount(), stored.getMaxStackSize());
                player.getInventory().setItem(slot, stored.split(amount));
                moved += amount;
            }
        }

        player.getInventory().setChanged();
        return moved;
    }
}
