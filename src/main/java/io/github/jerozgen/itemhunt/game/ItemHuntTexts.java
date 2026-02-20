package io.github.jerozgen.itemhunt.game;

import net.minecraft.world.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.ChatFormatting;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;

import java.util.List;
import java.util.UUID;

public final class ItemHuntTexts {
    public static final int ACCENT_COLOR = 0xaeffda;

    public static MutableComponent loading() {
        return Component.translatable("multiplayer.downloadingTerrain");
    }

    public static MutableComponent description(ItemHuntGame game) {
        var gameName = GameConfig.name(game.gameSpace().getMetadata().sourceConfig());
        return Component.empty().withStyle(ChatFormatting.GRAY)
                .append("\n").append(gameName.copy()
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(ChatFormatting.WHITE))
                .append("\n").append(Component.translatable("text.itemhunt.desc"))
                .append("\n")
                .append("\n").append(Component.translatable("options.%s.composed".formatted(game.config().crafting() ? "on" : "off"),
                        Component.translatable("container.crafting")))
                .append("\n");
    }

    public static MutableComponent itemsObtained() {
        return Component.translatable("statistic.itemhunt.items_obtained");
    }

    public static MutableComponent guiTitle(UUID winner, MinecraftServer server) {
        return Component.translatable("text.itemhunt.end.gui.title", ItemHuntUtils.getPlayerNameByUUID(winner, server));
    }

    public static MutableComponent guiPreviousPage() {
        return Component.translatable("text.itemhunt.end.gui.previous_page");
    }

    public static MutableComponent guiNextPage() {
        return Component.translatable("text.itemhunt.end.gui.next_page");
    }

    public static MutableComponent winSingleplayer(int itemsCount) {
        return Component.translatable("text.itemhunt.end.singleplayer.to_player", itemsCount).withStyle(ChatFormatting.GOLD);
    }

    public static MutableComponent win(List<UUID> winners, int itemsCount, boolean singleplayer, MinecraftServer server) {
        if (winners.size() == 1 || singleplayer) {
            var key = "text.itemhunt.end." + (singleplayer ? "singleplayer.to_spectator" : "multiplayer.winner");
            var name = ItemHuntUtils.getPlayerNameByUUID(winners.getFirst(), server);
            return Component.translatable(key, name, itemsCount).withStyle(ChatFormatting.GOLD);
        }
        var names = ComponentUtils.formatList(winners, uuid -> ItemHuntUtils.getPlayerNameByUUID(uuid, server));
        return Component.translatable("text.itemhunt.end.multiplayer.winners", names, itemsCount).withStyle(ChatFormatting.GOLD);
    }

    public static MutableComponent itemObtained(Item item) {
        return Component.translatable("text.itemhunt.item_obtained", Component.translatable(item.getDescriptionId()))
                .withStyle(s -> s.withColor(ACCENT_COLOR));
    }

    public static MutableComponent timeLeft(int seconds) {
        return Component.translatable("text.itemhunt.bossbar.time_left", time(seconds));
    }

    public static MutableComponent time(int seconds) {
        return Component.literal("%02d:%02d".formatted(seconds / 60, seconds % 60))
                .withStyle(s -> s.withColor(ACCENT_COLOR));
    }

    public static MutableComponent finishedError() {
        return Component.translatable("text.itemhunt.error.finished");
    }
}
