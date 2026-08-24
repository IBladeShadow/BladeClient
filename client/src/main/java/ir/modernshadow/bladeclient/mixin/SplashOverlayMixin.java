package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.screen.BladeLogoMask;
import ir.modernshadow.bladeclient.screen.UiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    private static final int ICON_TEX_W = 512;
    private static final int ICON_TEX_H = 512;
    private static float smoothedProgress = 0.0f;
    private static long lastProgressNs = 0L;
    private static long completionStartMs = -1L;
    private static final long COMPLETION_HOLD_MS = 120L;
    private static final long COMPLETION_FADE_MS = 900L;
    private static long animationStartNs = System.nanoTime();

    @Shadow private MinecraftClient client;
    @Shadow private float progress;

    @Inject(method = "render", at = @At("TAIL"))
    private void bladeclient$renderOverlay(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        // Fully opaque custom loading to hide vanilla Mojang splash completely.
        float raw = Math.max(0f, Math.min(1f, this.progress));
        long now = System.currentTimeMillis();
        if (raw >= 0.999f) {
            if (completionStartMs < 0L) {
                completionStartMs = now;
            }
        } else {
            completionStartMs = -1L;
        }

        float overlayAlpha = 1.0f;
        if (completionStartMs >= 0L) {
            long elapsed = now - completionStartMs;
            float t = 0.0f;
            if (elapsed > COMPLETION_HOLD_MS) {
                t = (elapsed - COMPLETION_HOLD_MS) / (float) COMPLETION_FADE_MS;
            }
            t = Math.max(0.0f, Math.min(1.0f, t));
            // smoothstep
            float s = t * t * (3.0f - 2.0f * t);
            overlayAlpha = 1.0f - s;
        }

        ctx.fill(0, 0, sw, sh, withAlpha(0xFF0B0F18, overlayAlpha));
        ctx.fill(0, 0, sw, sh, withAlpha(0x4D0C1630, overlayAlpha));

        int centerX = sw / 2;
        int centerY = sh / 2;

        int iconSize = Math.max(48, Math.min(96, Math.round(Math.min(sw, sh) * 0.12f)));
        int iconX = centerX - iconSize / 2;
        int barW = Math.round(sw * 0.42f);
        int barH = 12;
        int barX = centerX - barW / 2;
        int barY = centerY + 30;
        int titleY = barY - 42;
        int statusY = barY - 20;
        int iconY = titleY - iconSize - 10;
        if (iconY < 16) {
            iconY = 16;
            titleY = iconY + iconSize + 10;
            statusY = titleY + 18;
            barY = statusY + 16;
        }

        Identifier iconId = BladeLogoMask.iconId(this.client);
        float t = (System.nanoTime() - animationStartNs) / 1_000_000_000.0f;
        float pulse = 1.0f + 0.08f * (float) Math.sin(t * 3.2f);
        float bob = 2.8f * (float) Math.sin(t * 2.4f);
        float tilt = 0.04f * (float) Math.sin(t * 1.6f);
        float glow = 0.55f + 0.45f * (float) Math.sin(t * 2.0f + 1.0f);
        int glowPad = 8;
        int glowColor = withAlpha(0xFF4AA3FF, overlayAlpha * (0.07f + 0.06f * glow));
        drawRoundedRect(ctx, iconX - glowPad, iconY - glowPad, iconSize + glowPad * 2, iconSize + glowPad * 2, 12, glowColor);
        drawIcon(ctx, iconId, iconX, iconY, iconSize, iconSize, overlayAlpha, pulse, bob, tilt);

        String title = "BladeClient";
        int titleW = BladeFonts.titleWidth(title, BladeFonts.TITLE_SIZE);
        BladeFonts.drawTitle(ctx, title, centerX - titleW / 2.0f, titleY, withAlpha(UiTheme.TEXT_PRIMARY, overlayAlpha), BladeFonts.TITLE_SIZE, true);

        float p = smoothProgress(raw);
        drawRoundedBar(ctx, barX, barY, barW, barH, p, overlayAlpha);
        int percent = Math.round(p * 100f);
        String subtitle = "Loading resources " + percent + "%";
        int subW = BladeFonts.uiWidth(subtitle, BladeFonts.UI_SMALL);
        BladeFonts.drawUi(ctx, subtitle, centerX - subW / 2.0f, statusY, withAlpha(0xFFB4BED2, overlayAlpha), BladeFonts.UI_SMALL, true);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V"
            )
    )
    private void bladeclient$delayOverlayDismiss(MinecraftClient client, Overlay overlay) {
        // Preserve any non-null overlay assignment behavior.
        if (overlay != null) {
            client.setOverlay(overlay);
            return;
        }

        long now = System.currentTimeMillis();
        if (completionStartMs < 0L) {
            completionStartMs = now;
        }

        long wait = COMPLETION_HOLD_MS + COMPLETION_FADE_MS;
        if (now - completionStartMs < wait) {
            // Keep SplashOverlay alive until fade animation finishes.
            return;
        }

        client.setOverlay(null);
    }

    private static float smoothProgress(float raw) {
        long now = System.nanoTime();
        if (lastProgressNs == 0L) {
            lastProgressNs = now;
            smoothedProgress = raw;
            return smoothedProgress;
        }
        float dt = (now - lastProgressNs) / 1_000_000_000.0f;
        lastProgressNs = now;
        if (dt < 0.0f || dt > 0.25f) {
            dt = 0.05f;
        }

        if (raw + 0.02f < smoothedProgress) {
            smoothedProgress = raw;
            return smoothedProgress;
        }

        float speed = 9.0f;
        float t = 1.0f - (float) Math.exp(-speed * dt);
        smoothedProgress += (raw - smoothedProgress) * t;
        if (Math.abs(raw - smoothedProgress) < 0.0025f) {
            smoothedProgress = raw;
        }
        return Math.max(0.0f, Math.min(1.0f, smoothedProgress));
    }

    private static void drawRoundedBar(DrawContext ctx, int x, int y, int w, int h, float progress, float alphaMul) {
        int radius = Math.max(4, Math.min(8, h / 2));
        int border = withAlpha(0xFF1E2430, alphaMul);
        int bg = withAlpha(0xFF0A0C12, alphaMul);
        int fill = withAlpha(UiTheme.ACCENT, alphaMul);

        drawRoundedRect(ctx, x, y, w, h, radius, border);
        drawRoundedRect(ctx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), bg);

        int innerW = Math.max(0, w - 4);
        int fillW = Math.max(2, Math.round(innerW * progress));
        if (fillW > 0) {
            drawRoundedRect(ctx, x + 2, y + 2, fillW, h - 4, Math.max(0, radius - 2), fill);
        }
    }

    private static void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);
        int r2 = r * r;
        for (int dy = 0; dy < r; dy++) {
            int dx = (int) Math.floor(Math.sqrt(r2 - (dy * dy)));
            int yTop = y + r - dy - 1;
            int yBot = y + h - r + dy;
            ctx.fill(x + r - dx, yTop, x + r, yTop + 1, color);
            ctx.fill(x + w - r, yTop, x + w - r + dx, yTop + 1, color);
            ctx.fill(x + r - dx, yBot, x + r, yBot + 1, color);
            ctx.fill(x + w - r, yBot, x + w - r + dx, yBot + 1, color);
        }
    }

    private static int withAlpha(int color, float alphaMul) {
        int a = (color >>> 24) & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * alphaMul)));
        return (na << 24) | (color & 0x00FFFFFF);
    }

    private void drawIcon(DrawContext context, Identifier iconId, int x, int y, int w, int h, float alpha, float scale) {
        drawIcon(context, iconId, x, y, w, h, alpha, scale, 0.0f, 0.0f);
    }

    private void drawIcon(DrawContext context, Identifier iconId, int x, int y, int w, int h, float alpha, float scale, float yOffset, float rotation) {
        float sx = (w / (float) ICON_TEX_W) * scale;
        float sy = (h / (float) ICON_TEX_H) * scale;
        float cx = x + w / 2.0f;
        float cy = y + h / 2.0f + yOffset;
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(cx, cy);
        matrices.rotate(rotation);
        matrices.scale(sx, sy);
        int color = withAlpha(0xFFFFFFFF, alpha);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, iconId, -ICON_TEX_W / 2, -ICON_TEX_H / 2, 0.0F, 0.0F,
                ICON_TEX_W, ICON_TEX_H, ICON_TEX_W, ICON_TEX_H, color);
        matrices.popMatrix();
    }
}
