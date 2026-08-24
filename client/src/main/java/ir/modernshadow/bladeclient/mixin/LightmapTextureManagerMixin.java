package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @ModifyVariable(method = "update", at = @At("STORE"), index = 9)
    private float bladeclient$overrideNightVisionStrength(float original) {
        BladeClientConfig cfg = ConfigManager.get();
        if (!cfg.nightVision.enabled) {
            return original;
        }
        int level = cfg.nightVision.blockLight;
        if (level < 1) level = 1;
        if (level > 15) level = 15;
        return level / 15.0f;
    }
}
