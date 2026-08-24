package ir.modernshadow.bladeclient.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SkinManager {
    private SkinManager() {}

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BladeClient-Skin");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static volatile SkinTextures override;
    private static volatile SkinTextures preview;
    private static volatile String currentKey = "";
    private static volatile String previewKey = "";
    private static volatile boolean loading = false;
    private static volatile boolean previewLoading = false;
    private static volatile int lastAppliedNonce = -1;

    public static void tick(MinecraftClient client) {
        BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;
        if (!cfg.enabled) {
            override = null;
            currentKey = "";
            loading = false;
            lastAppliedNonce = cfg.applyNonce;
            return;
        }
        if (cfg.applyNonce == lastAppliedNonce) return;
        lastAppliedNonce = cfg.applyNonce;
        startOverrideLoad(client, cfg);
    }

    public static void applyNow(MinecraftClient client) {
        BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;
        cfg.enabled = true;
        cfg.applyNonce++;
        ConfigManager.saveQuiet();
        lastAppliedNonce = cfg.applyNonce;
        startOverrideLoad(client, cfg);
    }

    public static void requestPreview(MinecraftClient client) {
        BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;
        String key = buildKey(cfg);
        if (key.equals(previewKey) && preview != null) return;
        if (previewLoading && key.equals(previewKey)) return;
        previewKey = key;
        previewLoading = true;
        if (cfg.mode == BladeClientConfig.SkinMode.FILE) {
            Path path = resolvePath(cfg.filePath);
            loadFromFileAsync(client, path, cfg.model, true);
        } else {
            loadFromUsernameAsync(client, cfg.username, cfg.model, true);
        }
    }

    public static SkinTextures getPreviewOrOverride(GameProfile profile, SkinTextures fallback) {
        if (preview != null) return preview;
        SkinTextures over = getOverride(profile);
        return over != null ? over : fallback;
    }

    public static SkinTextures getOverride(GameProfile profile) {
        BladeClientConfig.SkinChanger cfg = ConfigManager.get().skin;
        if (!cfg.enabled) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return null;
        if (profile == null) return null;
        var session = mc.getSession();
        if (session == null) return null;
        if (session.getUuidOrNull() != null && profile.getId() != null) {
            if (Objects.equals(session.getUuidOrNull(), profile.getId())) return override;
        }
        if (session.getUsername() != null && profile.getName() != null) {
            if (session.getUsername().equalsIgnoreCase(profile.getName())) return override;
        }
        return null;
    }

    private static Path resolvePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        Path p = Paths.get(filePath.trim());
        if (p.isAbsolute()) return p;
        return Paths.get("config", "bladeclient", "skins").resolve(p);
    }

    private static void loadFromFileAsync(MinecraftClient client, Path path, BladeClientConfig.SkinModel model, boolean isPreview) {
        CompletableFuture.runAsync(() -> {
            try {
                if (path == null || !Files.exists(path)) return;
                try (InputStream in = Files.newInputStream(path)) {
                    NativeImage image = NativeImage.read(in);
                    SkinTextures.Model resolved = resolveModel(model, null);
                    registerTexture(client, image, "file:" + path.toString(), resolved, isPreview);
                }
            } catch (Throwable ignored) {
            } finally {
                if (isPreview) {
                    previewLoading = false;
                } else {
                    loading = false;
                }
            }
        }, EXEC);
    }

    private static void loadFromUsernameAsync(MinecraftClient client, String username, BladeClientConfig.SkinModel model, boolean isPreview) {
        CompletableFuture.runAsync(() -> {
            try {
                String name = username == null ? "" : username.trim();
                if (name.isEmpty()) return;

                String cacheKey = cacheKeyForUsername(name);
                if (cacheKey != null && loadFromCache(client, cacheKey, model, isPreview)) {
                    return;
                }

                String uuid = fetchUuid(name);
                if (uuid == null) return;

                SkinInfo info = fetchSkinInfo(uuid);
                if (info == null || info.url == null) return;

                NativeImage image = downloadImage(info.url);
                if (image == null) return;

                SkinTextures.Model resolved = resolveModel(model, info.model);
                if (cacheKey != null) {
                    saveCache(cacheKey, image, info.model);
                }
                registerTexture(client, image, info.url, resolved, isPreview);
            } catch (Throwable ignored) {
            } finally {
                if (isPreview) {
                    previewLoading = false;
                } else {
                    loading = false;
                }
            }
        }, EXEC);
    }

    private static void registerTexture(MinecraftClient client, NativeImage image, String url, SkinTextures.Model model, boolean isPreview) {
        if (client == null || image == null) return;
        client.execute(() -> {
            Identifier id = Identifier.of("bladeclient", isPreview ? "skins/preview" : "skins/custom");
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "bladeclient_skin", image);
            client.getTextureManager().registerTexture(id, texture);
            SkinTextures textures = new SkinTextures(id, url == null ? "" : url, null, null, model, false);
            if (isPreview) {
                preview = textures;
            } else {
                override = textures;
            }
        });
    }

    private static SkinTextures.Model resolveModel(BladeClientConfig.SkinModel cfgModel, String metaModel) {
        if (cfgModel == BladeClientConfig.SkinModel.SLIM) return SkinTextures.Model.SLIM;
        if (cfgModel == BladeClientConfig.SkinModel.WIDE) return SkinTextures.Model.WIDE;
        if (metaModel != null && metaModel.equalsIgnoreCase("slim")) return SkinTextures.Model.SLIM;
        return SkinTextures.Model.WIDE;
    }

    private static void startOverrideLoad(MinecraftClient client, BladeClientConfig.SkinChanger cfg) {
        String key = buildKey(cfg);
        if (key.equals(currentKey) && override != null) return;
        if (loading) return;
        currentKey = key;
        loading = true;
        if (preview != null && previewKey.equals(key)) {
            override = preview;
            loading = false;
            return;
        }
        if (cfg.mode == BladeClientConfig.SkinMode.FILE) {
            Path path = resolvePath(cfg.filePath);
            loadFromFileAsync(client, path, cfg.model, false);
        } else {
            loadFromUsernameAsync(client, cfg.username, cfg.model, false);
        }
    }

    private static String buildKey(BladeClientConfig.SkinChanger cfg) {
        return cfg.mode + "|" + cfg.filePath + "|" + cfg.username + "|" + cfg.model;
    }

    private static String cacheKeyForUsername(String username) {
        if (username == null) return null;
        String raw = username.trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) return null;
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static Path cacheDir() {
        return Paths.get("config", "bladeclient", "skins", "cache");
    }

    private static Path cacheImagePath(String key) {
        return cacheDir().resolve(key + ".png");
    }

    private static Path cacheMetaPath(String key) {
        return cacheDir().resolve(key + ".meta");
    }

    private static boolean loadFromCache(MinecraftClient client, String cacheKey, BladeClientConfig.SkinModel model, boolean isPreview) {
        try {
            Path imagePath = cacheImagePath(cacheKey);
            if (!Files.exists(imagePath)) return false;
            String metaModel = readCacheModel(cacheKey);
            try (InputStream in = Files.newInputStream(imagePath)) {
                NativeImage image = NativeImage.read(in);
                SkinTextures.Model resolved = resolveModel(model, metaModel);
                registerTexture(client, image, "cache:" + cacheKey, resolved, isPreview);
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void saveCache(String cacheKey, NativeImage image, String model) {
        try {
            Files.createDirectories(cacheDir());
            Path imagePath = cacheImagePath(cacheKey);
            image.writeTo(imagePath);
            if (model != null && !model.isBlank()) {
                Files.writeString(cacheMetaPath(cacheKey), model.trim());
            }
        } catch (Throwable ignored) {
        }
    }

    private static String readCacheModel(String cacheKey) {
        try {
            Path meta = cacheMetaPath(cacheKey);
            if (!Files.exists(meta)) return null;
            String val = Files.readString(meta).trim();
            return val.isEmpty() ? null : val;
        } catch (Throwable ignored) {
            return null;
        }
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
        String url = skin.has("url") ? skin.get("url").getAsString() : null;
        String model = null;
        if (skin.has("metadata")) {
            JsonObject meta = skin.getAsJsonObject("metadata");
            if (meta.has("model")) model = meta.get("model").getAsString();
        }
        return new SkinInfo(url, model);
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

    private static final class SkinInfo {
        final String url;
        final String model;

        SkinInfo(String url, String model) {
            this.url = url;
            this.model = model;
        }
    }
}
