package io.github.jerozgen.itemhunt;

import io.github.jerozgen.itemhunt.game.ItemHuntConfig;
import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class ItemHunt implements ModInitializer {
    public static final String ID = "itemhunt";

    public static Identifier id(String path) {
        return new Identifier(ID, path);
    }

    @Override
    public void onInitialize() {
        GameType.register(id(ID), ItemHuntConfig.CODEC, ItemHuntGame::open);
    }
}
