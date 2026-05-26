package com.iggy.iggytech.common;

import com.iggy.iggytech.Blocks.ConveyorBeltBlock;
import com.iggy.iggytech.Blocks.entities.ConveyorBeltBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = "iggytech")
public class ConveyorBeltSystem {

    // adjacency list: each belt -> the belt it points to (its output)
    private static final Map<ResourceKey<Level>, Map<BlockPos, BlockPos>> graph = new HashMap<>();

    // reverse adjacency: each belt -> all belts pointing into it (its inputs)
    private static final Map<ResourceKey<Level>, Map<BlockPos, Set<BlockPos>>> reverseGraph = new HashMap<>();

    // the sorted processing order, rebuilt when topology changes
    private static final Map<ResourceKey<Level>, List<BlockPos>> sortedBelts = new HashMap<>();

    private static Map<BlockPos, BlockPos> getGraph(Level level) {
        return graph.computeIfAbsent(level.dimension(), k -> new HashMap<>());
    }

    private static Map<BlockPos, Set<BlockPos>> getReverseGraph(Level level) {
        return reverseGraph.computeIfAbsent(level.dimension(), k -> new HashMap<>());
    }

    public static void onBeltPlaced(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        Direction facing = state.getValue(ConveyorBeltBlock.FACING);
        BlockPos frontPos = pos.relative(facing).immutable();
        pos = pos.immutable();

        // if there's a belt in front, add the edge
        if (level.getBlockEntity(frontPos) instanceof ConveyorBeltBlockEntity) {
            getGraph(level).put(pos, frontPos);
            getReverseGraph(level).computeIfAbsent(frontPos, k -> new HashSet<>()).add(pos);
        }

        // if there's a belt behind pointing at us, update its edge
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir).immutable();
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor instanceof ConveyorBeltBlockEntity) {
                Direction neighborFacing = neighbor.getBlockState().getValue(ConveyorBeltBlock.FACING);
                if (neighborPos.relative(neighborFacing).equals(pos)) {
                    getGraph(level).put(neighborPos, pos);
                    getReverseGraph(level).computeIfAbsent(pos, k -> new HashSet<>()).add(neighborPos);
                }
            }
        }

        dirtyDimensions.add(level.dimension());
    }

    public static void onBeltRemoved(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        pos = pos.immutable();

        // remove this belt's output edge
        BlockPos output = getGraph(level).remove(pos);
        if (output != null) {
            Set<BlockPos> inputs = getReverseGraph(level).get(output);
            if (inputs != null) inputs.remove(pos);
        }

        // remove all edges pointing into this belt
        Set<BlockPos> inputs = getReverseGraph(level).remove(pos);
        if (inputs != null) {
            for (BlockPos input : inputs) {
                getGraph(level).remove(input);
            }
        }

        dirtyDimensions.add(level.dimension());
    }

    private static void rebuildSort(Level level) {
        Map<BlockPos, BlockPos> g = getGraph(level);
        Map<BlockPos, Set<BlockPos>> rg = getReverseGraph(level);

        // count inputs for each node (Kahn's algorithm)
        Map<BlockPos, Integer> inputCount = new HashMap<>();
        for (BlockPos pos : g.keySet()) {
            inputCount.putIfAbsent(pos, 0);
            BlockPos output = g.get(pos);
            if (output != null) {
                inputCount.merge(output, 1, Integer::sum);
            }
        }

        // start with nodes that have no inputs
        Queue<BlockPos> queue = new LinkedList<>();
        for (Map.Entry<BlockPos, Integer> entry : inputCount.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<BlockPos> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            sorted.add(current);
            BlockPos output = g.get(current);
            if (output != null) {
                int remaining = inputCount.merge(output, -1, Integer::sum);
                if (remaining == 0) queue.add(output);
            }
        }

        // any nodes not in sorted are part of a loop, we just skip them

        Collections.reverse(sorted);
        sortedBelts.put(level.dimension(), sorted);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (dirtyDimensions.remove(level.dimension())) {
                rebuildSort(level);
            }
            if (level.getGameTime() % 20 != 0) continue;
            processBelts(level);
        }
    }

    private static void processBelts(ServerLevel level) {
        List<BlockPos> sorted = sortedBelts.getOrDefault(level.dimension(), List.of());

        for (BlockPos pos : sorted) {
            if (!(level.getBlockEntity(pos) instanceof ConveyorBeltBlockEntity belt)) continue;

            BlockPos output = getGraph(level).get(pos);
            if (output == null) continue; // this is a tail, nothing to push to

            if (!(level.getBlockEntity(output) instanceof ConveyorBeltBlockEntity outputBelt)) continue;

            ItemStack item = belt.inventory.getStackInSlot(0);
            if (item.isEmpty()) continue;
            if (!outputBelt.inventory.getStackInSlot(0).isEmpty()) continue;

            outputBelt.tryInsertItem(item);
            belt.clearSlot();
        }
    }

    private static final Set<ResourceKey<Level>> dirtyDimensions = new HashSet<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        LevelChunk chunk = level.getChunk(event.getChunk().getPos().x, event.getChunk().getPos().z);
        chunk.getBlockEntities().forEach((pos, be) -> {
            if (!(be instanceof ConveyorBeltBlockEntity)) return;
            BlockState state = level.getBlockState(pos);
            Direction facing = state.getValue(ConveyorBeltBlock.FACING);
            BlockPos frontPos = pos.relative(facing).immutable();
            BlockPos immutablePos = pos.immutable();

            // only add edge if front chunk is already loaded
            if (level.isLoaded(frontPos) && level.getBlockEntity(frontPos) instanceof ConveyorBeltBlockEntity) {
                getGraph(level).put(immutablePos, frontPos);
                getReverseGraph(level).computeIfAbsent(frontPos, k -> new HashSet<>()).add(immutablePos);
            }
        });
        dirtyDimensions.add(level.dimension());
    }
}