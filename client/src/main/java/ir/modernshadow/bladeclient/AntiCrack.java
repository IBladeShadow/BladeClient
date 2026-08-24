package ir.modernshadow.bladeclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class AntiCrack {
    private AntiCrack() {}

    private static final String LAUNCHER_URL_PROP = "bladeclient.launcher_url";
    private static final String SESSION_ID_PROP = "bladeclient.session_id";
    private static final int HTTP_TIMEOUT_MS = 5000;

    public static boolean verify() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) return true;

        String launcherUrl = System.getProperty(LAUNCHER_URL_PROP);
        String sessionId = System.getProperty(SESSION_ID_PROP);
        if (launcherUrl == null || launcherUrl.isBlank()) return false;
        if (sessionId == null || sessionId.isBlank()) return false;

        try {
            String verifyUrl = launcherUrl.replaceAll("/+$", "") + "/launcher/verify?id=" + sessionId;
            URI uri = URI.create(verifyUrl);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HTTP_TIMEOUT_MS);
            conn.setReadTimeout(HTTP_TIMEOUT_MS);

            int status = conn.getResponseCode();
            if (status != 200) return false;

            try (InputStream is = conn.getInputStream()) {
                String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                return json.has("valid") && json.get("valid").getAsBoolean();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
