package io.github.jerozgen.itemhunt.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class ItemHuntUtils {
    public static String getEncodedSkinTexture(String skinUrlHash) {
        var value = """
                {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/%s"}}}""".formatted(skinUrlHash);
        return new String(Base64.getEncoder().encode(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public static Component getPlayerNameByUUID(UUID uuid, MinecraftServer server) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getName();

        var profile = server.services().nameToIdCache().get(uuid);
        return Component.nullToEmpty(profile.map(NameAndId::name).orElseGet(uuid::toString));
    }
}
