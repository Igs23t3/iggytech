package com.iggy.iggytech;

import com.iggy.iggytech.Blocks.ConveyorBeltBlock;
import com.iggy.iggytech.Blocks.entities.ModBlockEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;


import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;


import net.neoforged.neoforge.common.NeoForge;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(iggytech.MODID)
public class iggytech {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "iggytech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredBlock<ConveyorBeltBlock> CONVEYOR_BELT = BLOCKS.registerBlock(
            "conveyor_belt",
            ConveyorBeltBlock::new,
            BlockBehaviour.Properties.of().noOcclusion()
    );

    public static final DeferredItem<BlockItem> CONVEYOR_BELT_ITEM = ITEMS.registerSimpleBlockItem("conveyor_belt", CONVEYOR_BELT);

    public iggytech(IEventBus modEventBus, ModContainer modContainer) {
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
    }
}


