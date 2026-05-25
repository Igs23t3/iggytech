package com.iggy.iggytech.Blocks.entities;

import com.iggy.iggytech.iggytech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class ConveyorBeltBlockEntity extends BlockEntity {
    public ConveyorBeltBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CONVEYOR_BELT_BE.get(), pos, blockState);
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level) {
        return level.isClientSide ? null : (lvl, pos, state, be) -> ((ConveyorBeltBlockEntity) be).tick(lvl, pos, state);
    }

    public final ItemStackHandler inventory = new ItemStackHandler(1){
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()){
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    public void clearSlot() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public void setItem(Item item, int amount) {
        inventory.setStackInSlot(0, new ItemStack(item, amount));
    }

    public boolean tryInsertItem(ItemStack stack) {
        if (inventory.getStackInSlot(0).isEmpty()) {
            inventory.setStackInSlot(0, stack.copy());
            return true;
        }
        return false;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        if (!inventory.getStackInSlot(0).isEmpty()) return; // belt is full, don't bother

        AABB area = new AABB(
                pos.getX(),           // min X
                pos.getY() + 3/16.0, // min Y (top of belt surface)
                pos.getZ(),           // min Z
                pos.getX() + 1,       // max X
                pos.getY() + 4/16.0,       // max Y (generous headroom)
                pos.getZ() + 1        // max Z
        );
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area,
                entity -> !entity.isRemoved()); // only alive entities

        if (items.isEmpty()) return;

        ItemEntity item = items.get(0); // just grab the first one
        if (tryInsertItem(item.getItem())) {
            item.discard();
        }
    }
}
