package io.github.jerozgen.itemhunt.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.dimension.LevelStem;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;

import java.util.List;
import java.util.Optional;

public record ItemHuntConfig(WaitingLobbyConfig playerConfig, LevelStem dimensionOptions, int duration, int endDuration,
                             Optional<String> statisticBundleNamespace, boolean crafting, Optional<List<ItemStack>> startItems) {
    public static final MapCodec<ItemHuntConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(ItemHuntConfig::playerConfig),
            LevelStem.CODEC.fieldOf("dimension_options").forGetter(ItemHuntConfig::dimensionOptions),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("duration", 180).forGetter(ItemHuntConfig::duration),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("end_duration", 15).forGetter(ItemHuntConfig::endDuration),
            GameStatisticBundle.NAMESPACE_CODEC.optionalFieldOf("statistic_bundle").forGetter(ItemHuntConfig::statisticBundleNamespace),
            Codec.BOOL.optionalFieldOf("crafting", false).forGetter(ItemHuntConfig::crafting),
            MoreCodecs.ITEM_STACK.listOf().optionalFieldOf("start_items").forGetter(ItemHuntConfig::startItems)
    ).apply(instance, ItemHuntConfig::new));
}
