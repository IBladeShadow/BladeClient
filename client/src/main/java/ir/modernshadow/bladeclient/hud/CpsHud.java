package ir.modernshadow.bladeclient.hud;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CpsHud {
    private CpsHud() {}

    private static final List<Long> leftClicks = new ArrayList<>();
    private static final List<Long> rightClicks = new ArrayList<>();
    private static boolean prevLeft = false;
    private static boolean prevRight = false;

    public static void register() {
        HudRenderCallback.EVENT.register(CpsHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        if (!RenderUtil.shouldRenderHud(mc)) return;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.cps.enabled) return;

        long window = mc.getWindow().getHandle();
        boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        updateClicks(left, right);

        int leftCps = leftClicks.size();
        int rightCps = rightClicks.size();

        String prefix = cfg.cps.showLabel ? "CPS: " : "";
        String text = cfg.cps.showRight ? (prefix + leftCps + " | " + rightCps) : (prefix + leftCps);

        float scale = RenderUtil.clamp(cfg.cps.scale, 0.5f, 3.0f);
        int x = cfg.cps.x;
        int y = cfg.cps.y;

        int textW = mc.textRenderer.getWidth(text);
        int textH = mc.textRenderer.fontHeight;
        int scaledW = RenderUtil.scale(textW, scale);
        int scaledH = RenderUtil.scale(textH, scale);

        if (cfg.cps.showBackground) {
            int pad = 6;
            int bg = RenderUtil.withAlpha(0x10131B, cfg.cps.backgroundOpacity);
            ctx.fill(x - pad, y - pad, x + scaledW + pad, y + scaledH + pad, bg);
        }
        RenderUtil.drawScaledText(ctx, mc.textRenderer, RenderUtil.mcOrdered(text), x, y, 0xFFFFFFFF, scale);
    }

    private static void updateClicks(boolean leftPressed, boolean rightPressed) {
        long now = System.currentTimeMillis();
        if (leftPressed && !prevLeft) leftClicks.add(now);
        if (rightPressed && !prevRight) rightClicks.add(now);
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
}
