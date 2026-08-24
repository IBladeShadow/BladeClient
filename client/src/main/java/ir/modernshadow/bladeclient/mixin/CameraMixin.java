package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.freelook.FreeLookModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Redirect(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw(F)F")
    )
    private float bladeclient$freeLookYaw(Entity entity, float tickDelta) {
        if (FreeLookModule.isActive() && entity instanceof AbstractClientPlayerEntity) {
            return FreeLookModule.getYaw();
        }
        return entity.getYaw(tickDelta);
    }

    @Redirect(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPitch(F)F")
    )
    private float bladeclient$freeLookPitch(Entity entity, float tickDelta) {
        if (FreeLookModule.isActive() && entity instanceof AbstractClientPlayerEntity) {
            return FreeLookModule.getPitch();
        }
        return entity.getPitch(tickDelta);
    }
}
