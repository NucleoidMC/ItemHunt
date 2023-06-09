package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

public class ItemHuntLoadingPhase extends ItemHuntPhase {
    private final ServerWorld loadingWorld;
    private final long spawnChunkPos;

    public ItemHuntLoadingPhase(ItemHuntGame game, ServerWorld loadingWorld) {
        super(game);
        this.loadingWorld = loadingWorld;
        this.spawnChunkPos = new ChunkPos(game.spawnPos()).toLong();
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        var widgets = GlobalWidgets.addTo(activity);
        var bossbar = widgets.addBossBar(Text.empty(), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
        bossbar.setTitle(ItemHuntTexts.loading());

        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
        activity.listen(GameActivityEvents.TICK, this::tick);
    }

    private void tick() {
        if (game.world().isChunkLoaded(spawnChunkPos)) {
            for (var player : game.gameSpace().getPlayers()) {
                var pos = game.spawnPos().toCenterPos();
                player.teleport(game.world(), pos.getX(), game.spawnPos().getY(), pos.getZ(), 0, 0);
                player.changeGameMode(GameMode.ADVENTURE);
            }
            var activePhase = new ItemHuntWaitingPhase(game);
            game.gameSpace().setActivity(activePhase::setup);
        }
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return offer.accept(loadingWorld, game.spawnPos().toCenterPos()).and(() -> {
            offer.player().sendMessage(ItemHuntTexts.description(game), false);
            offer.player().changeGameMode(GameMode.SPECTATOR);
        });
    }
}
