package ir.modernshadow.bladeclient.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.function.Supplier;

public final class BladeLogoMask {
    private BladeLogoMask() {}

    private static final Identifier RAW_ICON = Identifier.of("bladeclient", "textures/gui/icon.png");
    private static final Identifier MASKED_ICON = Identifier.of("bladeclient", "masked_icon");
    private static boolean loaded = false;

    public static Identifier iconId(MinecraftClient client) {
        ensureLoaded(client);
        return loaded ? MASKED_ICON : RAW_ICON;
    }

    private static void ensureLoaded(MinecraftClient client) {
        if (loaded || client == null) return;
        try (InputStream in = BladeLogoMask.class.getResourceAsStream("/assets/bladeclient/textures/gui/icon.png")) {
            if (in == null) return;
            NativeImage img = NativeImage.read(in);
            roundImageCornersSoft(img, Math.max(8, Math.min(img.getWidth(), img.getHeight()) / 8), 5);
            Supplier<String> name = () -> "bladeclient/masked_icon";
            NativeImageBackedTexture tex = new NativeImageBackedTexture(name, img);
            client.getTextureManager().registerTexture(MASKED_ICON, tex);
            loaded = true;
        } catch (Exception ignored) {
        }
    }

    private static void roundImageCornersSoft(NativeImage img, int radius, int feather) {
        if (img == null) return;
        int w = img.getWidth();
        int h = img.getHeight();
        int r = Math.max(1, Math.min(radius, Math.min(w, h) / 2));
        int f = Math.max(1, Math.min(feather, r));
        float inner = r - f;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int cx = -1;
                int cy = -1;
                if (x < r && y < r) {
                    cx = r - 1;
                    cy = r - 1;
                } else if (x >= w - r && y < r) {
                    cx = w - r;
                    cy = r - 1;
                } else if (x < r && y >= h - r) {
                    cx = r - 1;
                    cy = h - r;
                } else if (x >= w - r && y >= h - r) {
                    cx = w - r;
                    cy = h - r;
                }
                if (cx == -1) continue;

                float dx = x - cx;
                float dy = y - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= inner) continue;

                int argb = img.getColorArgb(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;

                int out;
                if (dist >= r) {
                    out = argb & 0x00FFFFFF;
                } else {
                    float t = (r - dist) / f;
                    t = Math.max(0.0f, Math.min(1.0f, t));
                    float s = t * t * (3.0f - 2.0f * t); // smoothstep
                    int na = Math.max(0, Math.min(255, Math.round(a * s)));
                    out = (na << 24) | (argb & 0x00FFFFFF);
                }
                img.setColorArgb(x, y, out);
            }
        }
    }
}
