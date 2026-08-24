package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class DirectionHud {
    private DirectionHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(DirectionHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.direction.enabled) return;

        String facing = mc.player.getHorizontalFacing().asString().toUpperCase();
        float yaw = mc.player.getYaw();
        if (yaw < 0) yaw += 360f;

        String text = cfg.direction.showAngle
                ? ("Facing: " + facing + " (" + Math.round(yaw) + " deg)")
                : ("Facing: " + facing);

        float scale = RenderUtil.clamp(cfg.direction.scale, 0.5f, 3.0f);
        int x = cfg.direction.x;
        int y = cfg.direction.y;

        int textW = mc.textRenderer.getWidth(text);
        int textH = mc.textRenderer.fontHeight;
        int scaledW = RenderUtil.scale(textW, scale);
        int scaledH = RenderUtil.scale(textH, scale);

        if (cfg.direction.showBackground) {
            int pad = 6;
            int bg = RenderUtil.withAlpha(0x10131B, cfg.direction.backgroundOpacity);
            ctx.fill(x - pad, y - pad, x + scaledW + pad, y + scaledH + pad, bg);
        }
        RenderUtil.drawScaledText(ctx, mc.textRenderer, RenderUtil.mcOrdered(text), x, y, 0xFFFFFFFF, scale);
    }
}
