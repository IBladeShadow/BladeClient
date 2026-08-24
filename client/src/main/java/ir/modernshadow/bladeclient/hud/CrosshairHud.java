package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public final class CrosshairHud {
    private CrosshairHud() {}
    private static final Identifier CROSSHAIR_ATLAS = Identifier.of("bladeclient", "textures/gui/ui/crosshairs.png");
    private static final int CROSSHAIR_ATLAS_SIZE = 256;

    public static void register() {
        HudRenderCallback.EVENT.register(CrosshairHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.crosshair.enabled) return;
        if (cfg.crosshair.hideInF5 && mc.options != null && !mc.options.getPerspective().isFirstPerson()) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int cx = w / 2;
        int cy = h / 2;

        if (cfg.crosshair.style != BladeClientConfig.CrosshairStyle.CUSTOM) {
            AtlasSprite sprite = atlasSprite(cfg.crosshair.style);
            int drawSize = Math.max(10, Math.min(44, cfg.crosshair.size * 2 + 4));
            int drawW = drawSize;
            int drawH = drawSize;
            if (cfg.crosshair.style == BladeClientConfig.CrosshairStyle.WIDE_RING) {
                // Keep this style visibly wider than other presets.
                drawW = Math.round(drawSize * 2.0f);
            }
            int drawX = cx - drawW / 2;
            int drawY = cy - drawH / 2;
            int tint = hsvToRgb(cfg.crosshair.customHue, cfg.crosshair.customSaturation, cfg.crosshair.customValue);
            int color = RenderUtil.withAlpha(tint, cfg.crosshair.opacity);
            drawPresetSprite(ctx, sprite, drawX, drawY, drawW, drawH, 1.0f, color);
        } else {
            int[] rows = cfg.crosshair.customPixels;
            if (rows == null || rows.length != 16) return;
            int pixel = Math.max(1, Math.round(cfg.crosshair.size / 6.0f));
            int startX = cx - (16 * pixel) / 2;
            int startY = cy - (16 * pixel) / 2;
            int customColor = RenderUtil.withAlpha(hsvToRgb(cfg.crosshair.customHue, cfg.crosshair.customSaturation, cfg.crosshair.customValue), cfg.crosshair.opacity);
            drawCustomPixels(ctx, rows, startX, startY, pixel, customColor, 0, 16);
        }

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

    private static void drawPresetSprite(DrawContext ctx, AtlasSprite sprite, int x, int y, int drawW, int drawH, float progress, int color) {
        float p = clamp01(progress);
        if (p <= 0.0f) return;

        int partH = Math.max(1, Math.round(drawH * p));
        int drawY = y + (drawH - partH);
        float texPart = sprite.regionHeight * p;
        int regionH = Math.max(1, Math.round(texPart));
        float v = sprite.v + (sprite.regionHeight - texPart);

        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                CROSSHAIR_ATLAS,
                x,
                drawY,
                sprite.u,
                v,
                drawW,
                partH,
                sprite.regionWidth,
                regionH,
                CROSSHAIR_ATLAS_SIZE,
                CROSSHAIR_ATLAS_SIZE,
                color
        );
    }

    private static void drawCustomPixels(DrawContext ctx, int[] rows, int startX, int startY, int pixel, int color, int fromRow, int toRow) {
        int from = Math.max(0, fromRow);
        int to = Math.min(16, toRow);
        for (int y = from; y < to; y++) {
            int row = rows[y];
            for (int x = 0; x < 16; x++) {
                if ((row & (1 << x)) != 0) {
                    int px = startX + x * pixel;
                    int py = startY + y * pixel;
                    ctx.fill(px, py, px + pixel, py + pixel, color);
                }
            }
        }
    }

    private static AtlasSprite atlasSprite(BladeClientConfig.CrosshairStyle style) {
        // These UVs are from crosshairs.png and normalized so non-custom styles keep a fixed visual size.
        return switch (style) {
            case DOT -> new AtlasSprite(7.0f, 7.0f, 1, 1);              // row 0
            case PLUS -> new AtlasSprite(3.0f, 19.0f, 9, 9);            // row 1
            case PLUS_GAP -> new AtlasSprite(3.0f, 35.0f, 9, 9);        // row 2
            case SQUARE -> new AtlasSprite(4.0f, 52.0f, 7, 7);          // row 3
            case SQUARE_DOT -> new AtlasSprite(4.0f, 68.0f, 7, 7);      // row 4
            case CIRCLE -> new AtlasSprite(4.0f, 84.0f, 7, 7);          // row 5
            case CIRCLE_DOT -> new AtlasSprite(4.0f, 100.0f, 7, 7);     // row 6
            case X -> new AtlasSprite(3.0f, 115.0f, 9, 9);              // row 7
            case X_DOT -> new AtlasSprite(3.0f, 131.0f, 9, 9);          // row 8
            case CHEVRON -> new AtlasSprite(3.0f, 151.0f, 9, 5);        // row 9
            case X_STAR -> new AtlasSprite(3.0f, 163.0f, 9, 9);         // row 10
            case WIDE_RING -> new AtlasSprite(0.0f, 180.0f, 15, 7);     // row 11
            case TECH -> new AtlasSprite(2.0f, 194.0f, 11, 11);         // row 12
            case CUSTOM -> new AtlasSprite(3.0f, 19.0f, 9, 9);
        };
    }

    private static final class AtlasSprite {
        final float u;
        final float v;
        final int regionWidth;
        final int regionHeight;

        AtlasSprite(float u, float v, int regionWidth, int regionHeight) {
            this.u = u;
            this.v = v;
            this.regionWidth = regionWidth;
            this.regionHeight = regionHeight;
        }
    }
}
