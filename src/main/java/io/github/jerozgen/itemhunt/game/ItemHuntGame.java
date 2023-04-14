package io.github.jerozgen.itemhunt.game;

import io.github.jerozgen.itemhunt.event.StatusEffectAddEvent;
import io.github.jerozgen.itemhunt.game.phase.ItemHuntWaitingPhase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.SpawnLocating;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
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
        var dimensionOptions = context.server().getRegistryManager().get(RegistryKeys.DIMENSION).get(config.dimensionOptionsKey());
        if (dimensionOptions == null)
            throw new NullPointerException("Couldn't find %s dimension options from %s game config"
                    .formatted(config.dimensionOptionsKey().getValue(), context.game().source()));
        var worldConfig = new RuntimeWorldConfig()
                .setDimensionType(dimensionOptions.dimensionTypeEntry())
                .setGenerator(dimensionOptions.chunkGenerator())
                .setSeed(context.server().getOverworld().getRandom().nextLong());
        return context.openWithWorld(worldConfig, (activity, world) -> {
            var gameSpace = activity.getGameSpace();
            var statistics = config.statisticBundleNamespace()
                    .map(value -> gameSpace.getStatistics().bundle(value))
                    .orElse(null);
            var game = new ItemHuntGame(config, gameSpace, world, findSpawnPos(world), statistics);
            activity.listen(GameActivityEvents.CREATE, () -> {
                var waitingPhase = new ItemHuntWaitingPhase(game);
                game.gameSpace.setActivity(waitingPhase::setup);
            });
        });
    }

    public ItemHuntGame {
        world.getWorldBorder().addListener(getWorldBorderListener());
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
        var spawnPos = chunkManager.getNoiseConfig().getMultiNoiseSampler().findBestSpawnPosition();
        var chunkPos = new ChunkPos(spawnPos);
        var x = chunkPos.getStartX() + 8;
        var z = chunkPos.getStartZ() + 8;
        int y = chunkManager.getChunkGenerator().getSpawnHeight(world);
        if (y < world.getBottomY())
            y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        spawnPos = new BlockPos(x, y, z);

        var dx = 0;
        var dz = 0;
        var stepX = 0;
        var stepZ = -1;
        for (var i = 0; i < MathHelper.square(11); i++) {
            if (dx >= -5 && dx <= 5 && dz >= -5 && dz <= 5) {
                var pos = SpawnLocating.findServerSpawnPoint(world, new ChunkPos(chunkPos.x + dx, chunkPos.z + dz));
                if (pos != null) {
                    spawnPos = pos;
                    break;
                }
            }
            if (dx == dz || dx < 0 && dx == -dz || dx > 0 && dx == 1 - dz) {
                var tmp = stepX;
                stepX = -stepZ;
                stepZ = tmp;
            }
            dx += stepX;
            dz += stepZ;
        }

        return spawnPos;
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
