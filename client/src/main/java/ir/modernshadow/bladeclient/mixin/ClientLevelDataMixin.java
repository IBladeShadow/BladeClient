package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$ClientLevelData")
public class ClientLevelDataMixin {
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void bladeclient$overrideClientDayTime(CallbackInfoReturnable<Long> cir) {
        BladeClientConfig.TimeChanger cfg = ConfigManager.get().timeChanger;
        if (cfg == null || !cfg.enabled) {
            return;
        }
        long time = cfg.time;
        if (time < 0) time = 0;
        time = time % 24000L;
        cir.setReturnValue(time);
    }
}
