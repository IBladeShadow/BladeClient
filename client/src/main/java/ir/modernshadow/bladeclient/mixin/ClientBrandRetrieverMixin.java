package ir.modernshadow.bladeclient.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class ClientBrandRetrieverMixin {
    private static final String BLADECLIENT_BRAND = "BladeClient";

    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true)
    private static void bladeclient$brand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(BLADECLIENT_BRAND);
    }
}
