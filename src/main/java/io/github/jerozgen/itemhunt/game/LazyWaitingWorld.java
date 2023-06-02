package io.github.jerozgen.itemhunt.game;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorld;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

public class LazyWaitingWorld extends RuntimeWorld {
    private final WorldChunk chunk;

    protected LazyWaitingWorld(MinecraftServer server, RegistryKey<World> registryKey, RuntimeWorldConfig config, Style style) {
        super(server, registryKey, config, style);
        this.chunk = new EmptyChunk(this, ChunkPos.ORIGIN, server.getRegistryManager().get(RegistryKeys.BIOME).getEntry(BiomeKeys.THE_VOID).get()) {
            @Override
            public BlockState getBlockState(BlockPos pos) {
                return Blocks.BARRIER.getDefaultState();
            }
        };
    }

    @Nullable
    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        return this.chunk;
    }
}
