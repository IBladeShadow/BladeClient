package ir.modernshadow.bladeclient.skin;

import com.mojang.authlib.GameProfile;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public final class SkinResolver {
    private SkinResolver() {}

    public static SkinTextures resolve(GameProfile profile, SkinTextures baseDefault) {
        if (profile == null) return baseDefault;
        var cfg = ConfigManager.get().skin;

        SkinTextures base = baseDefault;
        boolean showMojang = cfg.showMojangSkins == null || cfg.showMojangSkins;
        if (!showMojang && isMojangTexture(base)) {
            base = defaultSkinForModel(base != null ? base.model() : null);
        }
        boolean baseIsDefault = isDefaultSkin(base);
        if (showMojang && (base == null || baseIsDefault)) {
            SkinTextures premium = PremiumSkinManager.getPremiumTextures(profile);
            if (premium != null) {
                base = premium;
            }
        }

        SkinTextures override = SkinManager.getOverride(profile);
        if (override != null) {
            Identifier cape = base != null ? base.capeTexture() : null;
            Identifier elytra = base != null ? base.elytraTexture() : null;
            base = new SkinTextures(
                    override.texture(),
                    override.textureUrl(),
                    cape,
                    elytra,
                    override.model(),
                    override.secure()
            );
        }

        if (base == null) return null;

        boolean vanillaCape = cfg.showVanillaCape == null || cfg.showVanillaCape;
        if (!vanillaCape) {
            base = new SkinTextures(
                    base.texture(),
                    base.textureUrl(),
                    null,
                    null,
                    base.model(),
                    base.secure()
            );
        }

        Identifier ofCape = OptifineCapeManager.getCape(profile);
        if (ofCape != null) {
            base = new SkinTextures(
                    base.texture(),
                    base.textureUrl(),
                    ofCape,
                    ofCape,
                    base.model(),
                    base.secure()
            );
        }
        return base;
    }

    private static boolean isDefaultSkin(SkinTextures textures) {
        if (textures == null) return true;
        if (textures.textureUrl() != null && !textures.textureUrl().isBlank()) return false;
        Identifier id = textures.texture();
        if (id == null) return true;
        String path = id.getPath();
        if (path == null) return true;
        if (path.endsWith("textures/entity/steve.png") || path.endsWith("textures/entity/alex.png")) {
            return true;
        }
        // Newer default skins live under textures/entity/player/*
        return path.startsWith("textures/entity/player/");
    }

    private static boolean isMojangTexture(SkinTextures textures) {
        if (textures == null) return false;
        String url = textures.textureUrl();
        if (url != null && url.contains("textures.minecraft.net")) return true;
        Identifier id = textures.texture();
        if (id == null) return false;
        String path = id.getPath();
        return path != null && path.startsWith("skins/");
    }

    private static SkinTextures defaultSkinForModel(SkinTextures.Model model) {
        Identifier id = Identifier.of("minecraft", "textures/entity/steve.png");
        if (model == SkinTextures.Model.SLIM) {
            id = Identifier.of("minecraft", "textures/entity/alex.png");
        }
        return new SkinTextures(id, "", null, null, model == null ? SkinTextures.Model.WIDE : model, false);
    }
}
