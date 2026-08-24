package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.freelook.FreeLookModule;
import ir.modernshadow.bladeclient.screen.PackDragState;
import ir.modernshadow.bladeclient.screen.PlayerQuickActionScreen;
import ir.modernshadow.bladeclient.zoom.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    private static double smoothDeltaX = 0.0;
    private static double smoothDeltaY = 0.0;
    private static boolean smoothLookActive = false;

    @Redirect(
            method = "updateMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V")
    )
    private void bladeclient$freeLookChangeLookDirection(ClientPlayerEntity player, double deltaX, double deltaY) {
        if (FreeLookModule.isActive()) {
            FreeLookModule.handleLookInput(deltaX, deltaY);
            return;
        }
        if (shouldSmoothLook()) {
            float zoom = ZoomModule.getZoomFactor(1.0f);
            float factor = MathHelper.clamp(0.12f / zoom, 0.02f, 0.12f);
            smoothDeltaX += (deltaX - smoothDeltaX) * factor;
            smoothDeltaY += (deltaY - smoothDeltaY) * factor;
            player.changeLookDirection(smoothDeltaX, smoothDeltaY);
            smoothLookActive = true;
            return;
        }

        if (smoothLookActive) {
            smoothDeltaX = 0.0;
            smoothDeltaY = 0.0;
            smoothLookActive = false;
        }
        player.changeLookDirection(deltaX, deltaY);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void bladeclient$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ZoomModule.handleScroll(vertical)) {
            ci.cancel();
        }
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void bladeclient$handlePackDrag(long window, int button, int action, int mods, CallbackInfo ci) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && action == GLFW.GLFW_PRESS) {
            if (openQuickActions()) {
                ci.cancel();
                return;
            }
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_1) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof PackScreen screen)) {
            if (action == GLFW.GLFW_RELEASE) {
                PackDragState.clear();
            }
            return;
        }

        double mouseX = client.mouse.getX() * (double) client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * (double) client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        if (action == GLFW.GLFW_PRESS) {
            PackDragState.handlePress(screen, mouseX, mouseY);
        } else if (action == GLFW.GLFW_RELEASE) {
            PackDragState.handleRelease(screen, mouseX, mouseY);
        }
    }

    private boolean openQuickActions() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen != null) {
            return false;
        }

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult entityHit)) {
            return false;
        }

        if (!(entityHit.getEntity() instanceof PlayerEntity target)) {
            return false;
        }

        if (client.player != null && target.getUuid().equals(client.player.getUuid())) {
            return false;
        }

        String name = target.getGameProfile().getName();
        if (name == null || name.isBlank()) {
            name = target.getName().getString();
        }
        client.setScreen(new PlayerQuickActionScreen(name));
        return true;
    }

    private boolean shouldSmoothLook() {
        if (!ZoomModule.isZooming()) {
            return false;
        }
        BladeClientConfig cfg = ConfigManager.get();
        if (cfg == null || cfg.zoom == null) {
            return false;
        }
        return cfg.zoom.enabled && Boolean.TRUE.equals(cfg.zoom.smoothLook);
    }
}
