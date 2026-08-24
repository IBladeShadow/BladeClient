package ir.modernshadow.bladeclient.skin;

import com.mojang.authlib.GameProfile;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OptifineCapeManager {
    private OptifineCapeManager() {}

    private static final String CAPE_URL = "http://s.optifine.net/capes/";

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BladeClient-OptifineCape");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ConcurrentHashMap<String, Identifier> CAPES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> LOADING = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> NEGATIVE = new ConcurrentHashMap<>();

    public static Identifier getCape(GameProfile profile) {
        if (!isOptifineEnabled()) return null;
        if (profile == null) return null;
        String name = profile.getName();
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);

        Identifier cached = CAPES.get(key);
        if (cached != null) return cached;
        if (NEGATIVE.getOrDefault(key, false)) return null;
        if (LOADING.putIfAbsent(key, true) != null) return null;

        startFetch(key, name);
        return null;
    }

    public static void clearCache() {
        CAPES.clear();
        LOADING.clear();
        NEGATIVE.clear();
    }

    private static boolean isOptifineEnabled() {
        var cfg = ConfigManager.get().skin;
        return cfg.showOptifineCape == null || cfg.showOptifineCape;
    }

    private static void startFetch(String key, String name) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = CAPE_URL + name + ".png";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();
                HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() != 200 || resp.body() == null || resp.body().length == 0) {
                    NEGATIVE.put(key, true);
                    return;
                }
                NativeImage image = NativeImage.read(resp.body());
                if (image == null) {
                    NEGATIVE.put(key, true);
                    return;
                }
                final NativeImage finalImage = normalizeCape(image);
                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null) return;
                client.execute(() -> {
                    Identifier id = Identifier.of("bladeclient", "capes/of_" + key);
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "bladeclient_of_cape", finalImage);
                    client.getTextureManager().registerTexture(id, tex);
                    CAPES.put(key, id);
                });
            } catch (Throwable ignored) {
                NEGATIVE.put(key, true);
            } finally {
                LOADING.remove(key);
            }
        }, EXEC);
    }

    private static NativeImage normalizeCape(NativeImage src) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        if (sw == 64 && sh == 32) {
            return src;
        }
        int cw = 64;
        int ch = 32;
        while (sw > cw || sh > ch) {
            cw *= 2;
            ch *= 2;
        }
        NativeImage out = new NativeImage(cw, ch, true);
        blit(src, out, 0, 0, 0, 0, sw, sh);
        return out;
    }

    private static void blit(NativeImage src, NativeImage dst, int sx, int sy, int dx, int dy, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dst.setColorArgb(dx + x, dy + y, src.getColorArgb(sx + x, sy + y));
            }
        }
    }
}
