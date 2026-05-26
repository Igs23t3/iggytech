package com.iggy.iggytech.Blocks.entities.renderer;

import com.iggy.iggytech.Blocks.ConveyorBeltBlock;
import com.iggy.iggytech.Blocks.entities.ConveyorBeltBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class ConveyorBeltBlockEntityRenderer implements BlockEntityRenderer<ConveyorBeltBlockEntity> {
    public ConveyorBeltBlockEntityRenderer(BlockEntityRendererProvider.Context context){

    }

    @Override
    public void render(ConveyorBeltBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = be.inventory.getStackInSlot(0);
        if (stack.isEmpty()) return;

        BlockState state = be.getBlockState();
        Direction facing = state.getValue(ConveyorBeltBlock.FACING);

        float yRot = switch (facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };

        // global synchronized timer, 0.0 to 1.0 over 20 ticks
        float time = (Minecraft.getInstance().level.getGameTime() % 20 + partialTick) / 20f;
        if (time < 0.05f) return;

        pose.pushPose();
        pose.translate(0.5, 0.2, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yRot));
        pose.translate(0, 0, -time); // move along Z before rotating, facing handles direction
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, pose, bufferSource, null, 0
        );
        pose.popPose();
    }

}
