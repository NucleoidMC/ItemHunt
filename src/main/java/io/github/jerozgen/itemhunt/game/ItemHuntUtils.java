package io.github.jerozgen.itemhunt.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class ItemHuntUtils {
    public static String getEncodedSkinTexture(String skinUrlHash) {
        var value = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/%s\"}}}".formatted(skinUrlHash);
        return new String(Base64.getEncoder().encode(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public static Text getPlayerNameByUUID(UUID uuid, MinecraftServer server) {
        var player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) return player.getDisplayName();

        var profile = server.getUserCache().getByUuid(uuid);
        return Text.of(profile.isEmpty() ? uuid.toString() : profile.get().getName());
    }
}
