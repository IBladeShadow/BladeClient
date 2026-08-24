package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class PingHud {
    private PingHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(PingHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.ping.enabled) return;

        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }

        String text = (cfg.ping.showLabel ? "Ping: " : "") + ping + " ms";
        float scale = RenderUtil.clamp(cfg.ping.scale, 0.5f, 3.0f);
        int x = cfg.ping.x;
        int y = cfg.ping.y;

        int textW = mc.textRenderer.getWidth(text);
        int textH = mc.textRenderer.fontHeight;
        int scaledW = RenderUtil.scale(textW, scale);
        int scaledH = RenderUtil.scale(textH, scale);

        if (cfg.ping.showBackground) {
            int pad = 6;
            int bg = RenderUtil.withAlpha(0x10131B, cfg.ping.backgroundOpacity);
            ctx.fill(x - pad, y - pad, x + scaledW + pad, y + scaledH + pad, bg);
        }

        int color = cfg.ping.colorByLatency ? colorForPing(ping) : cfg.ping.color;
        RenderUtil.drawScaledText(ctx, mc.textRenderer, RenderUtil.mcOrdered(text), x, y, color, scale);
    }

    private static int colorForPing(int ping) {
        if (ping >= 300) return 0xFFCC4444;
        if (ping >= 200) return 0xFFFF6666;
        if (ping >= 100) return 0xFFFFFF66;
        return 0xFFFFFFFF;
    }
}
