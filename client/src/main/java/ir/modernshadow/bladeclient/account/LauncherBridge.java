package ir.modernshadow.bladeclient.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class LauncherBridge {
    private LauncherBridge() {}

    private static final String LAUNCHER_URL_PROP = "bladeclient.launcher_url";

    public static boolean isAvailable() {
        String url = System.getProperty(LAUNCHER_URL_PROP);
        return url != null && !url.isBlank();
    }

    public static CompletableFuture<MicrosoftSession> requestMicrosoftLogin() {
        return CompletableFuture.supplyAsync(() -> {
            String baseUrl = System.getProperty(LAUNCHER_URL_PROP);
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new RuntimeException("Launcher URL not available. Run through the BladeClient launcher.");
            }

            try {
                URL url = URI.create(baseUrl + "/launcher/auth/microsoft").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(0);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write("{}".getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();
                if (status != 200) {
                    String errBody = new String(
                            conn.getErrorStream() != null
                                    ? conn.getErrorStream().readAllBytes()
                                    : new byte[0],
                            StandardCharsets.UTF_8
                    );
                    String msg = "HTTP " + status;
                    try {
                        JsonObject err = JsonParser.parseString(errBody).getAsJsonObject();
                        if (err.has("error")) msg += ": " + err.get("error").getAsString();
                    } catch (Exception ignored) {}
                    throw new RuntimeException(msg);
                }

                String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                if (json.has("ok") && !json.get("ok").getAsBoolean()) {
                    throw new RuntimeException(json.has("error") ? json.get("error").getAsString() : "Login failed");
                }

                String username = json.has("username") ? json.get("username").getAsString() : "";
                String uuid = json.has("uuid") ? json.get("uuid").getAsString() : "";
                String accessToken = json.has("accessToken") ? json.get("accessToken").getAsString() : "";

                if (username.isEmpty()) {
                    throw new RuntimeException("Microsoft login returned empty username");
                }

                return new MicrosoftSession(username, uuid, accessToken);

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Failed to communicate with launcher: " + e.getMessage(), e);
            }
        });
    }

    public record MicrosoftSession(String username, String uuid, String accessToken) {}
}
