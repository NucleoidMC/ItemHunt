package io.github.jerozgen.itemhunt.game;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import xyz.nucleoid.fantasy.RuntimeLevel;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

public class LazyLoadingWorld extends RuntimeLevel {
    private final LevelChunk chunk;

    protected LazyLoadingWorld(MinecraftServer server, ResourceKey<Level> registryKey, RuntimeLevelConfig config, Style style) {
        super(server, registryKey, config, style);
        var biome = server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.THE_VOID);
        this.chunk = new EmptyLevelChunk(this, ChunkPos.ZERO, biome) {
            @Override
            public @NonNull BlockState getBlockState(BlockPos pos) {
                return Blocks.BARRIER.defaultBlockState();
            }
        };
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, @NonNull ChunkStatus leastStatus, boolean create) {
        return this.chunk;
    }
}
