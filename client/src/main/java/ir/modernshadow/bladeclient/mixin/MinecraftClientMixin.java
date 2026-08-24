package ir.modernshadow.bladeclient.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void onGetWindowTitle(CallbackInfoReturnable<String> cir) {
        String gameVer = MinecraftClient.getInstance().getGameVersion();
        String modVer = FabricLoader.getInstance()
                .getModContainer("bladeclient")
                .map(m -> m.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
        cir.setReturnValue("BladeClient " + modVer + " (" + gameVer + ")");
    }
}