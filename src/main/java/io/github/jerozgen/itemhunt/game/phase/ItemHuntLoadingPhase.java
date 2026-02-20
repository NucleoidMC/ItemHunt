package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;

import java.util.Set;

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
        game.world().getChunkManager().addTicket(new ChunkTicket(ChunkTicketType.FORCED, 2), new ChunkPos(game.spawnPos()));

        activity.listen(GamePlayerEvents.ACCEPT, this::acceptPlayer);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GameActivityEvents.DESTROY, this::destroy);
    }

    private void tick() {
        if (game.world().isChunkLoaded(spawnChunkPos)) {
            for (var player : game.gameSpace().getPlayers().participants()) {
                var pos = game.spawnPos().toCenterPos();
                player.teleport(game.world(), pos.getX(), game.spawnPos().getY(), pos.getZ(), Set.of(), 0, 0, false);
                player.changeGameMode(GameMode.ADVENTURE);
            }
            var activePhase = new ItemHuntWaitingPhase(game);
            game.gameSpace().setActivity(activePhase::setup);
        }
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return offer.teleport(loadingWorld, game.spawnPos().toCenterPos()).thenRunForEach(player -> {
            player.sendMessage(ItemHuntTexts.description(game), false);
            player.changeGameMode(GameMode.SPECTATOR);
        });
    }

    private void destroy(GameCloseReason reason) {
        game.world().getChunkManager().removeTicket(ChunkTicketType.FORCED, new ChunkPos(game.spawnPos()), 3);
    }
}
