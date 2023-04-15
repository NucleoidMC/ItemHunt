package io.github.jerozgen.itemhunt.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.dimension.DimensionOptions;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;

import java.util.List;
import java.util.Optional;

public record ItemHuntConfig(PlayerConfig playerConfig, RegistryKey<DimensionOptions> dimensionOptionsKey, int duration,
                             Optional<String> statisticBundleNamespace, boolean crafting, Optional<List<ItemStack>> startItems) {
    public static final Codec<ItemHuntConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(ItemHuntConfig::playerConfig),
            RegistryKey.createCodec(RegistryKeys.DIMENSION).optionalFieldOf("dimension_options", DimensionOptions.OVERWORLD).forGetter(ItemHuntConfig::dimensionOptionsKey),
            Codecs.POSITIVE_INT.optionalFieldOf("duration", 180).forGetter(ItemHuntConfig::duration),
            GameStatisticBundle.NAMESPACE_CODEC.optionalFieldOf("statistic_bundle").forGetter(ItemHuntConfig::statisticBundleNamespace),
            Codec.BOOL.optionalFieldOf("crafting", false).forGetter(ItemHuntConfig::crafting),
            MoreCodecs.ITEM_STACK.listOf().optionalFieldOf("start_items").forGetter(ItemHuntConfig::startItems)
    ).apply(instance, ItemHuntConfig::new));
}
