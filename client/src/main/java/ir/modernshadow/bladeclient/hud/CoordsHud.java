package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class CoordsHud {
    private CoordsHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(CoordsHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.coords.enabled) return;

        int x = cfg.coords.x;
        int y = cfg.coords.y;
        float scale = RenderUtil.clamp(cfg.coords.scale, 0.5f, 3.0f);

        int px = (int) Math.floor(mc.player.getX());
        int py = (int) Math.floor(mc.player.getY());
        int pz = (int) Math.floor(mc.player.getZ());

        String line = "XYZ: " + px + ", " + py + ", " + pz;
        drawLine(ctx, mc, line, x, y, scale, cfg.coords.showBackground, cfg.coords.backgroundOpacity);

        if (cfg.coords.showBiome && mc.world != null) {
            String biome = mc.world.getBiome(mc.player.getBlockPos())
                    .getKey()
                    .map(key -> key.getValue().getPath())
                    .orElse("unknown");
            drawLine(ctx, mc, "Biome: " + biome, x, y + RenderUtil.scale(mc.textRenderer.fontHeight + 4, scale),
                    scale, cfg.coords.showBackground, cfg.coords.backgroundOpacity);
        }
    }

    private static void drawLine(DrawContext ctx, MinecraftClient mc, String text, int x, int y, float scale,
                                 boolean showBackground, float backgroundOpacity) {
        int textW = mc.textRenderer.getWidth(text);
        int textH = mc.textRenderer.fontHeight;
        int scaledW = RenderUtil.scale(textW, scale);
        int scaledH = RenderUtil.scale(textH, scale);
        if (showBackground) {
            int pad = 6;
            int bg = RenderUtil.withAlpha(0x10131B, backgroundOpacity);
            ctx.fill(x - pad, y - pad, x + scaledW + pad, y + scaledH + pad, bg);
        }
        RenderUtil.drawScaledText(ctx, mc.textRenderer, RenderUtil.mcOrdered(text), x, y, 0xFFFFFFFF, scale);
    }
}
