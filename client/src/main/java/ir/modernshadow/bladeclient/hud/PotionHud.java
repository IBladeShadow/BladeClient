package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PotionHud {
    private PotionHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(PotionHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.potion.enabled) return;

        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
        if (effects.isEmpty()) return;
        effects.sort(Comparator.comparing(PotionHud::effectName));

        float scale = RenderUtil.clamp(cfg.potion.scale, 0.5f, 3.0f);
        int baseX = cfg.potion.x;
        int baseY = cfg.potion.y;

        int baseLineH = Math.round(mc.textRenderer.fontHeight * scale) + 2;
        int iconSize = cfg.potion.showIcons ? Math.max(10, Math.round(12 * scale)) : 0;
        int iconGap = cfg.potion.showIcons ? Math.max(3, Math.round(4 * scale)) : 0;
        int lineStep = Math.max(baseLineH, iconSize) + 5;
        int idx = 0;

        for (StatusEffectInstance effect : effects) {
            String name = effectName(effect);
            int amp = effect.getAmplifier();
            String ampText = amp > 0 ? " " + (amp + 1) : "";
            String time = formatDuration(effect.getDuration());
            String line = name + ampText + " " + time;

            int x = baseX;
            int y = baseY + idx * lineStep;

            int textW = mc.textRenderer.getWidth(line);
            int textH = mc.textRenderer.fontHeight;
            int scaledW = RenderUtil.scale(textW, scale);
            int scaledH = RenderUtil.scale(textH, scale);

            int lineW = scaledW + (iconSize > 0 ? iconSize + iconGap : 0);
            int lineH = Math.max(scaledH, iconSize);

            if (cfg.potion.showBackground) {
                int pad = 4;
                int bg = RenderUtil.withAlpha(0x10131B, cfg.potion.backgroundOpacity);
                ctx.fill(x - pad, y - pad, x + lineW + pad, y + lineH + pad, bg);
            }

            int textX = x;
            if (iconSize > 0) {
                Identifier icon = effectIcon(effect);
                if (icon != null) {
                    int iconY = y + (lineH - iconSize) / 2;
                    ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, iconY, 0, 0, iconSize, iconSize, 18, 18);
                }
                textX += iconSize + iconGap;
            }

            int textY = y + Math.max(0, (lineH - scaledH) / 2);
            RenderUtil.drawScaledText(ctx, mc.textRenderer, RenderUtil.mcOrdered(line), textX, textY, 0xFFFFFFFF, scale);
            idx++;
        }
    }

    private static String formatDuration(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    private static String effectName(StatusEffectInstance effect) {
        return effect.getEffectType().value().getName().getString();
    }

    private static Identifier effectIcon(StatusEffectInstance effect) {
        return effect.getEffectType()
                .getKey()
                .map(key -> Identifier.ofVanilla("textures/mob_effect/" + key.getValue().getPath() + ".png"))
                .orElse(null);
    }
}
