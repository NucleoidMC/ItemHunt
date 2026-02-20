package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;

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
        bossbar = widgets.addBossBar(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GamePlayerEvents.OFFER, offer -> offer.reject(ItemHuntTexts.finishedError()));
    }

    private void start() {
        endDuration = TimeUnit.SECONDS.toMillis(game.config().endDuration());
        endTime = Util.getMillis() + endDuration;
    }

    private void tick() {
        var millisLeft = endTime - Util.getMillis();
        var secondsLeft = (int) TimeUnit.MILLISECONDS.toSeconds(millisLeft);
        if (secondsLeft != lastSecondsLeft) {
            lastSecondsLeft = secondsLeft;
            bossbar.setTitle(ItemHuntTexts.time(secondsLeft));
        }
        bossbar.setProgress((float) (millisLeft / (double) endDuration));
        if (millisLeft <= 0)
            game.gameSpace().close(GameCloseReason.FINISHED);
    }
}
