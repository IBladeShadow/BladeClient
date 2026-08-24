package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.mixin.HungerManagerAccessor;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.HungerManager;

import java.util.ArrayList;
import java.util.List;

public final class AppleSkinHud {
    private AppleSkinHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(AppleSkinHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.appleSkin.enabled) return;

        // Render AppleSkin-style saturation overlay near the vanilla food bar.
        if (cfg.appleSkin.showSaturation) {
            int right = ctx.getScaledWindowWidth() / 2 + 91;
            int top = ctx.getScaledWindowHeight() - 39;
            AppleSkinOverlay.render(ctx, mc.player, right, top);
        }

        if (!cfg.appleSkin.showLabel) return;

        HungerManager hunger = mc.player.getHungerManager();
        int food = hunger.getFoodLevel();
        float saturation = hunger.getSaturationLevel();
        float exhaustion = ((HungerManagerAccessor) hunger).bladeclient$getExhaustion();

        List<String> lines = new ArrayList<>();
        lines.add("Hunger: " + food + "/20");
        if (cfg.appleSkin.showSaturation) {
            lines.add(String.format("Saturation: %.1f", saturation));
        }
        if (cfg.appleSkin.showExhaustion) {
            lines.add(String.format("Exhaustion: %.2f", exhaustion));
        }

        if (lines.isEmpty()) return;

        float scale = RenderUtil.clamp(cfg.appleSkin.scale, 0.5f, 3.0f);
        int x = cfg.appleSkin.x;
        int y = cfg.appleSkin.y;

        int textH = mc.textRenderer.fontHeight;
        int maxW = 0;
        for (String line : lines) {
            maxW = Math.max(maxW, mc.textRenderer.getWidth(line));
        }
        int scaledW = RenderUtil.scale(maxW, scale);
        int scaledH = RenderUtil.scale(textH * lines.size(), scale);

        if (cfg.appleSkin.showBackground) {
            int pad = 6;
            int bg = RenderUtil.withAlpha(0x10131B, cfg.appleSkin.backgroundOpacity);
            ctx.fill(x - pad, y - pad, x + scaledW + pad, y + scaledH + pad, bg);
        }

        int lineY = y;
        for (String line : lines) {
            RenderUtil.drawScaledText(ctx, mc.textRenderer, RenderUtil.mcOrdered(line), x, lineY, 0xFFFFFFFF, scale);
            lineY += RenderUtil.scale(textH, scale);
        }
    }
}
