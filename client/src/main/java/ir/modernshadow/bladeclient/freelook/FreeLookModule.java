package ir.modernshadow.bladeclient.freelook;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.mixin.KeyBindingAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class FreeLookModule {
    private FreeLookModule() {}

    private static KeyBinding keyBinding;
    private static boolean active = false;
    private static boolean wasActive = false;
    private static Perspective previousPerspective = null;
    private static float yaw = 0.0f;
    private static float pitch = 0.0f;
    private static float lockedYaw = 0.0f;
    private static float lockedPitch = 0.0f;
    private static float smoothedBodyYaw = 0.0f;
    private static final float LOOK_STEP = 0.15f;
    private static final float BODY_YAW_MAX = 45.0f;
    private static final float BODY_YAW_SMOOTH = 0.2f;

    public static void register(KeyBinding key) {
        keyBinding = key;
        ClientTickEvents.END_CLIENT_TICK.register(FreeLookModule::onTick);
    }

    public static KeyBinding getKeyBinding() {
        return keyBinding;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    public static void handleLookInput(double deltaX, double deltaY) {
        if (!active) return;
        yaw += (float) (deltaX * LOOK_STEP);
        pitch += (float) (deltaY * LOOK_STEP);
        pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
    }

    private static void onTick(MinecraftClient client) {
        if (client == null) return;
        BladeClientConfig cfg = ConfigManager.get();

        boolean keyDown = cfg.freeLook.enabled && isKeyDown(client) && client.currentScreen == null;
        if (keyDown && !wasActive) {
            previousPerspective = client.options.getPerspective();
            if (client.player != null) {
                lockedYaw = client.player.getYaw();
                lockedPitch = client.player.getPitch();
                yaw = lockedYaw;
                pitch = lockedPitch;
                smoothedBodyYaw = lockedYaw;
            }
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }

        if (!keyDown && wasActive) {
            if (previousPerspective != null) {
                client.options.setPerspective(previousPerspective);
            }
            previousPerspective = null;
        }

        active = keyDown;
        wasActive = keyDown;

        if (active && client.player != null) {
            ClientPlayerEntity player = client.player;
            player.setYaw(lockedYaw);
            player.setPitch(lockedPitch);

            float bodyYaw = clampBodyYaw(getMovementYaw(player, lockedYaw), lockedYaw);
            smoothedBodyYaw = MathHelper.lerpAngleDegrees(BODY_YAW_SMOOTH, smoothedBodyYaw, bodyYaw);
            player.setHeadYaw(lockedYaw);
            player.setBodyYaw(smoothedBodyYaw);
        }
    }

    private static float getMovementYaw(ClientPlayerEntity player, float fallback) {
        Vec3d vel = player.getVelocity();
        double x = vel.x;
        double z = vel.z;
        if ((x * x + z * z) < 1.0E-4) {
            return fallback;
        }
        return (float) (MathHelper.atan2(z, x) * 57.2957763671875) - 90.0f;
    }

    private static float clampBodyYaw(float targetYaw, float headYaw) {
        float delta = MathHelper.wrapDegrees(targetYaw - headYaw);
        delta = MathHelper.clamp(delta, -BODY_YAW_MAX, BODY_YAW_MAX);
        return headYaw + delta;
    }

    private static boolean isKeyDown(MinecraftClient client) {
        if (keyBinding == null || client == null) return false;
        InputUtil.Key key = ((KeyBindingAccessor) keyBinding).bladeclient$getBoundKey();
        if (key == null || key == InputUtil.UNKNOWN_KEY) return false;
        long window = client.getWindow().getHandle();
        InputUtil.Type type = key.getCategory();
        if (type == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getCode()) == GLFW.GLFW_PRESS;
        }
        if (type == InputUtil.Type.KEYSYM) {
            return InputUtil.isKeyPressed(window, key.getCode());
        }
        return keyBinding.isPressed();
    }
}
