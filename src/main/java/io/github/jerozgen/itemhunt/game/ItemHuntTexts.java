package io.github.jerozgen.itemhunt.game;

import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

public final class ItemHuntTexts {
    public static final int ACCENT_COLOR = 0xaeffda;

    public static MutableText description(ItemHuntGame game) {
        var gameName = game.gameSpace().getMetadata().sourceConfig().name();
        return Text.empty().formatted(Formatting.GRAY)
                .append("\n").append(gameName.copy()
                        .formatted(Formatting.BOLD)
                        .formatted(Formatting.WHITE))
                .append("\n").append(Text.translatable("text.itemhunt.desc"))
                .append("\n")
                .append("\n").append(Text.translatable("options.%s.composed".formatted(game.config().crafting() ? "on" : "off"),
                        Text.translatable("container.crafting")))
                .append("\n");
    }

    public static MutableText itemsObtained() {
        return Text.translatable("statistic.itemhunt.items_obtained");
    }

    public static MutableText guiTitle(UUID winner, MinecraftServer server) {
        return Text.translatable("text.itemhunt.end.gui.title", ItemHuntUtils.getPlayerNameByUUID(winner, server));
    }

    public static MutableText guiPreviousPage() {
        return Text.translatable("text.itemhunt.end.gui.previous_page");
    }

    public static MutableText guiNextPage() {
        return Text.translatable("text.itemhunt.end.gui.next_page");
    }

    public static MutableText winSingleplayer(int itemsCount) {
        return Text.translatable("text.itemhunt.end.singleplayer.to_player", itemsCount).formatted(Formatting.GOLD);
    }

    public static MutableText win(List<UUID> winners, int itemsCount, boolean singleplayer, MinecraftServer server) {
        if (winners.size() == 1 || singleplayer) {
            var key = "text.itemhunt.end." + (singleplayer ? "singleplayer.to_spectator" : "multiplayer.single");
            var name = ItemHuntUtils.getPlayerNameByUUID(winners.get(0), server);
            return Text.translatable(key, name, itemsCount).formatted(Formatting.GOLD);
        }
        var names = Texts.join(winners, uuid -> ItemHuntUtils.getPlayerNameByUUID(uuid, server));
        return Text.translatable("text.itemhunt.end.multiplayer.multiple", names, itemsCount).formatted(Formatting.GOLD);
    }

    public static MutableText itemObtained(Item item) {
        return Text.translatable("text.itemhunt.item_obtained", Text.translatable(item.getTranslationKey()))
                .styled(s -> s.withColor(ACCENT_COLOR));
    }

    public static MutableText timeLeft(int seconds) {
        var timeText = Text.literal("%02d:%02d".formatted(seconds / 60, seconds % 60))
                .styled(s -> s.withColor(ACCENT_COLOR));
        return Text.translatable("text.itemhunt.bossbar.time_left", timeText);
    }

    public static MutableText finishedError() {
        return Text.translatable("text.itemhunt.error.finished");
    }
}
