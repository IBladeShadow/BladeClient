package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.screen.BladeClientMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ResponsiveScreenMixin {
    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("TAIL"))
    private void bladeclient$init(MinecraftClient client, int width, int height, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!shouldResponsive(screen)) return;

        if (screen instanceof BladeClientMenuScreen) {
            float scale = computeMenuScale(width, height);
            if (scale < 0.999f) {
                int fontH = client != null && client.textRenderer != null ? client.textRenderer.fontHeight : 9;
                int minH = fontH + 6;
                for (Element e : screen.children()) {
                    if (e instanceof ClickableWidget w) {
                        int newW = Math.max(40, Math.round(w.getWidth() * scale));
                        int newH = Math.max(minH, Math.round(w.getHeight() * scale));
                        if (w instanceof ClickableWidgetAccessor acc) {
                            acc.bladeclient$setWidth(newW);
                            acc.bladeclient$setHeight(newH);
                        }
                    }
                }
            }
            return; // keep positions unchanged for the right-shift menu
        }

        int pad = 6;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean hasWidgets = false;

        for (Element e : screen.children()) {
            if (e instanceof ClickableWidget w) {
                hasWidgets = true;
                minX = Math.min(minX, w.getX());
                minY = Math.min(minY, w.getY());
                maxX = Math.max(maxX, w.getX() + w.getWidth());
                maxY = Math.max(maxY, w.getY() + w.getHeight());
            }
        }

        if (!hasWidgets) return;

        int dx = 0;
        int dy = 0;
        if (minX < pad) dx = pad - minX;
        else if (maxX > width - pad) dx = (width - pad) - maxX;
        if (minY < pad) dy = pad - minY;
        else if (maxY > height - pad) dy = (height - pad) - maxY;

        if (dx != 0 || dy != 0) {
            for (Element e : screen.children()) {
                if (e instanceof ClickableWidget w) {
                    w.setX(w.getX() + dx);
                    w.setY(w.getY() + dy);
                }
            }
        }

        // Final clamp to ensure no widget goes out of bounds
        for (Element e : screen.children()) {
            if (e instanceof ClickableWidget w) {
                int maxXPos = Math.max(pad, width - pad - w.getWidth());
                int maxYPos = Math.max(pad, height - pad - w.getHeight());
                w.setX(clamp(w.getX(), pad, maxXPos));
                w.setY(clamp(w.getY(), pad, maxYPos));
            }
        }
    }

    private boolean shouldResponsive(Screen screen) {
        if (screen == null) return false;
        // Exclude in-game UI and chat
        if (screen instanceof HandledScreen<?> || screen instanceof ChatScreen || screen instanceof DeathScreen) {
            return false;
        }
        // Apply to all menus including our custom screens
        return true;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private float computeMenuScale(int width, int height) {
        float baseW = 960f;
        float baseH = 540f;
        float s = Math.min(width / baseW, height / baseH);
        return Math.max(0.75f, Math.min(1.0f, s));
    }
}
