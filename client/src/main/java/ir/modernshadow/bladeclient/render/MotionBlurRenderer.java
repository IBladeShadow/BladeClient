package ir.modernshadow.bladeclient.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.mixin.PostEffectPassAccessor;
import ir.modernshadow.bladeclient.mixin.PostEffectProcessorAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.Pool;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public final class MotionBlurRenderer {
    private MotionBlurRenderer() {}

    private static final Identifier EFFECT_ID = Identifier.of("bladeclient", "motion_blur");
    private static final Pool EFFECT_POOL = new Pool(32);

    private static PostEffectProcessor effect;
    private static GpuBuffer strengthBuffer;
    private static long failedUntilMs;
    private static long lastErrorLogMs;
    private static boolean hasPrevFrame = false;
    private static float smoothedStrength = 0.0f;
    private static long lastStrengthNs = 0L;
    private static boolean lastValid = false;
    private static float lastYaw;
    private static float lastPitch;
    private static double lastX;
    private static double lastY;
    private static double lastZ;

    public static void render(RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.getFramebuffer() == null) {
            reset();
            return;
        }

        BladeClientConfig cfg = ConfigManager.get();
        if (cfg.motionBlur == null || !cfg.motionBlur.enabled) {
            reset();
            return;
        }

        if (client.currentScreen != null) {
            reset();
            return;
        }

        if (isFailed()) {
            return;
        }

        PostEffectProcessor processor = ensureEffect(client);
        if (processor == null || strengthBuffer == null || !isBufferAlive(strengthBuffer)) {
            invalidate();
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        float deltaTicks = tickCounter != null ? clamp(tickCounter.getDynamicDeltaTicks(), 0.05f, 3.0f) : 1.0f;
        float baseStrength = clamp(cfg.motionBlur.strength, 0.0f, 0.95f);
        float fpsFactor = computeFpsFactor(client.getCurrentFps());
        float perspectiveFactor = perspectiveFactor(client);
        float motionBoost = computeMotionBoost(camera, deltaTicks, client);
        float targetStrength = clamp(baseStrength * fpsFactor * perspectiveFactor * (1.0f + motionBoost * 0.9f), 0.0f, 0.90f);
        if (client.options != null && !client.options.getPerspective().isFirstPerson()) {
            targetStrength = Math.min(targetStrength, 0.55f);
        }
        float strength = smoothStrength(targetStrength);
        if (!hasPrevFrame) {
            strength = 0.0f;
            smoothedStrength = 0.0f;
        }

        updateStrength(strengthBuffer, strength);
        try {
            processor.render(client.getFramebuffer(), EFFECT_POOL);
            EFFECT_POOL.decrementLifespan();
            hasPrevFrame = true;
        } catch (Throwable t) {
            long nowErr = System.currentTimeMillis();
            if (nowErr - lastErrorLogMs > 1000L) {
                System.err.println("[BladeClient] Motion blur render failed: " + t.getMessage());
                lastErrorLogMs = nowErr;
            }
            invalidate();
        }
    }

    private static PostEffectProcessor ensureEffect(MinecraftClient client) {
        if (effect != null && strengthBuffer != null && isBufferAlive(strengthBuffer)) {
            return effect;
        }
        dropEffect();
        try {
            PostEffectProcessor loaded = client.getShaderLoader().loadPostEffect(EFFECT_ID, DefaultFramebufferSet.MAIN_ONLY);
            GpuBuffer loadedBuffer = findStrengthBuffer(loaded);
            effect = loaded;
            strengthBuffer = loadedBuffer;
            clearFailed();
            return loaded;
        } catch (Throwable t) {
            System.err.println("[BladeClient] Failed to load motion blur effect: " + t.getMessage());
            dropEffect();
            setFailed(System.currentTimeMillis() + 2000L);
            return null;
        }
    }

    private static void updateStrength(GpuBuffer buffer, float strength) {
        if (buffer == null) return;
        RenderSystem.assertOnRenderThread();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = Math.max(16, buffer.size());
            ByteBuffer data = stack.malloc(size);
            Std140Builder.intoBuffer(data).putFloat(strength);
            data.flip();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(buffer.slice(), data);
        } catch (Throwable t) {
            // If the buffer can't be updated, keep the last value.
        }
    }

    private static GpuBuffer findStrengthBuffer(PostEffectProcessor processor) {
        if (processor == null) return null;
        List<PostEffectPass> passes = ((PostEffectProcessorAccessor) processor).bladeclient$getPasses();
        for (PostEffectPass pass : passes) {
            Map<String, GpuBuffer> buffers = ((PostEffectPassAccessor) pass).bladeclient$getUniformBuffers();
            if (buffers != null && buffers.containsKey("MotionBlurConfig")) {
                return ensureWritableBuffer(buffers, buffers.get("MotionBlurConfig"));
            }
        }
        return null;
    }

    private static boolean isBufferAlive(GpuBuffer buffer) {
        if (buffer == null) return false;
        try {
            return !buffer.isClosed();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void reset() {
        hasPrevFrame = false;
        smoothedStrength = 0.0f;
        lastStrengthNs = 0L;
        lastValid = false;
    }

    public static void close() {
        disposeEffect();
        hasPrevFrame = false;
        smoothedStrength = 0.0f;
        lastStrengthNs = 0L;
        lastValid = false;
        EFFECT_POOL.clear();
    }

    private static float computeMotionBoost(Camera camera, float deltaTicks, MinecraftClient client) {
        if (camera == null) {
            lastValid = false;
            return 0.0f;
        }
        Vec3d pos = camera.getPos();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();

        if (!lastValid) {
            lastValid = true;
            lastYaw = yaw;
            lastPitch = pitch;
            lastX = pos.x;
            lastY = pos.y;
            lastZ = pos.z;
            return 0.0f;
        }

        float dyaw = MathHelper.wrapDegrees(yaw - lastYaw);
        float dpitch = pitch - lastPitch;
        double dx = pos.x - lastX;
        double dy = pos.y - lastY;
        double dz = pos.z - lastZ;

        lastYaw = yaw;
        lastPitch = pitch;
        lastX = pos.x;
        lastY = pos.y;
        lastZ = pos.z;

        float tickScale = Math.max(0.05f, deltaTicks);
        float rotPerTick = (Math.abs(dyaw) + Math.abs(dpitch)) / tickScale;
        float movePerTick = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / tickScale;

        if (client.options != null && !client.options.getPerspective().isFirstPerson()) {
            // Third-person camera path exaggerates movement; keep boost much lower in F5.
            rotPerTick *= 0.65f;
            movePerTick *= 0.35f;
        }

        float boost = rotPerTick * 0.03f + movePerTick * 2.0f;
        return clamp(boost, 0.0f, 1.0f);
    }

    private static float perspectiveFactor(MinecraftClient client) {
        if (client == null || client.options == null) return 1.0f;
        Perspective perspective = client.options.getPerspective();
        return perspective != null && !perspective.isFirstPerson() ? 0.70f : 1.0f;
    }

    private static float computeFpsFactor(int fps) {
        if (fps <= 0) return 1.0f;
        if (fps >= 120) return 1.0f;
        if (fps >= 90) return MathHelper.lerp((fps - 90f) / 30f, 0.92f, 1.0f);
        if (fps >= 60) return MathHelper.lerp((fps - 60f) / 30f, 0.75f, 0.92f);
        if (fps >= 45) return MathHelper.lerp((fps - 45f) / 15f, 0.58f, 0.75f);
        if (fps >= 30) return MathHelper.lerp((fps - 30f) / 15f, 0.42f, 0.58f);
        return MathHelper.lerp(Math.max(0.0f, fps) / 30f, 0.25f, 0.42f);
    }

    private static float smoothStrength(float target) {
        long now = System.nanoTime();
        if (lastStrengthNs == 0L) {
            lastStrengthNs = now;
            smoothedStrength = target;
            return smoothedStrength;
        }
        float dt = (now - lastStrengthNs) / 1_000_000_000.0f;
        lastStrengthNs = now;
        if (dt < 0.0f || dt > 0.25f) {
            dt = 0.05f;
        }
        float speed = 10.0f;
        float t = 1.0f - (float) Math.exp(-speed * dt);
        smoothedStrength += (target - smoothedStrength) * t;
        return clamp(smoothedStrength, 0.0f, 0.95f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static GpuBuffer ensureWritableBuffer(Map<String, GpuBuffer> buffers, GpuBuffer existing) {
        if (existing == null) return null;
        int usage = existing.usage();
        int want = usage | GpuBuffer.USAGE_COPY_DST;
        if ((usage & GpuBuffer.USAGE_COPY_DST) != 0) {
            return existing;
        }
        try {
            RenderSystem.assertOnRenderThread();
            int size = Math.max(16, existing.size());
            GpuBuffer replacement = RenderSystem.getDevice().createBuffer(
                    () -> "bladeclient_motion_blur",
                    want,
                    size
            );
            buffers.put("MotionBlurConfig", replacement);
            existing.close();
            return replacement;
        } catch (Throwable t) {
            return existing;
        }
    }

    private static void dropEffect() {
        effect = null;
        strengthBuffer = null;
        EFFECT_POOL.clear();
    }

    private static void disposeEffect() {
        if (effect != null) {
            effect.close();
        }
        dropEffect();
    }

    private static void invalidate() {
        dropEffect();
        setFailed(System.currentTimeMillis() + 2000L);
    }

    private static boolean isFailed() {
        return System.currentTimeMillis() < failedUntilMs;
    }

    private static void setFailed(long until) {
        failedUntilMs = until;
    }

    private static void clearFailed() {
        setFailed(0L);
    }
}
