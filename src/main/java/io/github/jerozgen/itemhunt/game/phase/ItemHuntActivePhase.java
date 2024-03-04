package io.github.jerozgen.itemhunt.game.phase;

import io.github.jerozgen.itemhunt.ItemHunt;
import io.github.jerozgen.itemhunt.event.InventoryChangedEvent;
import io.github.jerozgen.itemhunt.game.ItemHuntGame;
import io.github.jerozgen.itemhunt.game.ItemHuntSidebarWidget;
import io.github.jerozgen.itemhunt.game.ItemHuntTexts;
import io.github.jerozgen.itemhunt.game.ObtainedItemsGui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.BossBarWidget;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.stats.StatisticKey;
import xyz.nucleoid.plasmid.game.stats.StatisticKeys;

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
        bossbar = widgets.addBossBar(Text.empty(), BossBar.Color.BLUE, BossBar.Style.PROGRESS);
        sidebar = widgets.addWidget(new ItemHuntSidebarWidget(ItemHuntTexts.itemsObtained()));
        sidebar.show();

        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
        activity.listen(GamePlayerEvents.ADD, this::addPlayer);
        activity.listen(GamePlayerEvents.LEAVE, this::removePlayer);

        activity.listen(InventoryChangedEvent.EVENT, this::onInventoryChanged);
    }

    private void start() {
        singleplayer = game.gameSpace().getPlayers().size() == 1;

        for (var player : game.gameSpace().getPlayers()) {
            player.getInventory().clear();
            player.playerScreenHandler.getCraftingInput().clear();
            player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
            player.currentScreenHandler.sendContentUpdates();
            player.playerScreenHandler.onContentChanged(player.getInventory());

            player.changeGameMode(GameMode.SURVIVAL);

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, -1, 0, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, -1, 0, false, false));

            game.config().startItems().ifPresent(stacks -> stacks.forEach(stack -> {
                var stackCopy = stack.copy();
                stackCopy.getOrCreateNbt().putBoolean(START_ITEM_NBT_KEY, true);
                player.getInventory().insertStack(stackCopy);
            }));

            itemsCollectedByPlayers.put(player.getUuid(), new LinkedHashSet<>());
            sidebar.setLine(player.getNameForScoreboard(), 0);

            game.stat(stats -> stats.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1));
        }

        game.world().getEntitiesByType(EntityType.ITEM, Entity::isAlive).forEach(Entity::kill);
        game.world().getWorldBorder().setSize(99999);

        startTime = Util.getMeasuringTimeMs();
        endTime = startTime + TimeUnit.SECONDS.toMillis(game.config().duration());
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return offer.accept(game.world(), game.spawnPos().toCenterPos()).and(() -> {
            offer.player().changeGameMode(GameMode.SPECTATOR);
        });
    }

    private void addPlayer(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(game.world().getWorldBorder()));
    }

    private void removePlayer(ServerPlayerEntity player) {
        if (!singleplayer)
            game.stat(stats -> stats.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1));
        itemsCollectedByPlayers.remove(player.getUuid());
        sidebar.removeLine(player.getNameForScoreboard());
        if (itemsCollectedByPlayers.isEmpty())
            game.gameSpace().close(GameCloseReason.FINISHED);
    }

    private void tick() {
        var secondsLeft = (int) TimeUnit.MILLISECONDS.toSeconds(endTime - Util.getMeasuringTimeMs());
        if (secondsLeft != lastSecondsLeft) {
            lastSecondsLeft = secondsLeft;

            bossbar.setTitle(ItemHuntTexts.timeLeft(secondsLeft));
            bossbar.setProgress((float) (secondsLeft / (double) TimeUnit.MILLISECONDS.toSeconds((endTime - startTime))));

            if (secondsLeft <= 10) {
                if (secondsLeft > 0) for (var player : game.gameSpace().getPlayers()) {
                    player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, .6f, 1);
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
            var winner = winners.get(0);
            var winnerItems = new ArrayList<>(itemsCollectedByPlayers.get(winner));
            var guiTitleText = ItemHuntTexts.guiTitle(winner, server);
            var winText = ItemHuntTexts.win(winners, maxSize, singleplayer, server);
            for (var player : game.gameSpace().getPlayers()) {
                var isWinner = player.getUuid().equals(winner);
                if (singleplayer) {
                    if (isWinner) player.sendMessage(ItemHuntTexts.winSingleplayer(maxSize));
                    else player.sendMessage(winText, false);
                } else {
                    if (isWinner) game.stat(stats -> stats.forPlayer(player).increment(StatisticKeys.GAMES_WON, 1));
                    else game.stat(stats -> stats.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1));
                    player.sendMessage(winText, false);
                }
                new ObtainedItemsGui(player, guiTitleText, winnerItems).open();
            }
        }
        var endingPhase = new ItemHuntEndingPhase(game);
        game.gameSpace().setActivity(endingPhase::setup);
    }

    private void onInventoryChanged(ServerPlayerEntity player, PlayerInventory inventory, ItemStack stack) {
        if (!itemsCollectedByPlayers.containsKey(player.getUuid())) return;
        if (stack.isEmpty()) return;
        if (stack.getOrCreateNbt().getBoolean(START_ITEM_NBT_KEY)) return;

        var collectedItems = itemsCollectedByPlayers.get(player.getUuid());
        var item = stack.getItem();
        if (collectedItems.add(item)) {
            game.stat(stats -> stats.forPlayer(player).increment(ITEMS_OBTAINED_STAT_KEY, 1));
            sidebar.setLine(player.getNameForScoreboard(), collectedItems.size());
            player.networkHandler.sendPacket(new BundleS2CPacket(List.of(
                    new TitleFadeS2CPacket(0, 20, 10),
                    new TitleS2CPacket(Text.of("")),
                    new SubtitleS2CPacket(ItemHuntTexts.itemObtained(item)))));
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1, 1);
        }
    }
}
