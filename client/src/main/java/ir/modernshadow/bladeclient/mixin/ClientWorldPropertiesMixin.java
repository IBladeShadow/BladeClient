package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Shadow
    private long timeOfDay;

    @Inject(method = "setTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void bladeclient$overrideTimeOfDay(long timeOfDay, CallbackInfo ci) {
        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.timeChanger.enabled) {
            return;
        }
        int time = cfg.timeChanger.time;
        if (time < 0) time = 0;
        if (time > 23999) time = 23999;
        this.timeOfDay = time;
        ci.cancel();
    }
}

