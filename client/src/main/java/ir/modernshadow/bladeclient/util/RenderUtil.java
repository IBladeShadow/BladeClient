package ir.modernshadow.bladeclient.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class RenderUtil {
    private RenderUtil() {}

    private static final Identifier FONT_MC = Identifier.of("minecraft", "default");
    private static final Style MC_STYLE = Style.EMPTY.withFont(FONT_MC);

    public static int scale(int value, float scale) {
        return Math.round(value * scale);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int withAlpha(int rgb, float opacity) {
        int a = Math.round(clamp(opacity, 0f, 1f) * 255f);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    public static void drawScaledText(DrawContext ctx, TextRenderer renderer, OrderedText text,
                                      int x, int y, int color, float scale) {
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.scale(scale, scale);
        int drawX = Math.round(x / scale);
        int drawY = Math.round(y / scale);
        ctx.drawTextWithShadow(renderer, text, drawX, drawY, color);
        matrices.popMatrix();
    }

    public static Text mcText(String text) {
        return Text.literal(text).setStyle(MC_STYLE);
    }

    public static OrderedText mcOrdered(String text) {
        return mcText(text).asOrderedText();
    }

    public static boolean shouldRenderHud(net.minecraft.client.MinecraftClient mc) {
        if (mc == null) return false;
        if (mc.options != null && mc.options.hudHidden) return false;
        return mc.getDebugHud() == null || !mc.getDebugHud().shouldShowDebugHud();
    }
}
