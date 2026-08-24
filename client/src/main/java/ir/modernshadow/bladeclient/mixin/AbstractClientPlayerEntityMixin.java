package ir.modernshadow.bladeclient.mixin;

import ir.modernshadow.bladeclient.skin.SkinResolver;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures()Lnet/minecraft/client/util/SkinTextures;",
            at = @At("RETURN"), cancellable = true)
    private void bladeclient$overrideSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        SkinTextures base = cir.getReturnValue();
        SkinTextures resolved = SkinResolver.resolve(self.getGameProfile(), base);
        if (resolved != null) {
            cir.setReturnValue(resolved);
        }
    }
}
