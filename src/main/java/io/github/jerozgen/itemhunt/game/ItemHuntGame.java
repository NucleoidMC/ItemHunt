package io.github.jerozgen.itemhunt.game;

import io.github.jerozgen.itemhunt.event.StatusEffectAddEvent;
import io.github.jerozgen.itemhunt.game.phase.ItemHuntLoadingPhase;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.network.protocol.Packet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;

import java.util.function.Consumer;

public record ItemHuntGame(ItemHuntConfig config, GameSpace gameSpace, ServerLevel world, BlockPos spawnPos, GameStatisticBundle statistics) {

    public static GameOpenProcedure open(GameOpenContext<ItemHuntConfig> context) {
        var config = context.config();
        var dimensionOptions = config.dimensionOptions();
        var worldConfig = new RuntimeLevelConfig()
                .setDimensionType(dimensionOptions.type())
                .setGenerator(dimensionOptions.generator())
                .setSeed(RandomSource.create().nextLong());
        var loadingWorldConfig = new RuntimeLevelConfig()
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                .setGenerator(new VoidChunkGenerator(context.server().registryAccess().lookupOrThrow(Registries.BIOME)))
                .setLevelConstructor(LazyLoadingWorld::new);
        return context.open((activity) -> {
            var gameSpace = activity.getGameSpace();
            var loadingWorld = gameSpace.getLevels().add(loadingWorldConfig);
            var world = gameSpace.getLevels().add(worldConfig);
            var statistics = config.statisticBundleNamespace()
                    .map(value -> gameSpace.getStatistics().bundle(value))
                    .orElse(null);
            var game = new ItemHuntGame(config, gameSpace, world, findSpawnPos(world), statistics);
            activity.listen(GameActivityEvents.CREATE, () -> {
                var loadingPhase = new ItemHuntLoadingPhase(game, loadingWorld);
                game.gameSpace.setActivity(loadingPhase::setup);
            });
        });
    }

    public ItemHuntGame {
        world.getWorldBorder().addListener(getWorldBorderListener());
    }

    public void setup(GameActivity activity) {
        activity.listen(PlayerDamageEvent.EVENT, (player, source, amount) -> EventResult.DENY);
        activity.listen(StatusEffectAddEvent.EVENT, this::onAddStatusEffect);

        if (!config.crafting())
            activity.deny(GameRuleType.CRAFTING);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.PORTALS);
    }

    private InteractionResult onAddStatusEffect(Entity entity, MobEffectInstance instance, @Nullable Entity source) {
        if (entity instanceof ServerPlayer && instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    public void stat(Consumer<GameStatisticBundle> consumer) {
        if (statistics != null) consumer.accept(statistics);
    }

    public void sendToAll(Packet<?> packet) {
        for (var player : gameSpace.getPlayers()) {
            player.connection.send(packet);
        }
    }

    private static BlockPos findSpawnPos(ServerLevel world) {
        var chunkManager = world.getChunkSource();
        var noiseConfig = chunkManager.randomState();
        var chunkGenerator = chunkManager.getGenerator();
        var startChunkPos = ChunkPos.containing(noiseConfig.sampler().findSpawnPosition());

        var dx = 0;
        var dz = 0;
        var stepX = 0;
        var stepZ = -1;
        for (var i = 0; i < 11 * 11; i++) {
            if (dx >= -5 && dx <= 5 && dz >= -5 && dz <= 5) {
                var chunkPos = new ChunkPos(startChunkPos.x() + dx, startChunkPos.z() + dz);
                var x = chunkPos.getMinBlockX() + 8;
                var z = chunkPos.getMinBlockZ() + 8;
                var y = chunkGenerator.getFirstFreeHeight(x, z, Heightmap.Types.MOTION_BLOCKING, world, noiseConfig);
                var oceanFloorY = chunkGenerator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR, world, noiseConfig);
                if (oceanFloorY >= y)
                    return new BlockPos(x, y, z);
            }
            if (dx == dz || dx < 0 && dx == -dz || dx > 0 && dx == 1 - dz) {
                var tmp = stepX;
                stepX = -stepZ;
                stepZ = tmp;
            }
            dx += stepX;
            dz += stepZ;
        }

        var x = startChunkPos.getMinBlockX() + 8;
        var z = startChunkPos.getMinBlockZ() + 8;
        var y = chunkGenerator.getFirstFreeHeight(x, z, Heightmap.Types.MOTION_BLOCKING, world, noiseConfig);
        return new BlockPos(x, y, z);
    }

    private BorderChangeListener getWorldBorderListener() {
        return new BorderChangeListener() {
            @Override
            public void onSetSize(@NonNull WorldBorder border, double size) {
                ItemHuntGame.this.sendToAll(new ClientboundSetBorderSizePacket(border));
            }

            @Override
            public void onLerpSize(@NonNull WorldBorder border, double fromSize, double toSize, long time, long l) {
                ItemHuntGame.this.sendToAll(new ClientboundSetBorderLerpSizePacket(border));
            }

            @Override
            public void onSetCenter(@NonNull WorldBorder border, double centerX, double centerZ) {
                ItemHuntGame.this.sendToAll(new ClientboundSetBorderCenterPacket(border));
            }

            @Override
            public void onSetWarningTime(@NonNull WorldBorder border, int warningTime) {
                ItemHuntGame.this.sendToAll(new ClientboundSetBorderWarningDelayPacket(border));
            }

            @Override
            public void onSetWarningBlocks(@NonNull WorldBorder border, int warningBlockDistance) {
                ItemHuntGame.this.sendToAll(new ClientboundSetBorderWarningDistancePacket(border));
            }

            @Override
            public void onSetDamagePerBlock(@NonNull WorldBorder border, double damagePerBlock) {}

            @Override
            public void onSetSafeZone(@NonNull WorldBorder border, double safeZoneRadius) {}
        };
    }
}
