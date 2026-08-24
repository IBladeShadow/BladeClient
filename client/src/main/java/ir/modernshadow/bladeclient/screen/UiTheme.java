package ir.modernshadow.bladeclient.screen;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public final class UiTheme {
    private UiTheme() {}

    public static final Identifier FONT_UI = Identifier.of("minecraft", "default");
    public static final Identifier FONT_TITLE = Identifier.of("minecraft", "default");

    public static final Identifier BACKGROUND = Identifier.of("bladeclient", "textures/gui/background.png");
    public static final int BG_TEX_W = 1024;
    public static final int BG_TEX_H = 512;

    public static final int OVERLAY = 0x70000000;
    public static final int PANEL_BG = 0x9910131B;
    public static final int PANEL_BORDER = 0xFF2A2F3A;
    public static final int ACCENT = 0xFF3A6DFF;

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFFB6BDC8;

    public static void drawBackground(DrawContext ctx, int width, int height) {
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA;
        float scale = Math.max(width / (float) BG_TEX_W, height / (float) BG_TEX_H);
        int drawW = Math.round(BG_TEX_W * scale);
        int drawH = Math.round(BG_TEX_H * scale);
        int drawX = (width - drawW) / 2;
        int drawY = (height - drawH) / 2;

        Matrix3x2fStack matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.scale(scale, scale);
        int scaledX = Math.round(drawX / scale);
        int scaledY = Math.round(drawY / scale);
        ctx.drawTexture(pipeline, BACKGROUND, scaledX, scaledY, 0.0F, 0.0F, BG_TEX_W, BG_TEX_H, BG_TEX_W, BG_TEX_H);
        matrices.popMatrix();
        ctx.fill(0, 0, width, height, OVERLAY);
    }

    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, PANEL_BG);
        ctx.fill(x, y, x + w, y + 2, ACCENT);
        ctx.fill(x, y, x + 2, y + h, ACCENT);
        ctx.fill(x + w - 1, y, x + w, y + h, PANEL_BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, PANEL_BORDER);
    }
}
