package com.iggy.iggytech.Blocks.entities;

import com.iggy.iggytech.iggytech;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "iggytech");

    public static Supplier<BlockEntityType<ConveyorBeltBlockEntity>> CONVEYOR_BELT_BE =
            BLOCK_ENTITIES.register("conveyor_belt_be", () -> BlockEntityType.Builder.of(
                    ConveyorBeltBlockEntity::new, iggytech.CONVEYOR_BELT.get()).build(null));

    public static void register(IEventBus eventBus){
        BLOCK_ENTITIES.register(eventBus);
    }
}
