package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

public class ItemHuntEndingPhase extends ItemHuntPhase {
    public static final long ENDING_DURATION = 30 * 1000;

    private long endTime;

    public ItemHuntEndingPhase(ItemHuntGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
    }

    private void start() {
        endTime = Util.getMeasuringTimeMs() + ENDING_DURATION;
    }

    private void tick() {
        if (Util.getMeasuringTimeMs() >= endTime)
            game.gameSpace().close(GameCloseReason.FINISHED);
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return offer.reject(ItemHuntTexts.finishedError());
    }
}
