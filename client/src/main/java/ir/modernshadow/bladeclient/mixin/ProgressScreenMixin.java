package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.screen.UiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProgressScreen.class)
public class ProgressScreenMixin {
    private static final int TEXT_GRAY = 0xFF9EA7B3;
    private static final int BAR_BG = 0xFF1F2430;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.textRenderer == null) return;

            int sw = ctx.getScaledWindowWidth();
            int sh = ctx.getScaledWindowHeight();

            int titleY = Math.max(20, sh / 2 - 48);
            UiTheme.drawBackground(ctx, sw, sh);
            String title = "BladeClient";
            int titleW = BladeFonts.titleWidth(title, BladeFonts.TITLE_SIZE);
            BladeFonts.drawTitle(ctx, title, sw / 2.0f - titleW / 2.0f, titleY, UiTheme.TEXT_PRIMARY, BladeFonts.TITLE_SIZE, true);

            int left = Math.round(sw * 0.20f);
            int right = Math.round(sw * 0.80f);
            int barW = Math.max(40, right - left);
            int barX0 = left;
            int barY = Math.max(titleY + 28, sh / 2);
            int barX1 = left + barW;
            int barH = 10;

            ctx.fill(barX0, barY, barX1, barY + barH, BAR_BG);

            long t = System.nanoTime();
            long cycle = 1_000_000_000L;
            double frac = ((t % cycle) / (double) cycle);
            float segFrac = 0.28f;
            int segW = Math.max(8, Math.round(barW * segFrac));
            int segX = barX0 + Math.round((float) ((barW + segW) * frac)) - segW;
            if (segX < barX0) segX = barX0;
            if (segX + segW > barX1) segX = barX1 - segW;
            ctx.fill(segX, barY, segX + segW, barY + barH, UiTheme.ACCENT);

            String subtitle = "";
            try {
                var m = ProgressScreen.class.getMethod("getMessage");
                Object ret = m.invoke((ProgressScreen) (Object) this);
                if (ret != null) subtitle = ret.toString();
            } catch (Throwable ignored) {
            }

            if (!subtitle.isEmpty()) {
                int subW = BladeFonts.uiWidth(subtitle, BladeFonts.UI_SMALL);
                BladeFonts.drawUi(ctx, subtitle, sw / 2.0f - subW / 2.0f, barY + barH + 8, TEXT_GRAY, BladeFonts.UI_SMALL, true);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
