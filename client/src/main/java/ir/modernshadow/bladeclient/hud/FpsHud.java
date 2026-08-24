package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class FpsHud {
    private FpsHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(FpsHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.fps.enabled) return;

        int fps = mc.getCurrentFps();
        String text = (cfg.fps.showLabel ? "FPS: " : "") + fps;

        float scale = RenderUtil.clamp(cfg.fps.scale, 0.5f, 3.0f);
        int x = cfg.fps.x;
        int y = cfg.fps.y;

        int textW = mc.textRenderer.getWidth(text);
        int textH = mc.textRenderer.fontHeight;
        int scaledW = RenderUtil.scale(textW, scale);
        int scaledH = RenderUtil.scale(textH, scale);

        if (cfg.fps.showBackground) {
            int pad = 6;
            int bg = RenderUtil.withAlpha(0x10131B, cfg.fps.backgroundOpacity);
            ctx.fill(x - pad, y - pad, x + scaledW + pad, y + scaledH + pad, bg);
        }

        int color = cfg.fps.colorByFps ? colorForFps(fps) : 0xFFFFFFFF;
        RenderUtil.drawScaledText(ctx, mc.textRenderer, RenderUtil.mcOrdered(text), x, y, color, scale);
    }

    private static int colorForFps(int fps) {
        if (fps <= 30) return 0xFFFF5555;
        if (fps <= 60) {
            float t = (fps - 30f) / 30f;
            return interpolate(0xFFFF5555, 0xFFFFFF55, t);
        }
        if (fps <= 120) return 0xFFFFFF55;
        if (fps <= 240) {
            float t = (fps - 120f) / 120f;
            return interpolate(0xFFFFFF55, 0xFFFFFFFF, t);
        }
        return 0xFFFFFFFF;
    }

    private static int interpolate(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
