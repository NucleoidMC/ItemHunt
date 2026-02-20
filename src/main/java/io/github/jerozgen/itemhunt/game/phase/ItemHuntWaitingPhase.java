package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;

public class ItemHuntWaitingPhase extends ItemHuntPhase {

    public ItemHuntWaitingPhase(ItemHuntGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        GameWaitingLobby.addTo(activity, game.config().playerConfig());

        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GamePlayerEvents.ACCEPT, this::acceptPlayers);
        activity.listen(GamePlayerEvents.ADD, this::addPlayer);
        activity.listen(GameActivityEvents.REQUEST_START, this::requestStart);
    }

    private void start() {
        var pos = game.spawnPos().getCenter();
        var worldBorder = game.world().getWorldBorder();
        worldBorder.setCenter(pos.x(), pos.z());
        worldBorder.setSize(9);
        worldBorder.setWarningBlocks(-100);
    }

    private JoinAcceptorResult acceptPlayers(JoinAcceptor offer) {
        return offer.teleport(game.world(), game.spawnPos().getCenter()).thenRunForEach((player, intent) -> {
            player.setGameMode(intent == JoinIntent.SPECTATE ? GameType.SPECTATOR : GameType.ADVENTURE);
            player.displayClientMessage(ItemHuntTexts.description(game), false);
        });
    }

    private void addPlayer(ServerPlayer player) {
        player.connection.send(new ClientboundInitializeBorderPacket(game.world().getWorldBorder()));
    }

    private GameResult requestStart() {
        var activePhase = new ItemHuntActivePhase(game);
        game.gameSpace().setActivity(activePhase::setup);
        return GameResult.ok();
    }
}
