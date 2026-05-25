package com.iggy.iggytech.Blocks.entities;

import com.iggy.iggytech.Blocks.ConveyorBeltBlock;
import com.iggy.iggytech.iggytech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
            if (level != null && !level.isClientSide()) {
                iggytech.LOGGER.info("sending block update");
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }


    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        iggytech.LOGGER.info("onDataPacket called");
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), registries);
        }
    }

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
        if (level.getGameTime() % 2 != 0) return;

        // try to pick up items if empty
        if (inventory.getStackInSlot(0).isEmpty()) {
            AABB area = new AABB(pos).inflate(0.5, 1, 0.5).move(0, 0.5, 0);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area,
                    entity -> !entity.isRemoved());
            if (items.isEmpty()) return;
            ItemEntity item = items.get(0);
            if (tryInsertItem(item.getItem())) {
                item.discard();
            }
            return;
        }

        // every 20 ticks try to move item forward
        if (level.getGameTime() % 20 != 0) return;

        Direction facing = state.getValue(ConveyorBeltBlock.FACING);
        BlockPos frontPos = pos.relative(facing);
        BlockEntity frontBe = level.getBlockEntity(frontPos);

        if (frontBe instanceof ConveyorBeltBlockEntity frontBelt) {
            // try to insert into the next belt
            if (frontBelt.tryInsertItem(inventory.getStackInSlot(0))) {
                clearSlot();
            }
            // if full, just wait
        }
        // no belt in front, just wait
    }
}
