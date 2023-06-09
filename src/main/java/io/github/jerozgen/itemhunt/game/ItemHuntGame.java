package io.github.jerozgen.itemhunt.game;

import io.github.jerozgen.itemhunt.event.StatusEffectAddEvent;
import io.github.jerozgen.itemhunt.game.phase.ItemHuntLoadingPhase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;

import java.util.function.Consumer;

public record ItemHuntGame(ItemHuntConfig config, GameSpace gameSpace, ServerWorld world, BlockPos spawnPos, GameStatisticBundle statistics) {

    public static GameOpenProcedure open(GameOpenContext<ItemHuntConfig> context) {
        var config = context.config();
        var dimensionOptions = config.dimensionOptions();
        var worldConfig = new RuntimeWorldConfig()
                .setDimensionType(dimensionOptions.dimensionTypeEntry())
                .setGenerator(dimensionOptions.chunkGenerator())
                .setSeed(Random.create().nextLong());
        var loadingWorldConfig = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setGenerator(new VoidChunkGenerator(context.server().getRegistryManager().get(RegistryKeys.BIOME)))
                .setWorldConstructor(LazyLoadingWorld::new);
        return context.open((activity) -> {
            var gameSpace = activity.getGameSpace();
            var loadingWorld = gameSpace.getWorlds().add(loadingWorldConfig);
            var world = gameSpace.getWorlds().add(worldConfig);
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
        world.getChunkManager().addTicket(ChunkTicketType.START, new ChunkPos(spawnPos), 3, Unit.INSTANCE);
    }

    public void setup(GameActivity activity) {
        activity.listen(PlayerDamageEvent.EVENT, (player, source, amount) -> ActionResult.FAIL);
        activity.listen(StatusEffectAddEvent.EVENT, this::onAddStatusEffect);

        if (!config.crafting())
            activity.deny(GameRuleType.CRAFTING);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.PORTALS);
    }

    private ActionResult onAddStatusEffect(Entity entity, StatusEffectInstance instance, @Nullable Entity source) {
        if (entity instanceof ServerPlayerEntity && instance.getEffectType().getCategory() == StatusEffectCategory.HARMFUL) {
            return ActionResult.FAIL;
        }
        return ActionResult.SUCCESS;
    }

    public void stat(Consumer<GameStatisticBundle> consumer) {
        if (statistics != null) consumer.accept(statistics);
    }

    public void sendToAll(Packet<?> packet) {
        for (var player : gameSpace.getPlayers()) {
            player.networkHandler.sendPacket(packet);
        }
    }

    private static BlockPos findSpawnPos(ServerWorld world) {
        var chunkManager = world.getChunkManager();
        var noiseConfig = chunkManager.getNoiseConfig();
        var chunkGenerator = chunkManager.getChunkGenerator();
        var startChunkPos = new ChunkPos(noiseConfig.getMultiNoiseSampler().findBestSpawnPosition());

        var dx = 0;
        var dz = 0;
        var stepX = 0;
        var stepZ = -1;
        for (var i = 0; i < 11 * 11; i++) {
            if (dx >= -5 && dx <= 5 && dz >= -5 && dz <= 5) {
                var chunkPos = new ChunkPos(startChunkPos.x + dx, startChunkPos.z + dz);
                var x = chunkPos.getStartX() + 8;
                var z = chunkPos.getStartZ() + 8;
                var y = chunkGenerator.getHeightOnGround(x, z, Heightmap.Type.MOTION_BLOCKING, world, noiseConfig);
                var oceanFloorY = chunkGenerator.getHeightOnGround(x, z, Heightmap.Type.OCEAN_FLOOR, world, noiseConfig);
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

        var x = startChunkPos.getStartX() + 8;
        var z = startChunkPos.getStartZ() + 8;
        var y = chunkGenerator.getHeightOnGround(x, z, Heightmap.Type.MOTION_BLOCKING, world, noiseConfig);
        return new BlockPos(x, y, z);
    }

    private WorldBorderListener getWorldBorderListener() {
        return new WorldBorderListener() {
            @Override
            public void onSizeChange(WorldBorder border, double size) {
                ItemHuntGame.this.sendToAll(new WorldBorderSizeChangedS2CPacket(border));
            }

            @Override
            public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
                ItemHuntGame.this.sendToAll(new WorldBorderInterpolateSizeS2CPacket(border));
            }

            @Override
            public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
                ItemHuntGame.this.sendToAll(new WorldBorderCenterChangedS2CPacket(border));
            }

            @Override
            public void onWarningTimeChanged(WorldBorder border, int warningTime) {
                ItemHuntGame.this.sendToAll(new WorldBorderWarningTimeChangedS2CPacket(border));
            }

            @Override
            public void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {
                ItemHuntGame.this.sendToAll(new WorldBorderWarningBlocksChangedS2CPacket(border));
            }

            @Override
            public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {}

            @Override
            public void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {}
        };
    }
}
