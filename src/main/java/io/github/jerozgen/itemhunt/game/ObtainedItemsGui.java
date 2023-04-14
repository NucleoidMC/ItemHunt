package io.github.jerozgen.itemhunt.game;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

import static io.github.jerozgen.itemhunt.game.ItemHuntUtils.getEncodedSkinTexture;

public class ObtainedItemsGui extends SimpleGui {
    private static final String PREVIOUS_PAGE_TEXTURE = getEncodedSkinTexture("50820f76e3e041c75f76d0f301232bdf48321b534fe6a859ccb873d2981a9623");
    private static final String NEXT_PAGE_TEXTURE = getEncodedSkinTexture("7e57720a4878c8bcab0e9c9c47d9e55128ccd77ba3445a54a91e3e1e1a27356e");

    private final GuiElementBuilder previousPageButtonBuilder;
    private final GuiElementBuilder nextPageButtonBuilder;

    private final List<Item> items;
    private int page = 0;
    private final int maxPage;

    public ObtainedItemsGui(ServerPlayerEntity player, Text title, List<Item> items) {
        super(ScreenHandlerType.GENERIC_9X6, player, false);
        this.setTitle(title);
        this.items = items;
        this.maxPage = (items.size() - 1) / (9 * 5);
        this.previousPageButtonBuilder = new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(ItemHuntTexts.guiPreviousPage())
                .hideFlags()
                .setSkullOwner(PREVIOUS_PAGE_TEXTURE)
                .setCallback(() -> {
                    if (page > 0) {
                        page -= 1;
                        refresh();
                    }
                });
        this.nextPageButtonBuilder = new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(ItemHuntTexts.guiNextPage())
                .hideFlags()
                .setSkullOwner(NEXT_PAGE_TEXTURE)
                .setCallback(() -> {
                    if (page < maxPage) {
                        page += 1;
                        refresh();
                    }
                });
        this.refresh();
    }

    private void refresh() {
        var startIndex = page * 9 * 5;
        var endIndex = (page + 1) * 9 * 5 - 1;
        for (int i = startIndex; i <= endIndex; i++) {
            if (i < items.size()) this.setSlot(i - startIndex, items.get(i).getDefaultStack());
            else this.clearSlot(i - startIndex);
        }

        if (page < maxPage) this.setSlot(9 * 5 + 7, nextPageButtonBuilder);
        else this.clearSlot(9 * 5 + 7);

        if (page > 0) this.setSlot(9 * 5 + 1, previousPageButtonBuilder);
        else this.clearSlot(9 * 5 + 1);
    }
}
