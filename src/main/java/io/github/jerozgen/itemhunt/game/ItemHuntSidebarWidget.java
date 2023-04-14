package io.github.jerozgen.itemhunt.game;

import eu.pb4.sidebars.api.Sidebar;
import eu.pb4.sidebars.api.lines.AbstractSidebarLine;
import eu.pb4.sidebars.api.lines.LineBuilder;
import eu.pb4.sidebars.api.lines.SidebarLine;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.widget.GameWidget;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ItemHuntSidebarWidget extends Sidebar implements GameWidget {

    public ItemHuntSidebarWidget(Text title) {
        super(title, Sidebar.Priority.MEDIUM);
    }

    public void setLine(String name, int value) {
        for (var line : elements) {
            if (!(line instanceof NamedLine namedLine)) throw new UnsupportedOperationException();
            if (namedLine.getName().equals(name)) {
                elements.set(elements.indexOf(line), new NamedLine(name, value));
                return;
            }
        }
        elements.add(new NamedLine(name, value));
        this.markDirty();
    }

    public void removeLine(String name) {
        for (var line : new ArrayList<>(elements)) {
            if (!(line instanceof NamedLine namedLine)) throw new UnsupportedOperationException();
            if (namedLine.getName().equals(name)) {
                elements.remove(line);
                line.setSidebar(null);
            }
        }
    }

    @Override
    public void close() {
        this.hide();
        players.clear();
    }

    @Override
    public void setLine(int value, Text text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLine(SidebarLine line) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLines(SidebarLine... lines) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLines(Text... texts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLine(SidebarLine line) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLine(int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable SidebarLine getLine(int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceLines(Text... texts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceLines(SidebarLine... lines) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceLines(LineBuilder builder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(Consumer<LineBuilder> consumer) {
        throw new UnsupportedOperationException();
    }

    public static class NamedLine extends AbstractSidebarLine {
        protected String name;

        public NamedLine(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        @Override
        public Text getText() {
            return Text.of(name);
        }

        @Override
        public Text getText(ServerPlayNetworkHandler handler) {
            if (handler.player.getEntityName().equals(name))
                return Text.empty().append(super.getText(handler)).styled(s -> s.withColor(ItemHuntTexts.ACCENT_COLOR));
            return super.getText(handler);
        }
    }
}