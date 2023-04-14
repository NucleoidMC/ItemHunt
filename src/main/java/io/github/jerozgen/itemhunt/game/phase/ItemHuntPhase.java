package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import xyz.nucleoid.plasmid.game.GameActivity;

public abstract class ItemHuntPhase {
    protected final ItemHuntGame game;

    protected ItemHuntPhase(ItemHuntGame game) {
        this.game = game;
    }

    public final void setup(GameActivity activity) {
        game.setup(activity);
        setupPhase(activity);
    }

    protected abstract void setupPhase(GameActivity activity);
}
