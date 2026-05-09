package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;

import java.util.Set;

public class ItemHuntLoadingPhase extends ItemHuntPhase {
    private final ServerLevel loadingWorld;
    private final long spawnChunkPos;

    public ItemHuntLoadingPhase(ItemHuntGame game, ServerLevel loadingWorld) {
        super(game);
        this.loadingWorld = loadingWorld;
        this.spawnChunkPos = ChunkPos.containing(game.spawnPos()).pack();
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        var widgets = GlobalWidgets.addTo(activity);
        var bossbar = widgets.addBossBar(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        bossbar.setTitle(ItemHuntTexts.loading());
        game.world().getChunkSource().addTicket(new Ticket(TicketType.FORCED, 2), ChunkPos.containing(game.spawnPos()));

        activity.listen(GamePlayerEvents.ACCEPT, this::acceptPlayer);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GameActivityEvents.DESTROY, this::destroy);
    }

    private void tick() {
        if (game.world().areEntitiesLoaded(spawnChunkPos)) {
            for (var player : game.gameSpace().getPlayers().participants()) {
                var pos = game.spawnPos().getCenter();
                player.teleportTo(game.world(), pos.x(), game.spawnPos().getY(), pos.z(), Set.of(), 0, 0, false);
                player.setGameMode(GameType.ADVENTURE);
            }
            var activePhase = new ItemHuntWaitingPhase(game);
            game.gameSpace().setActivity(activePhase::setup);
        }
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return offer.teleport(loadingWorld, game.spawnPos().getCenter()).thenRunForEach(player -> {
            player.sendSystemMessage(ItemHuntTexts.description(game), false);
            player.setGameMode(GameType.SPECTATOR);
        });
    }

    private void destroy(GameCloseReason reason) {
        game.world().getChunkSource().removeTicketWithRadius(TicketType.FORCED, ChunkPos.containing(game.spawnPos()), 3);
    }
}
