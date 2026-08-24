package ir.modernshadow.bladeclient.screen.widget;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class CrosshairEditorWidget extends ClickableWidget {
    private static final int GRID = 16;
    private static final int PADDING = 8;
    private static final int BORDER = 1;

    private final BladeClientConfig.Crosshair crosshair;
    private boolean dragging = false;
    private int dragButton = -1;

    public CrosshairEditorWidget(int x, int y, int width, int height, BladeClientConfig.Crosshair crosshair) {
        super(x, y, width, height, Text.literal("Crosshair Editor"));
        this.crosshair = crosshair;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // Match the darker module-settings panel style (no blue accent line)
        ctx.fill(x, y, x + w, y + h, 0x66101010);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x551A1D24);

        int cell = Math.max(1, Math.min((w - PADDING * 2) / GRID, (h - PADDING * 2) / GRID));
        int gridSize = cell * GRID;
        int gridX = x + (w - gridSize) / 2;
        int gridY = y + (h - gridSize) / 2;

        int color = withOpacity(hsvToRgb(crosshair.customHue, crosshair.customSaturation, crosshair.customValue), crosshair.opacity);
        int empty = 0xFF0B0F18;
        int border = 0xFF1F2430;

        for (int row = 0; row < GRID; row++) {
            int mask = rowMask(row);
            for (int col = 0; col < GRID; col++) {
                int cx = gridX + col * cell;
                int cy = gridY + row * cell;
                int fill = ((mask & (1 << col)) != 0) ? color : empty;
                ctx.fill(cx, cy, cx + cell, cy + cell, fill);
                ctx.fill(cx, cy, cx + cell, cy + BORDER, border);
                ctx.fill(cx, cy + cell - BORDER, cx + cell, cy + cell, border);
                ctx.fill(cx, cy, cx + BORDER, cy + cell, border);
                ctx.fill(cx + cell - BORDER, cy, cx + cell, cy + cell, border);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;
        if (applyPaint(mouseX, mouseY, button)) {
            dragging = true;
            dragButton = button;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && (button == dragButton)) {
            applyPaint(mouseX, mouseY, button);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == dragButton) {
            dragging = false;
            dragButton = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean applyPaint(double mouseX, double mouseY, int button) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int cell = Math.max(1, Math.min((w - PADDING * 2) / GRID, (h - PADDING * 2) / GRID));
        int gridSize = cell * GRID;
        int gridX = x + (w - gridSize) / 2;
        int gridY = y + (h - gridSize) / 2;

        if (mouseX < gridX || mouseX >= gridX + gridSize || mouseY < gridY || mouseY >= gridY + gridSize) {
            return false;
        }

        int col = (int) ((mouseX - gridX) / cell);
        int row = (int) ((mouseY - gridY) / cell);
        if (col < 0 || col >= GRID || row < 0 || row >= GRID) return false;

        boolean set = button == 0;
        setPixel(row, col, set);
        ConfigManager.saveQuiet();
        return true;
    }

    private int rowMask(int row) {
        if (crosshair.customPixels == null || crosshair.customPixels.length != GRID) {
            crosshair.customPixels = new int[GRID];
        }
        return crosshair.customPixels[row];
    }

    private void setPixel(int row, int col, boolean on) {
        if (crosshair.customPixels == null || crosshair.customPixels.length != GRID) {
            crosshair.customPixels = new int[GRID];
        }
        int mask = 1 << col;
        if (on) {
            crosshair.customPixels[row] |= mask;
        } else {
            crosshair.customPixels[row] &= ~mask;
        }
    }

    private static int withOpacity(int rgb, float opacity) {
        int a = Math.round(Math.max(0.0f, Math.min(1.0f, opacity)) * 255.0f);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int hsvToRgb(int hue, int saturation, int value) {
        float h = ((hue % 360) + 360) % 360;
        float s = clamp01(saturation / 100.0f);
        float v = clamp01(value / 100.0f);
        float c = v * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;
        float r1, g1, b1;
        if (h < 60) { r1 = c; g1 = x; b1 = 0; }
        else if (h < 120) { r1 = x; g1 = c; b1 = 0; }
        else if (h < 180) { r1 = 0; g1 = c; b1 = x; }
        else if (h < 240) { r1 = 0; g1 = x; b1 = c; }
        else if (h < 300) { r1 = x; g1 = 0; b1 = c; }
        else { r1 = c; g1 = 0; b1 = x; }
        int r = Math.round((r1 + m) * 255.0f);
        int g = Math.round((g1 + m) * 255.0f);
        int b = Math.round((b1 + m) * 255.0f);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
