package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

public class ItemHuntWaitingPhase extends ItemHuntPhase {
    private final ServerWorld waitingWorld;
    private boolean hasMoved = false;
    private long spawnChunk;
    private ItemHuntActivePhase delayedPhase;

    public ItemHuntWaitingPhase(ItemHuntGame game, ServerWorld waitingWorld) {
        super(game);
        this.waitingWorld = waitingWorld;
        this.spawnChunk = new ChunkPos(game.spawnPos()).toLong();
        System.out.println(this.game.spawnPos());
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        GameWaitingLobby.addTo(activity, game.config().playerConfig());

        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
        activity.listen(GamePlayerEvents.ADD, this::addPlayer);
        activity.listen(GameActivityEvents.REQUEST_START, this::requestStart);
        activity.listen(GameActivityEvents.TICK, this::tick);
    }

    private void tick() {
        if (this.hasMoved) {
            return;
        }

        if (this.isMainWorldReady()) {
            for (var x : this.game.gameSpace().getPlayers()) {
                x.teleport(this.game.world(), this.game.spawnPos().getX(), this.game.spawnPos().getY(), this.game.spawnPos().getZ(), 0, 0);
                x.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(game.world().getWorldBorder()));

            }
            this.hasMoved = true;

            if (this.delayedPhase != null) {
                game.gameSpace().setActivity(this.delayedPhase::setup);
            }
        }
    }

    private void start() {
        var pos = game.spawnPos().toCenterPos();
        var worldBorder = game.world().getWorldBorder();
        worldBorder.setCenter(pos.getX(), pos.getZ());
        worldBorder.setSize(9);
        worldBorder.setWarningBlocks(-100);
    }

    private boolean isMainWorldReady() {
        return this.game.world().isChunkLoaded(this.spawnChunk);
        //return this.game.world().getChunkManager().getLoadedChunkCount() >= 36;
    }

    private ServerWorld getActiveWorld() {
        return this.isMainWorldReady() ? this.game.world() : this.waitingWorld;
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return offer.accept(this.getActiveWorld(), game.spawnPos().toCenterPos()).and(() -> {
            offer.player().sendMessage(ItemHuntTexts.description(game), false);
            offer.player().changeGameMode(GameMode.ADVENTURE);
        });
    }

    private void addPlayer(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(game.world().getWorldBorder()));
    }

    private GameResult requestStart() {
        var activePhase = new ItemHuntActivePhase(game);
        if (this.hasMoved) {
            game.gameSpace().setActivity(activePhase::setup);
        } else {
            this.delayedPhase = activePhase;
        }

        return GameResult.ok();
    }
}
