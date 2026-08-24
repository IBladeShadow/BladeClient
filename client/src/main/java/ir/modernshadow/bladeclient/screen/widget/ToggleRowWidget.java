package ir.modernshadow.bladeclient.screen.widget;

import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.module.setting.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ToggleRowWidget extends ButtonWidget {
    private static final float HOVER_SPEED = 1.0f;
    private static final int TOGGLE_W = 40;
    private static final int TOGGLE_H = 14;
    private static final int INDICATOR_W = 22;
    private static final int INDICATOR_H = 8;
    private static final int ARROW_W = 14;

    private final BooleanSetting setting;
    private final boolean showArrow;
    private final java.util.function.BooleanSupplier expanded;
    private final Runnable toggleExpanded;
    private float hoverProgress = 0f;

    public ToggleRowWidget(int x, int y, int width, int height, BooleanSetting setting) {
        this(x, y, width, height, setting, null, null);
    }

    public ToggleRowWidget(int x, int y, int width, int height, BooleanSetting setting,
                           java.util.function.BooleanSupplier expanded, Runnable toggleExpanded) {
        super(x, y, width, height, Text.literal(setting.name()), button -> setting.toggle(), DEFAULT_NARRATION_SUPPLIER);
        this.setting = setting;
        this.showArrow = expanded != null && toggleExpanded != null;
        this.expanded = expanded;
        this.toggleExpanded = toggleExpanded;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        boolean hovered = isMouseOver(mouseX, mouseY);
        if (hovered) {
            hoverProgress = Math.min(1f, hoverProgress + delta * HOVER_SPEED);
        } else {
            hoverProgress = Math.max(0f, hoverProgress - delta * HOVER_SPEED);
        }

        boolean on = setting.get();
        int toggleX = x + 8;
        int toggleY = y + (h - TOGGLE_H) / 2;
        int border = blend(0xFF666B73, 0xFF8B919B, hoverProgress * 0.55f);
        int shell = blend(0xFF292D35, 0xFF333844, hoverProgress * 0.35f);
        if (!this.active) {
            border = 0xFF4E535C;
            shell = 0xFF23262D;
        }
        drawRoundedRectAA(context, toggleX, toggleY, TOGGLE_W, TOGGLE_H, 3, border);
        drawRoundedRectAA(context, toggleX + 1, toggleY + 1, TOGGLE_W - 2, TOGGLE_H - 2, 2, shell);

        String pillText = on ? "ON" : "OFF";
        int indicatorX = on ? toggleX + 3 : toggleX + TOGGLE_W - INDICATOR_W - 3;
        int indicatorY = toggleY + (TOGGLE_H - INDICATOR_H) / 2;
        int indicatorColor = on ? 0xFF31BF76 : 0xFFC55252;
        if (!this.active) {
            indicatorColor = on ? 0xFF2A7C53 : 0xFF7A4040;
        }
        drawRoundedRectAA(context, indicatorX, indicatorY, INDICATOR_W, INDICATOR_H, 2, indicatorColor);
        context.fill(indicatorX + 1, indicatorY + 1, indicatorX + INDICATOR_W - 1, indicatorY + 2, 0x33FFFFFF);
        BladeFonts.drawUiCentered(context, pillText, indicatorX + INDICATOR_W / 2.0f, indicatorY + INDICATOR_H / 2.0f,
                0xFFFFFFFF, 10.0f, true);

        int arrowX = -1;
        if (showArrow) {
            arrowX = toggleX + TOGGLE_W + 4;
            int arrowY = y + (int) ((h - BladeFonts.UI_SMALL) / 2.0f) + 1;
            String arrow = expanded.getAsBoolean() ? "v" : ">";
            BladeFonts.drawUi(context, arrow, arrowX, arrowY, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        }

        int labelX = toggleX + TOGGLE_W + (showArrow ? 18 : 10);
        int textCol = this.active ? 0xFFB6BDC8 : 0xFF7A808A;
        BladeFonts.drawUi(context, setting.name(), labelX, y + (h - BladeFonts.UI_SIZE) / 2.0f + 1,
                textCol, BladeFonts.UI_SIZE, true);
    }

    private boolean isMouseOver(int mx, int my) {
        return mx >= this.getX() && my >= this.getY() && mx < this.getX() + this.getWidth() && my < this.getY() + this.getHeight();
    }

    private static void drawRoundedRectAA(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        int radius = Math.max(0, Math.min(r, Math.min(w / 2, h / 2)));
        if (radius == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }

        ctx.fill(x + radius, y, x + w - radius, y + h, color);
        ctx.fill(x, y + radius, x + radius, y + h - radius, color);
        ctx.fill(x + w - radius, y + radius, x + w, y + h - radius, color);

        drawQuarterCircleAA(ctx, x + radius, y + radius, radius, color, -1, -1);
        drawQuarterCircleAA(ctx, x + w - radius - 1, y + radius, radius, color, 1, -1);
        drawQuarterCircleAA(ctx, x + radius, y + h - radius - 1, radius, color, -1, 1);
        drawQuarterCircleAA(ctx, x + w - radius - 1, y + h - radius - 1, radius, color, 1, 1);
    }

    private static void drawQuarterCircleAA(DrawContext ctx, int cx, int cy, int r, int color, int sx, int sy) {
        int aBase = (color >>> 24) & 0xFF;
        int rC = (color >>> 16) & 0xFF;
        int gC = (color >>> 8) & 0xFF;
        int bC = color & 0xFF;
        float rr = r + 0.5f;
        int min = -r - 1;
        int max = r + 1;
        for (int dy = min; dy <= max; dy++) {
            if ((sy < 0 && dy > 0) || (sy > 0 && dy < 0)) {
                continue;
            }
            for (int dx = min; dx <= max; dx++) {
                if ((sx < 0 && dx > 0) || (sx > 0 && dx < 0)) {
                    continue;
                }
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float edge = rr - dist;
                if (edge <= 0.0f) continue;
                float alpha = edge >= 1.0f ? aBase : (aBase * edge);
                int a = Math.min(255, Math.max(0, Math.round(alpha)));
                if (a == 0) continue;
                int c = (a << 24) | (rC << 16) | (gC << 8) | bC;
                ctx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, c);
            }
        }
    }

    private static int blend(int from, int to, float t) {
        float v = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = (from >>> 24) & 0xFF;
        int r1 = (from >>> 16) & 0xFF;
        int g1 = (from >>> 8) & 0xFF;
        int b1 = from & 0xFF;
        int a2 = (to >>> 24) & 0xFF;
        int r2 = (to >>> 16) & 0xFF;
        int g2 = (to >>> 8) & 0xFF;
        int b2 = to & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * v);
        int r = Math.round(r1 + (r2 - r1) * v);
        int g = Math.round(g1 + (g2 - g1) * v);
        int b = Math.round(b1 + (b2 - b1) * v);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (showArrow) {
            int toggleX = this.getX() + 8;
            int arrowX = toggleX + TOGGLE_W + 2;
            if (mouseX >= arrowX && mouseX < arrowX + ARROW_W) {
                toggleExpanded.run();
                return;
            }
        }
        super.onClick(mouseX, mouseY);
    }
}
