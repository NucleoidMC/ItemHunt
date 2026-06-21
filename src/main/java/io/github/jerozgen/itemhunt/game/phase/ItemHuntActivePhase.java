package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.ItemHunt;
import io.github.jerozgen.itemhunt.event.InventoryChangedEvent;
import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntSidebarWidget;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import io.github.jerozgen.itemhunt.game.ObtainedItemsGui;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKeys;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ItemHuntActivePhase extends ItemHuntPhase {
    private static final String START_ITEM_NBT_KEY = "StartItem";
    private static final StatisticKey<Integer> ITEMS_OBTAINED_STAT_KEY = StatisticKey.intKey(ItemHunt.id("items_obtained"));

    private final Map<UUID, LinkedHashSet<Item>> itemsCollectedByPlayers = new HashMap<>();

    private BossBarWidget bossbar;
    private ItemHuntSidebarWidget sidebar;

    private boolean singleplayer;
    private long startTime;
    private long endTime;
    private int lastSecondsLeft = -1;

    public ItemHuntActivePhase(ItemHuntGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        var widgets = GlobalWidgets.addTo(activity);
        bossbar = widgets.addBossBar(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        sidebar = widgets.addWidget(new ItemHuntSidebarWidget(ItemHuntTexts.itemsObtained()));
        sidebar.show();

        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
        activity.listen(GamePlayerEvents.ACCEPT, this::acceptPlayer);
        activity.listen(GamePlayerEvents.ADD, this::addPlayer);
        activity.listen(GamePlayerEvents.LEAVE, this::removePlayer);

        activity.listen(InventoryChangedEvent.EVENT, this::onInventoryChanged);
    }

    private void start() {
        singleplayer = game.gameSpace().getPlayers().participants().size() == 1;

        for (var player : game.gameSpace().getPlayers().participants()) {
            player.getInventory().clearContent();
            player.inventoryMenu.getCraftSlots().clearContent();
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(player.getInventory());

            player.setGameMode(GameType.SURVIVAL);

            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, -1, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, -1, 0, false, false));

            game.config().startItems().ifPresent(stacks -> stacks.forEach(stack -> {
                var stackCopy = stack.create();
                stackCopy.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                player.getInventory().add(stackCopy);
            }));

            itemsCollectedByPlayers.put(player.getUUID(), new LinkedHashSet<>());
            sidebar.setLine(player.getScoreboardName(), 0);

            game.stat(stats -> stats.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1));
        }

        game.world().getEntities(EntityTypes.ITEM, Entity::isAlive).forEach(x -> x.kill(game.world()));
        game.world().getWorldBorder().setSize(99999);

        startTime = Util.getMillis();
        endTime = startTime + TimeUnit.SECONDS.toMillis(game.config().duration());
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return offer.teleport(game.world(), Vec3.atCenterOf(game.spawnPos())).thenRunForEach(player -> {
            player.setGameMode(GameType.SPECTATOR);
        });
    }

    private void addPlayer(ServerPlayer player) {
        player.connection.send(new ClientboundInitializeBorderPacket(game.world().getWorldBorder()));
    }

    private void removePlayer(ServerPlayer player) {
        if (!singleplayer)
            game.stat(stats -> stats.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1));
        itemsCollectedByPlayers.remove(player.getUUID());
        sidebar.removeLine(player.getScoreboardName());
        if (itemsCollectedByPlayers.isEmpty())
            game.gameSpace().close(GameCloseReason.FINISHED);
    }

    private void tick() {
        var secondsLeft = (int) TimeUnit.MILLISECONDS.toSeconds(endTime - Util.getMillis());
        if (secondsLeft != lastSecondsLeft) {
            lastSecondsLeft = secondsLeft;

            bossbar.setTitle(ItemHuntTexts.timeLeft(secondsLeft));
            bossbar.setProgress((float) (secondsLeft / (double) TimeUnit.MILLISECONDS.toSeconds((endTime - startTime))));

            if (secondsLeft <= 10) {
                if (secondsLeft > 0) for (var player : game.gameSpace().getPlayers()) {
                    player.connection.send(new ClientboundSoundEntityPacket(Holder.direct(SoundEvents.UI_BUTTON_CLICK.value()), SoundSource.PLAYERS, player, .6f, 1, player.level().getRandom().nextLong()));
                }
                else this.end();
            }
        }
    }

    private void end() {
        var maxSize = Collections.max(itemsCollectedByPlayers.values(), Comparator.comparingInt(HashSet::size)).size();
        var winners = itemsCollectedByPlayers.entrySet().stream()
                .filter(entry -> entry.getValue().size() == maxSize)
                .map(Map.Entry::getKey)
                .toList();
        if (!winners.isEmpty()) {
            var server = game.world().getServer();
            var winner = winners.getFirst();
            var winnerItems = new ArrayList<>(itemsCollectedByPlayers.get(winner));
            var guiTitleText = ItemHuntTexts.guiTitle(winner, server);
            var winText = ItemHuntTexts.win(winners, maxSize, singleplayer, server);
            for (var player : game.gameSpace().getPlayers().participants()) {
                var isWinner = player.getUUID().equals(winner);
                if (singleplayer) {
                    if (isWinner) player.sendSystemMessage(ItemHuntTexts.winSingleplayer(maxSize));
                    else player.sendSystemMessage(winText, false);
                } else {
                    if (isWinner) game.stat(stats -> stats.forPlayer(player).increment(StatisticKeys.GAMES_WON, 1));
                    else game.stat(stats -> stats.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1));
                    player.sendSystemMessage(winText, false);
                }
                new ObtainedItemsGui(player, guiTitleText, winnerItems).open();
            }
        }
        var endingPhase = new ItemHuntEndingPhase(game);
        game.gameSpace().setActivity(endingPhase::setup);
    }

    private void onInventoryChanged(ServerPlayer player, Inventory inventory, ItemStack stack) {
        if (!itemsCollectedByPlayers.containsKey(player.getUUID())) return;
        if (stack.isEmpty()) return;
        if (stack.has(DataComponents.CUSTOM_DATA)) return;

        var collectedItems = itemsCollectedByPlayers.get(player.getUUID());
        var item = stack.getItem();
        if (collectedItems.add(item)) {
            game.stat(stats -> stats.forPlayer(player).increment(ITEMS_OBTAINED_STAT_KEY, 1));
            sidebar.setLine(player.getScoreboardName(), collectedItems.size());
            player.connection.send(new ClientboundBundlePacket(List.of(
                    new ClientboundSetTitlesAnimationPacket(0, 20, 10),
                    new ClientboundSetTitleTextPacket(Component.nullToEmpty("")),
                    new ClientboundSetSubtitleTextPacket(ItemHuntTexts.itemObtained(item)),
                    new ClientboundSoundEntityPacket(Holder.direct(SoundEvents.NOTE_BLOCK_BELL.value()), SoundSource.PLAYERS, player, 1, 1, player.level().getRandom().nextLong()))));
        }
    }
}
