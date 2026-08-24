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
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Pool;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public final class SaturationRenderer {
    private SaturationRenderer() {}

    private static final Identifier EFFECT_ID = Identifier.of("bladeclient", "saturation");
    private static final Pool EFFECT_POOL = new Pool(16);

    private static PostEffectProcessor effect;
    private static GpuBuffer amountBuffer;
    private static long failedUntilMs;
    private static long lastErrorLogMs;

    public static void render(RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.getFramebuffer() == null) {
            return;
        }

        BladeClientConfig cfg = ConfigManager.get();
        if (cfg.saturation == null || !cfg.saturation.enabled) {
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        if (isFailed()) {
            return;
        }

        PostEffectProcessor processor = ensureEffect(client);
        if (processor == null || amountBuffer == null || !isBufferAlive(amountBuffer)) {
            invalidate();
            return;
        }

        float amount = clamp(cfg.saturation.amount, 0.0f, 2.0f);
        updateAmount(amountBuffer, amount);
        try {
            processor.render(client.getFramebuffer(), EFFECT_POOL);
            EFFECT_POOL.decrementLifespan();
        } catch (Throwable t) {
            long nowErr = System.currentTimeMillis();
            if (nowErr - lastErrorLogMs > 1000L) {
                System.err.println("[BladeClient] Saturation render failed: " + t.getMessage());
                lastErrorLogMs = nowErr;
            }
            invalidate();
        }
    }

    private static PostEffectProcessor ensureEffect(MinecraftClient client) {
        if (effect != null && amountBuffer != null && isBufferAlive(amountBuffer)) {
            return effect;
        }
        dropEffect();
        try {
            PostEffectProcessor loaded = client.getShaderLoader().loadPostEffect(EFFECT_ID, DefaultFramebufferSet.MAIN_ONLY);
            GpuBuffer loadedBuffer = findAmountBuffer(loaded);
            effect = loaded;
            amountBuffer = loadedBuffer;
            clearFailed();
            return loaded;
        } catch (Throwable t) {
            System.err.println("[BladeClient] Failed to load saturation effect: " + t.getMessage());
            dropEffect();
            setFailed(System.currentTimeMillis() + 2000L);
            return null;
        }
    }

    private static void updateAmount(GpuBuffer buffer, float amount) {
        if (buffer == null) return;
        RenderSystem.assertOnRenderThread();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = Math.max(16, buffer.size());
            ByteBuffer data = stack.malloc(size);
            Std140Builder.intoBuffer(data).putFloat(amount);
            data.flip();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(buffer.slice(), data);
        } catch (Throwable t) {
            // If the buffer can't be updated, keep the last value.
        }
    }

    private static GpuBuffer findAmountBuffer(PostEffectProcessor processor) {
        if (processor == null) return null;
        List<PostEffectPass> passes = ((PostEffectProcessorAccessor) processor).bladeclient$getPasses();
        for (PostEffectPass pass : passes) {
            Map<String, GpuBuffer> buffers = ((PostEffectPassAccessor) pass).bladeclient$getUniformBuffers();
            if (buffers != null && buffers.containsKey("SaturationConfig")) {
                return ensureWritableBuffer(buffers, buffers.get("SaturationConfig"));
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
                    () -> "bladeclient_saturation",
                    want,
                    size
            );
            buffers.put("SaturationConfig", replacement);
            existing.close();
            return replacement;
        } catch (Throwable t) {
            return existing;
        }
    }

    private static void dropEffect() {
        effect = null;
        amountBuffer = null;
        EFFECT_POOL.clear();
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
