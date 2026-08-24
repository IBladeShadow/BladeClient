package ir.modernshadow.bladeclient.ui;

import ir.modernshadow.bladeclient.font.BladeFonts;
import net.minecraft.client.gui.DrawContext;

public final class BladeUI {

    private BladeUI() {}

    // -------- BUTTON --------

    public static void button(DrawContext ctx, String text,
                              float x, float y,
                              float width, float height,
                              boolean hovered,
                              int baseColor, int hoverColor) {

        int bg = hovered ? hoverColor : baseColor;

        // background
        ctx.fill((int)x, (int)y, (int)(x + width), (int)(y + height), bg);

        // text center
        float tx = x + width / 2f;
        float ty = y + height / 2f - 4;

        BladeFonts.drawUiCentered(
                ctx,
                text,
                tx,
                ty,
                0xFFFFFF,
                11f,
                true
        );
    }

    // -------- PANEL --------

    public static void panel(DrawContext ctx,
                             float x, float y,
                             float w, float h,
                             int color) {
        ctx.fill((int)x, (int)y, (int)(x + w), (int)(y + h), color);
    }

    // -------- SIMPLE CHECK --------

    public static boolean isHovered(double mx, double my,
                                    float x, float y,
                                    float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}