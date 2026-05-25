package com.iggy.iggytech.Blocks.entities;

import com.iggy.iggytech.iggytech;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ConveyorBeltBlockEntity extends BlockEntity {
    public ConveyorBeltBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CONVEYOR_BELT_BE.get(), pos, blockState);
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level) {
        return level.isClientSide ? null : (lvl, pos, state, be) -> ((ConveyorBeltBlockEntity) be).tick(lvl, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        AABB area = new AABB(pos.above()); // 1x1x1 box one block above
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);

        if (!level.isClientSide) {
            iggytech.LOGGER.info("belt ticking at {}", pos);
        }

        for (ItemEntity item : items) {
            item.discard(); // deletes the entity
        }
    }
}
