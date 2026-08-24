package ir.modernshadow.bladeclient.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PresenceService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BASE_URL = "https://blade.runflare.run";
    private static final String LAUNCHER_URL = "https://blade.runflare.run";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Set<UUID> REMOTE_UUIDS = ConcurrentHashMap.newKeySet();
    private static final Set<String> REMOTE_NAMES = ConcurrentHashMap.newKeySet();
    private static volatile boolean available = false;
    private static long nextHeartbeatAt = 0L;
    private static long nextFetchAt = 0L;
    private static long nextLogAt = 0L;
    private static long nextLauncherLogAt = 0L;
    private static String cachedVersion = "";
    private static long nextStateAt = 0L;
    private static String lastStateSignature = "";

    private static String lastServerId = "";

    private PresenceService() {}

    public static boolean isAvailable() {
        return available;
    }

    public static boolean hasUser(UUID uuid) {
        return uuid != null && REMOTE_UUIDS.contains(uuid);
    }

    public static boolean hasUserName(String name) {
        return name != null && REMOTE_NAMES.contains(name);
    }

    public static void tick(MinecraftClient client) {
        if (client == null) return;

        long now = System.currentTimeMillis();

        if (now >= nextStateAt || !lastStateSignature.equals(currentStateSignature(client))) {
            nextStateAt = now + 15000L;
            sendLauncherState(client, LAUNCHER_URL);
        }

        String currentServer = currentServerId(client);
        boolean serverChanged = !currentServer.equals(lastServerId);
        if (serverChanged) {
            lastServerId = currentServer;
            available = true;
            nextHeartbeatAt = 0;
            nextFetchAt = 0;
        }

        if (now >= nextHeartbeatAt) {
            nextHeartbeatAt = now + 15000L;
            sendHeartbeat(client, BASE_URL);
        }

        if (now >= nextFetchAt) {
            nextFetchAt = now + 10000L;
            fetchList(client, BASE_URL);
        }
    }

    private static void clear(boolean ok) {
        available = ok;
        REMOTE_UUIDS.clear();
        REMOTE_NAMES.clear();
    }

    private static String currentServerId(MinecraftClient client) {
        if (client == null) return "mainmenu";
        ServerInfo entry = client.getCurrentServerEntry();
        if (entry == null) return "mainmenu";
        if (entry.address == null || entry.address.isBlank()) return "mainmenu";
        return entry.address.toLowerCase();
    }

    private static void sendHeartbeat(MinecraftClient client, String baseUrl) {
        try {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            String uuid = player.getUuid().toString();
            String name = player.getName().getString();
            String serverId = currentServerId(client);

            JsonObject body = new JsonObject();
            body.addProperty("uuid", uuid);
            body.addProperty("name", name);
            body.addProperty("server", serverId);
            body.addProperty("version", getClientVersion());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/presence/heartbeat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(resp -> {
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            available = true;
                        } else {
                            logWarn("heartbeat failed: " + resp.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        clear(false);
                        logWarn("heartbeat error: " + ex.getClass().getSimpleName());
                        return null;
                    });
        } catch (Throwable t) {
            clear(false);
            logWarn("heartbeat error: " + t.getClass().getSimpleName());
        }
    }

    private static void fetchList(MinecraftClient client, String baseUrl) {
        try {
            String serverId = currentServerId(client);
            String query = URLEncoder.encode(serverId, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/presence/list?server=" + query))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                            clear(false);
                            logWarn("fetch failed: " + resp.statusCode());
                            return;
                        }
                        parseList(resp.body());
                        available = true;
                    })
                    .exceptionally(ex -> {
                        clear(false);
                        logWarn("fetch error: " + ex.getClass().getSimpleName());
                        return null;
                    });
        } catch (Throwable t) {
            clear(false);
            logWarn("fetch error: " + t.getClass().getSimpleName());
        }
    }

    private static String currentStateSignature(MinecraftClient client) {
        if (client == null) return "client|";
        ServerInfo entry = client.getCurrentServerEntry();
        if (entry == null || entry.address == null || entry.address.isBlank()) {
            return "client|";
        }
        return "server|" + entry.address.toLowerCase();
    }

    private static void sendLauncherState(MinecraftClient client, String baseUrl) {
        try {
            String status = "client";
            String serverName = "";
            String serverAddress = "";
            String serverIcon = "";
            String serverIconUrl = "";

            if (client != null) {
                ServerInfo entry = client.getCurrentServerEntry();
                if (entry != null && entry.address != null && !entry.address.isBlank()) {
                    status = "server";
                    serverName = entry.name == null ? "" : entry.name;
                    serverAddress = entry.address;
                    serverIcon = extractServerIcon(entry);
                    if (serverIcon.isBlank()) {
                        serverIconUrl = buildServerIconUrl(serverAddress);
                    }
                } else if (client.player != null) {
                    status = "client";
                    serverName = "Singleplayer";
                }
            }

            JsonObject body = new JsonObject();
            body.addProperty("source", "client");
            body.addProperty("status", status);
            if (!serverName.isBlank()) body.addProperty("serverName", serverName);
            if (!serverAddress.isBlank()) body.addProperty("serverAddress", serverAddress);
            if (!serverIcon.isBlank()) body.addProperty("serverIcon", serverIcon);
            if (!serverIconUrl.isBlank()) body.addProperty("serverIconUrl", serverIconUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/launcher/state"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(resp -> {
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            lastStateSignature = currentStateSignature(client);
                        } else {
                            logLauncher("launcher state failed: " + resp.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        logLauncher("launcher state error: " + ex.getClass().getSimpleName());
                        return null;
                    });
        } catch (Throwable t) {
            logLauncher("launcher state error: " + t.getClass().getSimpleName());
        }
    }

    private static String buildServerIconUrl(String address) {
        if (address == null || address.isBlank()) return "";
        String host = address.trim();
        int idx = host.indexOf('/');
        if (idx > 0) host = host.substring(0, idx);
        return "https://api.mcsrvstat.us/icon/" + host;
    }

    private static String extractServerIcon(ServerInfo entry) {
        if (entry == null) return "";
        try {
            Method m = entry.getClass().getMethod("getFavicon");
            Object res = m.invoke(entry);
            if (res instanceof byte[]) {
                String b64 = Base64.getEncoder().encodeToString((byte[]) res);
                return b64.isBlank() ? "" : ("data:image/png;base64," + b64);
            }
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}

        try {
            Method m = entry.getClass().getMethod("getIcon");
            Object res = m.invoke(entry);
            if (res instanceof byte[]) {
                String b64 = Base64.getEncoder().encodeToString((byte[]) res);
                return b64.isBlank() ? "" : ("data:image/png;base64," + b64);
            }
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}

        try {
            Field f = entry.getClass().getDeclaredField("favicon");
            f.setAccessible(true);
            Object res = f.get(entry);
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}

        try {
            Field f = entry.getClass().getDeclaredField("icon");
            f.setAccessible(true);
            Object res = f.get(entry);
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}

        return "";
    }

    private static void logWarn(String msg) {
        long now = System.currentTimeMillis();
        if (now < nextLogAt) return;
        nextLogAt = now + 5000L;
        LOGGER.warn("[BladeClient Presence] {}", msg);
    }

    private static void logLauncher(String msg) {
        long now = System.currentTimeMillis();
        if (now < nextLauncherLogAt) return;
        nextLauncherLogAt = now + 5000L;
        LOGGER.info("[BladeClient Presence] {}", msg);
    }

    private static void parseList(String body) {
        try {
            JsonElement root = JsonParser.parseString(body);
            Set<UUID> uuids = new HashSet<>();
            Set<String> names = new HashSet<>();

            if (root.isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray()) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.has("uuid")) {
                            try { uuids.add(UUID.fromString(obj.get("uuid").getAsString())); } catch (Throwable ignored) {}
                        }
                        if (obj.has("name")) {
                            names.add(obj.get("name").getAsString());
                        }
                    } else if (el.isJsonPrimitive()) {
                        String v = el.getAsString();
                        try { uuids.add(UUID.fromString(v)); } catch (Throwable ignored) { names.add(v); }
                    }
                }
            }

            REMOTE_UUIDS.clear();
            REMOTE_UUIDS.addAll(uuids);
            REMOTE_NAMES.clear();
            REMOTE_NAMES.addAll(names);
        } catch (Throwable t) {
            clear(false);
        }
    }

    private static String getClientVersion() {
        if (!cachedVersion.isBlank()) return cachedVersion;
        try {
            ModContainer mod = FabricLoader.getInstance().getModContainer("bladeclient").orElse(null);
            if (mod != null) {
                cachedVersion = mod.getMetadata().getVersion().getFriendlyString();
                return cachedVersion;
            }
        } catch (Throwable ignored) {}
        cachedVersion = "unknown";
        return cachedVersion;
    }
}
