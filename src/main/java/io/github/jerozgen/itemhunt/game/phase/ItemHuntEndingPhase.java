package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.BossBarWidget;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

import java.util.concurrent.TimeUnit;

public class ItemHuntEndingPhase extends ItemHuntPhase {
    private BossBarWidget bossbar;
    private long endTime;
    private long endDuration;
    private int lastSecondsLeft = -1;

    public ItemHuntEndingPhase(ItemHuntGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        var widgets = GlobalWidgets.addTo(activity);
        bossbar = widgets.addBossBar(Text.empty(), BossBar.Color.BLUE, BossBar.Style.PROGRESS);

        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
    }

    private void start() {
        endDuration = TimeUnit.SECONDS.toMillis(game.config().endDuration());
        endTime = Util.getMeasuringTimeMs() + endDuration;
    }

    private void tick() {
        var millisLeft = endTime - Util.getMeasuringTimeMs();
        var secondsLeft = (int) TimeUnit.MILLISECONDS.toSeconds(millisLeft);
        if (secondsLeft != lastSecondsLeft) {
            lastSecondsLeft = secondsLeft;
            bossbar.setTitle(ItemHuntTexts.time(secondsLeft));
        }
        bossbar.setProgress((float) (millisLeft / (double) endDuration));
        if (millisLeft <= 0)
            game.gameSpace().close(GameCloseReason.FINISHED);
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return offer.reject(ItemHuntTexts.finishedError());
    }
}
