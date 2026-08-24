package ir.modernshadow.bladeclient.zoom;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.mixin.KeyBindingAccessor;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class ZoomModule {
    private ZoomModule() {}

    private static KeyBinding zoomKey;
    private static float currentZoom = 1.0f;
    private static float prevZoom = 1.0f;
    private static boolean zooming = false;
    private static boolean smoothEnabled = true;
    private static float scrollOffset = 0.0f;
    private static boolean wasActive = false;

    public static void register(KeyBinding key) {
        zoomKey = key;
        ClientTickEvents.END_CLIENT_TICK.register(ZoomModule::onTick);
    }

    public static KeyBinding getKeyBinding() {
        return zoomKey;
    }

    public static boolean isZooming() {
        return zooming;
    }

    public static float getZoomFactor(float tickDelta) {
        if (!zooming) return 1.0f;
        if (!smoothEnabled) return currentZoom;
        return MathHelper.lerp(tickDelta, prevZoom, currentZoom);
    }

    public static boolean handleScroll(double verticalAmount) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.currentScreen != null) return false;

        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.zoom.enabled || !cfg.zoom.scrollZoom) return false;
        if (!isZoomKeyDown(mc)) return false;

        float step = (float) verticalAmount;
        if (step == 0f) return false;

        scrollOffset = RenderUtil.clamp(scrollOffset + step, -48.0f, 48.0f);
        return true;
    }

    private static void onTick(MinecraftClient client) {
        if (client == null) return;
        BladeClientConfig cfg = ConfigManager.get();

        prevZoom = currentZoom;
        smoothEnabled = cfg.zoom.smoothZoom;

        boolean active = cfg.zoom.enabled
                && isZoomKeyDown(client)
                && client.currentScreen == null;

        if (!active) {
            scrollOffset = 0.0f;
        } else if (!wasActive) {
            scrollOffset = 0.0f;
        }
        wasActive = active;

        float baseZoom = RenderUtil.clamp(cfg.zoom.zoom, 2.0f, 50.0f);
        float target = active ? RenderUtil.clamp(baseZoom + scrollOffset, 2.0f, 50.0f) : 1.0f;

        if (smoothEnabled) {
            float rate = target < currentZoom ? 0.6f : 0.12f;
            currentZoom += (target - currentZoom) * rate;
            if (Math.abs(currentZoom - target) < 0.01f) {
                currentZoom = target;
            }
        } else {
            currentZoom = target;
        }

        zooming = currentZoom > 1.01f || prevZoom > 1.01f;
    }

    private static boolean isZoomKeyDown(MinecraftClient client) {
        if (zoomKey == null || client == null) return false;
        InputUtil.Key key = ((KeyBindingAccessor) zoomKey).bladeclient$getBoundKey();
        if (key == null || key == InputUtil.UNKNOWN_KEY) return false;
        long window = client.getWindow().getHandle();
        InputUtil.Type type = key.getCategory();
        if (type == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getCode()) == GLFW.GLFW_PRESS;
        }
        if (type == InputUtil.Type.KEYSYM) {
            return InputUtil.isKeyPressed(window, key.getCode());
        }
        // Fallback for scancode or unknown categories
        return zoomKey.isPressed();
    }
}
