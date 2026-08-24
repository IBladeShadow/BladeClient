package ir.modernshadow.bladeclient.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PremiumSkinManager {
    private PremiumSkinManager() {}

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BladeClient-PremiumSkin");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ConcurrentHashMap<String, SkinTextures> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> LOADING = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> NEGATIVE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> LAST_TRY = new ConcurrentHashMap<>();
    private static volatile boolean forceRetry = false;
    private static final long RETRY_INTERVAL_MS = 10_000L;

    public static SkinTextures getPremiumTextures(GameProfile profile) {
        if (!isMojangEnabled()) return null;
        if (profile == null) return null;
        String name = profile.getName();
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);

        SkinTextures cached = CACHE.get(key);
        if (cached != null) {
            maybeRetry(key, name);
            return cached;
        }
        if (NEGATIVE.getOrDefault(key, false)) return null;
        if (LOADING.putIfAbsent(key, true) != null) return null;

        startFetch(key, name);
        return null;
    }

    public static void clearCache() {
        CACHE.clear();
        LOADING.clear();
        NEGATIVE.clear();
        LAST_TRY.clear();
        forceRetry = false;
    }

    public static void requestRetry() {
        NEGATIVE.clear();
        LOADING.clear();
        LAST_TRY.clear();
        forceRetry = true;
    }

    private static boolean isMojangEnabled() {
        var cfg = ConfigManager.get().skin;
        return cfg.showMojangSkins == null || cfg.showMojangSkins;
    }

    private static boolean isVanillaCapeEnabled() {
        var cfg = ConfigManager.get().skin;
        return cfg.showVanillaCape == null || cfg.showVanillaCape;
    }

    private static void startFetch(String key, String name) {
        CompletableFuture.runAsync(() -> {
            try {
                LAST_TRY.put(key, System.currentTimeMillis());
                String uuid = fetchUuid(name);
                if (uuid == null) {
                    NEGATIVE.put(key, true);
                    return;
                }
                SkinInfo info = fetchSkinInfo(uuid);
                if (info == null || info.skinUrl == null) {
                    NEGATIVE.put(key, true);
                    return;
                }

                NativeImage skin = downloadImage(info.skinUrl);
                if (skin == null) {
                    NEGATIVE.put(key, true);
                    return;
                }
                NativeImage cape = isVanillaCapeEnabled() && info.capeUrl != null ? downloadImage(info.capeUrl) : null;
                NativeImage elytra = isVanillaCapeEnabled() && info.elytraUrl != null ? downloadImage(info.elytraUrl) : null;

                final NativeImage finalSkin = skin;
                final NativeImage finalCape = cape != null ? normalizeCape(cape) : null;
                final NativeImage finalElytra = elytra != null ? normalizeCape(elytra) : null;
                final SkinTextures.Model model = resolveModel(info.model);

                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null) return;
                client.execute(() -> {
                    Identifier skinId = Identifier.of("bladeclient", "skins/premium_" + key);
                    NativeImageBackedTexture skinTex = new NativeImageBackedTexture(() -> "bladeclient_premium_skin", finalSkin);
                    client.getTextureManager().registerTexture(skinId, skinTex);

                    Identifier capeId = null;
                    if (finalCape != null) {
                        capeId = Identifier.of("bladeclient", "capes/mc_" + key);
                        NativeImageBackedTexture capeTex = new NativeImageBackedTexture(() -> "bladeclient_premium_cape", finalCape);
                        client.getTextureManager().registerTexture(capeId, capeTex);
                    }

                    Identifier elytraId = null;
                    if (finalElytra != null) {
                        elytraId = Identifier.of("bladeclient", "elytra/mc_" + key);
                        NativeImageBackedTexture elytraTex = new NativeImageBackedTexture(() -> "bladeclient_premium_elytra", finalElytra);
                        client.getTextureManager().registerTexture(elytraId, elytraTex);
                    } else if (capeId != null) {
                        elytraId = capeId;
                    }

                    SkinTextures textures = new SkinTextures(
                            skinId,
                            info.skinUrl,
                            capeId,
                            elytraId,
                            model,
                            false
                    );
                    CACHE.put(key, textures);
                    forceRetry = false;
                });
            } catch (Throwable ignored) {
                NEGATIVE.put(key, true);
            } finally {
                LOADING.remove(key);
            }
        }, EXEC);
    }

    private static void maybeRetry(String key, String name) {
        if (!forceRetry) return;
        long now = System.currentTimeMillis();
        long last = LAST_TRY.getOrDefault(key, 0L);
        if (now - last < RETRY_INTERVAL_MS) return;
        LAST_TRY.put(key, now);
        if (LOADING.putIfAbsent(key, true) != null) return;
        startFetch(key, name);
    }

    private static SkinTextures.Model resolveModel(String metaModel) {
        if (metaModel != null && metaModel.equalsIgnoreCase("slim")) {
            return SkinTextures.Model.SLIM;
        }
        return SkinTextures.Model.WIDE;
    }

    private static String fetchUuid(String username) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;
        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (!obj.has("id")) return null;
        return obj.get("id").getAsString();
    }

    private static SkinInfo fetchSkinInfo(String uuid) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;
        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray props = obj.getAsJsonArray("properties");
        if (props == null || props.isEmpty()) return null;
        JsonObject prop = props.get(0).getAsJsonObject();
        String value = prop.get("value").getAsString();
        String decoded = new String(Base64.getDecoder().decode(value));
        JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject()
                .getAsJsonObject("textures");
        if (textures == null || !textures.has("SKIN")) return null;
        JsonObject skin = textures.getAsJsonObject("SKIN");
        String skinUrl = skin.has("url") ? skin.get("url").getAsString() : null;
        String model = null;
        if (skin.has("metadata")) {
            JsonObject meta = skin.getAsJsonObject("metadata");
            if (meta.has("model")) model = meta.get("model").getAsString();
        }
        String capeUrl = null;
        if (textures.has("CAPE")) {
            JsonObject cape = textures.getAsJsonObject("CAPE");
            if (cape != null && cape.has("url")) capeUrl = cape.get("url").getAsString();
        }
        String elytraUrl = null;
        if (textures.has("ELYTRA")) {
            JsonObject elytra = textures.getAsJsonObject("ELYTRA");
            if (elytra != null && elytra.has("url")) elytraUrl = elytra.get("url").getAsString();
        }
        return new SkinInfo(skinUrl, model, capeUrl, elytraUrl);
    }

    private static NativeImage downloadImage(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) return null;
        return NativeImage.read(resp.body());
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

    private static final class SkinInfo {
        final String skinUrl;
        final String model;
        final String capeUrl;
        final String elytraUrl;

        SkinInfo(String skinUrl, String model, String capeUrl, String elytraUrl) {
            this.skinUrl = skinUrl;
            this.model = model;
            this.capeUrl = capeUrl;
            this.elytraUrl = elytraUrl;
        }
    }
}
