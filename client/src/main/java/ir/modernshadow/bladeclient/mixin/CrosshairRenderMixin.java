package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.config.ConfigManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public class CrosshairRenderMixin {
    private static final Identifier VANILLA_CROSSHAIR = Identifier.ofVanilla("hud/crosshair");
    private static final Identifier VANILLA_ATTACK_FULL = Identifier.ofVanilla("hud/crosshair_attack_indicator_full");
    private static final Identifier VANILLA_ATTACK_BG = Identifier.ofVanilla("hud/crosshair_attack_indicator_background");
    private static final Identifier VANILLA_ATTACK_PROGRESS = Identifier.ofVanilla("hud/crosshair_attack_indicator_progress");

    @Redirect(
            method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
            ),
            require = 0
    )
    private void bladeclient$hideVanillaCrosshair(DrawContext ctx, RenderPipeline pipeline, Identifier id, int x, int y, int w, int h) {
        if (ConfigManager.get().crosshair.enabled) {
            if (VANILLA_CROSSHAIR.equals(id)) {
                return;
            }
            if (isAttackIndicatorTexture(id)) {
                ctx.drawGuiTexture(pipeline, id, x, y, w, h, attackIndicatorTint());
                return;
            }
        }
        ctx.drawGuiTexture(pipeline, id, x, y, w, h);
    }

    @Redirect(
            method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V"
            ),
            require = 0
    )
    private void bladeclient$hideVanillaCrosshair5(DrawContext ctx, RenderPipeline pipeline, Identifier id,
                                                    int x, int y, int w, int h, int color) {
        if (ConfigManager.get().crosshair.enabled) {
            if (VANILLA_CROSSHAIR.equals(id)) {
                return;
            }
            if (isAttackIndicatorTexture(id)) {
                ctx.drawGuiTexture(pipeline, id, x, y, w, h, attackIndicatorTint());
                return;
            }
        }
        ctx.drawGuiTexture(pipeline, id, x, y, w, h, color);
    }

    @Redirect(
            method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIIIIII)V"
            ),
            require = 0
    )
    private void bladeclient$hideVanillaCrosshair8(DrawContext ctx, RenderPipeline pipeline, Identifier id,
                                                    int a, int b, int c, int d, int e, int f, int g, int h) {
        if (ConfigManager.get().crosshair.enabled) {
            if (VANILLA_CROSSHAIR.equals(id)) {
                return;
            }
            if (isAttackIndicatorTexture(id)) {
                // 8-int overload has no tint parameter; switch to 9-int color overload.
                ctx.drawGuiTexture(pipeline, id, a, b, c, d, e, f, g, h, attackIndicatorTint());
                return;
            }
        }
        ctx.drawGuiTexture(pipeline, id, a, b, c, d, e, f, g, h);
    }

    @Redirect(
            method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIIIIIII)V"
            ),
            require = 0
    )
    private void bladeclient$hideVanillaCrosshair9(DrawContext ctx, RenderPipeline pipeline, Identifier id,
                                                    int a, int b, int c, int d, int e, int f, int g, int h, int i) {
        if (ConfigManager.get().crosshair.enabled) {
            if (VANILLA_CROSSHAIR.equals(id)) {
                return;
            }
            if (isAttackIndicatorTexture(id)) {
                ctx.drawGuiTexture(pipeline, id, a, b, c, d, e, f, g, h, attackIndicatorTint());
                return;
            }
        }
        ctx.drawGuiTexture(pipeline, id, a, b, c, d, e, f, g, h, i);
    }

    private static boolean isAttackIndicatorTexture(Identifier id) {
        return VANILLA_ATTACK_FULL.equals(id)
                || VANILLA_ATTACK_BG.equals(id)
                || VANILLA_ATTACK_PROGRESS.equals(id);
    }

    private static int attackIndicatorTint() {
        var crosshair = ConfigManager.get().crosshair;
        int rgb = hsvToRgb(crosshair.customHue, crosshair.customSaturation, crosshair.customValue);
        // Vanilla attack-indicator textures already contain transparency,
        // so we boost user opacity a bit to match perceived crosshair opacity.
        float op = effectiveIndicatorOpacity(crosshair.opacity);
        int alpha = Math.max(0, Math.min(255, Math.round(op * 255.0f)));
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static float effectiveIndicatorOpacity(float opacity) {
        float o = clamp01(opacity);
        return 1.0f - (float) Math.pow(1.0f - o, 1.4);
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
}
