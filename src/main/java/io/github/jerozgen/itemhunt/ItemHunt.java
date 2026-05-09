package io.github.jerozgen.itemhunt;

import io.github.jerozgen.itemhunt.game.ItemHuntConfig;
import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class ItemHunt implements ModInitializer {
    public static final String ID = "itemhunt";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    @Override
    public void onInitialize() {
        GameTypes.register(id(ID), ItemHuntConfig.CODEC, ItemHuntGame::open);
    }
}
