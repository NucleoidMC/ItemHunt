package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

public class ItemHuntWaitingPhase extends ItemHuntPhase {
    public ItemHuntWaitingPhase(ItemHuntGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        GameWaitingLobby.addTo(activity, game.config().playerConfig());

        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
        activity.listen(GamePlayerEvents.ADD, this::addPlayer);
        activity.listen(GameActivityEvents.REQUEST_START, this::requestStart);
    }

    private void start() {
        var pos = game.spawnPos().toCenterPos();
        var worldBorder = game.world().getWorldBorder();
        worldBorder.setCenter(pos.getX(), pos.getZ());
        worldBorder.setSize(9);
        worldBorder.setWarningBlocks(-100);
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return offer.accept(game.world(), game.spawnPos().toCenterPos()).and(() -> {
            offer.player().sendMessage(ItemHuntTexts.description(game), false);
            offer.player().changeGameMode(GameMode.ADVENTURE);
        });
    }

    private void addPlayer(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(game.world().getWorldBorder()));
    }

    private GameResult requestStart() {
        var activePhase = new ItemHuntActivePhase(game);
        game.gameSpace().setActivity(activePhase::setup);
        return GameResult.ok();
    }
}
