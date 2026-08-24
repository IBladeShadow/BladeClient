package ir.modernshadow.bladeclient.mixin;

import com.mojang.authlib.GameProfile;
import ir.modernshadow.bladeclient.skin.SkinManager;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinProvider.class)
public class PlayerSkinProviderMixin {
    @Inject(method = "getSkinTextures(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/client/util/SkinTextures;)Lnet/minecraft/client/util/SkinTextures;",
            at = @At("HEAD"), cancellable = true)
    private void bladeclient$overrideSkin(GameProfile profile, SkinTextures defaultTextures,
                                          CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures override = SkinManager.getOverride(profile);
        if (override != null) {
            SkinTextures merged = new SkinTextures(
                    override.texture(),
                    override.textureUrl(),
                    defaultTextures == null ? null : defaultTextures.capeTexture(),
                    defaultTextures == null ? null : defaultTextures.elytraTexture(),
                    override.model(),
                    override.secure()
            );
            cir.setReturnValue(merged);
        }
    }
}
