package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.mixin.EntryListWidgetAccessor;
import ir.modernshadow.bladeclient.mixin.PackScreenAccessor;
import ir.modernshadow.bladeclient.mixin.ResourcePackEntryAccessor;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;

public final class PackDragState {
    private static DragState drag;
    private static final double DRAG_THRESHOLD = 6.0;

    private PackDragState() {
    }

    public static void handlePress(PackScreen screen, double mouseX, double mouseY) {
        drag = null;
        PackScreenAccessor accessor = (PackScreenAccessor) (Object) screen;
        PackListWidget available = accessor.bladeclient$getAvailablePackList();
        PackListWidget selected = accessor.bladeclient$getSelectedPackList();

        EntryHit hit = findEntry(available, mouseX, mouseY);
        boolean fromEnabled = false;
        if (hit == null) {
            hit = findEntry(selected, mouseX, mouseY);
            fromEnabled = true;
        }
        if (hit == null) {
            return;
        }

        PackListWidget list = fromEnabled ? selected : available;
        PackListWidget.ResourcePackEntry entry = hit.entry();
        ResourcePackOrganizer.Pack pack = ((ResourcePackEntryAccessor) (Object) entry).bladeclient$getPack();
        if (pack == null) {
            return;
        }

        int entryTop = list.getRowTop(hit.index());
        int entryBottom = list.getRowBottom(hit.index());
        int entryHeight = entryBottom - entryTop;
        int entryWidth = list.getRowWidth();
        int entryLeft = list.getRowLeft();
        double offsetX = mouseX - entryLeft;
        double offsetY = mouseY - entryTop;
        drag = new DragState(
                screen,
                entry,
                pack,
                fromEnabled,
                pack.isEnabled(),
                entryWidth,
                entryHeight,
                mouseX,
                mouseY,
                offsetX,
                offsetY,
                false
        );
    }

    public static void handleRelease(PackScreen screen, double mouseX, double mouseY) {
        if (drag == null) {
            return;
        }
        if (drag.screen != screen) {
            drag = null;
            return;
        }
        if (!drag.active) {
            drag = null;
            return;
        }
        if (drag.pack.isEnabled() != drag.wasEnabled) {
            drag = null;
            return;
        }

        PackScreenAccessor accessor = (PackScreenAccessor) (Object) screen;
        PackListWidget available = accessor.bladeclient$getAvailablePackList();
        PackListWidget selected = accessor.bladeclient$getSelectedPackList();

        boolean overAvailable = isOverList(available, mouseX, mouseY);
        boolean overSelected = isOverList(selected, mouseX, mouseY);
        boolean changed = false;

        if (drag.fromEnabled && overAvailable) {
            drag.entry.toggle();
            changed = true;
        } else if (!drag.fromEnabled && overSelected) {
            drag.entry.toggle();
            changed = true;
        }

        if (changed) {
            accessor.bladeclient$updatePackLists();
        }
        drag = null;
    }

    public static void clear() {
        drag = null;
    }

    public static void updateDrag(PackScreen screen, double mouseX, double mouseY) {
        if (drag == null || drag.screen != screen || drag.active) {
            return;
        }
        double dx = mouseX - drag.pressX;
        double dy = mouseY - drag.pressY;
        if (dx * dx + dy * dy >= DRAG_THRESHOLD * DRAG_THRESHOLD) {
            drag = drag.withActive(true);
        }
    }

    public static DragInfo getDragInfo(PackScreen screen) {
        DragState state = drag;
        if (state == null || state.screen != screen || !state.active) {
            return null;
        }
        return new DragInfo(state.pack, state.entryWidth, state.entryHeight, state.offsetX, state.offsetY);
    }

    public static boolean isDraggingEntry(Object entry) {
        DragState state = drag;
        return state != null && state.active && state.entry == entry;
    }

    private static EntryHit findEntry(PackListWidget list, double mouseX, double mouseY) {
        EntryListWidgetAccessor accessor = (EntryListWidgetAccessor) (Object) list;
        int left = list.getRowLeft();
        int right = list.getRowRight();
        if (mouseX < left || mouseX > right) {
            return null;
        }

        var children = accessor.bladeclient$getChildren();
        for (int i = 0; i < children.size(); i++) {
            int top = list.getRowTop(i);
            int bottom = list.getRowBottom(i);
            if (mouseY >= top && mouseY <= bottom) {
                Object entry = children.get(i);
                if (entry instanceof PackListWidget.ResourcePackEntry packEntry) {
                    return new EntryHit(packEntry, i);
                }
                return null;
            }
        }
        return null;
    }

    private static boolean isOverList(PackListWidget list, double mouseX, double mouseY) {
        int x = list.getX();
        int y = list.getY();
        int w = list.getWidth();
        int h = list.getHeight();
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public record DragInfo(ResourcePackOrganizer.Pack pack, int width, int height, double offsetX, double offsetY) {
    }

    private record EntryHit(PackListWidget.ResourcePackEntry entry, int index) {
    }

    private record DragState(
            PackScreen screen,
            PackListWidget.ResourcePackEntry entry,
            ResourcePackOrganizer.Pack pack,
            boolean fromEnabled,
            boolean wasEnabled,
            int entryWidth,
            int entryHeight,
            double pressX,
            double pressY,
            double offsetX,
            double offsetY,
            boolean active
    ) {
        DragState withActive(boolean nextActive) {
            if (active == nextActive) return this;
            return new DragState(screen, entry, pack, fromEnabled, wasEnabled, entryWidth, entryHeight, pressX, pressY, offsetX, offsetY, nextActive);
        }
    }
}
