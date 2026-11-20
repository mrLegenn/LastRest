package com.mrlegenn.lastrest;

import com.mrlegenn.lastrest.block.GraveBlock;
import com.mrlegenn.lastrest.network.NetworkHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(LastRest.MOD_ID)
public class LastRest {
    public static final String MOD_ID = "lastrest";

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<Block> GRAVE_BLOCK = BLOCKS.register(
            "grave",
            () -> new GraveBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F))
    );

    public static final RegistryObject<Item> GRAVE_ITEM = ITEMS.register(
            "grave",
            () -> new BlockItem(GRAVE_BLOCK.get(), new Item.Properties())
    );

    public static final RegistryObject<CreativeModeTab> GRAVE_TAB = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.lastrest"))
                    .icon(() -> new ItemStack(GRAVE_ITEM.get()))
                    .displayItems((parameters, output) -> output.accept(GRAVE_ITEM.get()))
                    .build()
    );

    public LastRest() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);

        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(new GraveEvents());
    }
}
