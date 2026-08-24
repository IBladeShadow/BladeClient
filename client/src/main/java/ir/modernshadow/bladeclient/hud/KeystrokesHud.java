package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class KeystrokesHud {
    private KeystrokesHud() {}

    private static final List<Long> leftClicks = new ArrayList<>();
    private static final List<Long> rightClicks = new ArrayList<>();
    private static boolean prevLeft = false;
    private static boolean prevRight = false;

    private static float wAnim = 0f;
    private static float aAnim = 0f;
    private static float sAnim = 0f;
    private static float dAnim = 0f;
    private static float spaceAnim = 0f;
    private static float lmbAnim = 0f;
    private static float rmbAnim = 0f;

    public static void register() {
        HudRenderCallback.EVENT.register(KeystrokesHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.keystrokes.enabled) return;

        float scale = RenderUtil.clamp(cfg.keystrokes.scale, 0.5f, 3.0f);
        int x = cfg.keystrokes.x;
        int y = cfg.keystrokes.y;

        int key = Math.max(8, Math.round(24 * scale));
        int gap = 0;
        int pad = Math.max(4, Math.round(4 * scale));

        int rowW = key * 3 + gap * 2;
        int mouseH = Math.max(18, Math.round(22 * scale));
        int spaceH = Math.max(14, Math.round(16 * scale));

        boolean wPressed = mc.options.forwardKey.isPressed();
        boolean aPressed = mc.options.leftKey.isPressed();
        boolean sPressed = mc.options.backKey.isPressed();
        boolean dPressed = mc.options.rightKey.isPressed();
        boolean spacePressed = mc.options.jumpKey.isPressed();

        long window = mc.getWindow().getHandle();
        boolean leftPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        wAnim = smooth(wAnim, wPressed);
        aAnim = smooth(aAnim, aPressed);
        sAnim = smooth(sAnim, sPressed);
        dAnim = smooth(dAnim, dPressed);
        spaceAnim = smooth(spaceAnim, spacePressed);
        lmbAnim = smooth(lmbAnim, leftPressed);
        rmbAnim = smooth(rmbAnim, rightPressed);

        int curY = y + pad;
        int rowX = x + pad;

        drawKey(ctx, mc, rowX + key + gap, curY, key, key, "W", wAnim, null);
        curY += key + gap;

        drawKey(ctx, mc, rowX, curY, key, key, "A", aAnim, null);
        drawKey(ctx, mc, rowX + key + gap, curY, key, key, "S", sAnim, null);
        drawKey(ctx, mc, rowX + (key + gap) * 2, curY, key, key, "D", dAnim, null);
        curY += key + gap;

        if (cfg.keystrokes.showSpace) {
            drawKey(ctx, mc, rowX, curY, rowW, spaceH, "§m        ", spaceAnim, null);
            curY += spaceH + gap;
        }

        if (cfg.keystrokes.showMouse) {
            int mouseExtra = Math.max(0, (int) Math.round(1.2 * scale));
            int mouseW = (rowW - gap) / 2 + mouseExtra;
            int mouseX = rowX - mouseExtra;

            updateCps(leftPressed, rightPressed);

            String leftSub = cfg.keystrokes.showCps ? Integer.toString(leftClicks.size()) : null;
            String rightSub = cfg.keystrokes.showCps ? Integer.toString(rightClicks.size()) : null;

            int lmbW = mouseW - 1;
            int rmbW = mouseW - 1;
            int mouseShift = 1;
            drawKey(ctx, mc, mouseX + mouseShift, curY, lmbW, mouseH, "LMB", lmbAnim, leftSub);
            drawKey(ctx, mc, mouseX + mouseShift + lmbW + gap, curY, rmbW, mouseH, "RMB", rmbAnim, rightSub);
        }
    }

    private static void updateCps(boolean leftPressed, boolean rightPressed) {
        long now = System.currentTimeMillis();

        if (leftPressed && !prevLeft) {
            leftClicks.add(now);
        }
        if (rightPressed && !prevRight) {
            rightClicks.add(now);
        }
        prevLeft = leftPressed;
        prevRight = rightPressed;

        prune(leftClicks, now);
        prune(rightClicks, now);
    }

    private static void prune(List<Long> list, long now) {
        long cutoff = now - 1000L;
        Iterator<Long> it = list.iterator();
        while (it.hasNext()) {
            if (it.next() < cutoff) it.remove();
            else break;
        }
    }

    private static void drawKey(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h,
                                String label, float pressedAnim, String subtitle) {
        int border = 0x33000000; // black, 20% opacity
        int fill = 0x33000000;   // black, 20% opacity

        ctx.fill(x, y, x + w, y + h, border);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);

        int glowAlpha = (int) (0x88 * pressedAnim);
        if (glowAlpha > 0) {
            ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, (glowAlpha << 24) | 0xFFFFFF);
        }

        int textY = y + (h - mc.textRenderer.fontHeight) / 2;
        if (subtitle != null && !subtitle.isEmpty()) {
            textY -= mc.textRenderer.fontHeight / 2;
        }

        int labelColor = blend(0xFFFFFFFF, 0xFF000000, pressedAnim);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, RenderUtil.mcText(label), x + w / 2, textY, labelColor);
        if (subtitle != null && !subtitle.isEmpty()) {
            int subColor = blend(0xFFB6BDC8, 0xFF000000, pressedAnim);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, RenderUtil.mcText(subtitle), x + w / 2,
                    textY + mc.textRenderer.fontHeight + 1, subColor);
        }
    }

    private static float smooth(float current, boolean targetOn) {
        float target = targetOn ? 1.0f : 0.0f;
        float next = current + (target - current) * 0.25f;
        if (Math.abs(target - next) < 0.01f) {
            return target;
        }
        return next;
    }

    private static int blend(int from, int to, float t) {
        float v = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = (from >>> 24) & 0xFF;
        int r1 = (from >>> 16) & 0xFF;
        int g1 = (from >>> 8) & 0xFF;
        int b1 = from & 0xFF;
        int a2 = (to >>> 24) & 0xFF;
        int r2 = (to >>> 16) & 0xFF;
        int g2 = (to >>> 8) & 0xFF;
        int b2 = to & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * v);
        int r = Math.round(r1 + (r2 - r1) * v);
        int g = Math.round(g1 + (g2 - g1) * v);
        int b = Math.round(b1 + (b2 - b1) * v);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
