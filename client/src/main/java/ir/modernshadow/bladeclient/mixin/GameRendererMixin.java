package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.render.MotionBlurRenderer;
import ir.modernshadow.bladeclient.render.SaturationRenderer;
import ir.modernshadow.bladeclient.zoom.ZoomModule;
import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    private static float smoothFov = 70.0f;
    private static long smoothLastNs = 0L;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bladeclient$applyZoom(Camera camera, float tickDelta, boolean changingFov,
                                       CallbackInfoReturnable<Float> cir) {
        float base = cir.getReturnValue();

        BladeClientConfig cfg = ConfigManager.get();
        boolean miniEnabled = cfg.miniFov != null && cfg.miniFov.enabled;
        if (miniEnabled && !changingFov) {
            // Keep hand FOV fixed to vanilla 70 and unaffected by Mini FOV/zoom.
            cir.setReturnValue(70.0f);
            return;
        }

        if (miniEnabled) {
            base = applyMiniFov(camera, tickDelta, changingFov, cfg.miniFov);
            if (cfg.miniFov.smoothEffects) {
                base = smoothTo(base);
            } else {
                smoothFov = base;
                smoothLastNs = 0L;
            }
        } else {
            smoothFov = base;
            smoothLastNs = 0L;
        }

        float zoom = ZoomModule.getZoomFactor(tickDelta);
        if (zoom > 1.0f) {
            base = base / zoom;
        }
        cir.setReturnValue(base);
    }

    private static float applyMiniFov(Camera camera, float tickDelta, boolean changingFov, BladeClientConfig.MiniFov cfg) {
        float base = cfg.fov;
        float fov = base;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        boolean firstPerson = mc.options != null && mc.options.getPerspective().isFirstPerson();

        if (changingFov && player != null) {
            boolean sprinting = player.isSprinting();

            if (!cfg.disableDynamicFov) {
                // Potion/movement speed effects (skip when sprinting uses custom offsets)
                if (!sprinting || !cfg.dynamicSprinting) {
                    float walkSpeed = player.getAbilities().getWalkSpeed();
                    if (walkSpeed != 0.0f) {
                        float speed = (float) (player.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) / walkSpeed);
                        float speedMult = (speed + 1.0f) / 2.0f;
                        float effectScale = mc.options.getFovEffectScale().getValue().floatValue();
                        speedMult = MathHelper.lerp(effectScale, 1.0f, speedMult);
                        fov *= speedMult;
                    }
                }
            }

            if (cfg.dynamicFlying && player.getAbilities().flying) {
                fov *= 1.1f;
            }

            if (cfg.dynamicSprinting && sprinting) {
                float delta = (cfg.sprintFov - base) * cfg.sprintMultiplier;
                delta = clampOffset(delta, cfg.sprintMinOffset, cfg.sprintMaxOffset);
                if (delta > 0.0f) {
                    // Sprinting should not increase FOV in Mini FOV.
                    delta = 0.0f;
                }
                fov += delta;
            }

            if (cfg.dynamicAiming && player.isUsingItem()) {
                if (player.getActiveItem().isOf(Items.BOW)) {
                    float t = Math.min(player.getItemUseTime() / 20.0f, 1.0f);
                    float delta = -base * MathHelper.square(t) * 0.15f * cfg.aimingMultiplier;
                    delta = clampOffset(delta, cfg.aimingMinOffset, cfg.aimingMaxOffset);
                    fov += delta;
                } else if (firstPerson && player.isUsingSpyglass()) {
                    fov = base * 0.1f;
                }
            }
        }

        if (!cfg.disableDynamicFov) {
            CameraSubmersionType submersion = camera.getSubmersionType();
            if (submersion == CameraSubmersionType.WATER || submersion == CameraSubmersionType.LAVA) {
                float effectScale = mc.options.getFovEffectScale().getValue().floatValue();
                fov *= MathHelper.lerp(effectScale, 1.0f, 0.85714287f);
            }
        }

        return fov;
    }

    private static float clampOffset(float value, float min, float max) {
        if (min > max) {
            float tmp = min;
            min = max;
            max = tmp;
        }
        return MathHelper.clamp(value, min, max);
    }

    private static float smoothTo(float target) {
        long now = System.nanoTime();
        if (smoothLastNs == 0L) {
            smoothLastNs = now;
            smoothFov = target;
            return smoothFov;
        }
        float dt = (now - smoothLastNs) / 1_000_000_000.0f;
        smoothLastNs = now;
        if (dt < 0.0f || dt > 0.25f) {
            dt = 0.05f;
        }
        // Exponential smoothing per second (higher = faster follow)
        float speed = 8.0f;
        float t = 1.0f - (float) Math.exp(-speed * dt);
        smoothFov += (target - smoothFov) * t;
        return smoothFov;
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void bladeclient$disableHurtShake(net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        BladeClientConfig cfg = ConfigManager.get();
        if (cfg.miniFov != null && cfg.miniFov.enabled && cfg.miniFov.disableHurtCamera) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bladeclient$disableViewBob(net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        BladeClientConfig cfg = ConfigManager.get();
        if (cfg.miniFov != null && cfg.miniFov.enabled && cfg.miniFov.disableViewBobbing) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            shift = At.Shift.BEFORE))
    private void bladeclient$motionBlur(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        MotionBlurRenderer.render(tickCounter);
        SaturationRenderer.render(tickCounter);
    }
}
