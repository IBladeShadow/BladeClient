package ir.modernshadow.bladeclient.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.session.Session;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class MicrosoftAuthService {
    private MicrosoftAuthService() {}

    private static final String OAUTH_SCOPE = "XboxLive.signin offline_access";
    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    private static final String MC_ENTITLEMENTS_URL = "https://api.minecraftservices.com/entitlements/mcstore";

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BladeClient-MicrosoftAuth");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static CompletableFuture<LoginResult> loginAsync(String preferredClientId, Consumer<DeviceCode> onDeviceCode, Consumer<String> onStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loginBlocking(preferredClientId, onDeviceCode, onStatus);
            } catch (Throwable t) {
                throw new CompletionException(t);
            }
        }, EXEC);
    }

    private static LoginResult loginBlocking(String preferredClientId, Consumer<DeviceCode> onDeviceCode, Consumer<String> onStatus)
            throws IOException, InterruptedException, AuthException {
        String clientId = resolveClientId(preferredClientId);
        if (clientId == null || clientId.isBlank()) {
            throw new AuthException("Microsoft App ID is empty. Add your Azure app Client ID first.");
        }

        emitStatus(onStatus, "Requesting Microsoft device code...");
        DeviceCode device = requestDeviceCode(clientId);
        if (onDeviceCode != null) {
            onDeviceCode.accept(device);
        }

        emitStatus(onStatus, "Waiting for Microsoft confirmation...");
        OAuthToken oauthToken = pollMicrosoftToken(clientId, device, onStatus);

        emitStatus(onStatus, "Authenticating with Xbox Live...");
        XboxToken xboxToken = authenticateXbox(oauthToken.accessToken);

        emitStatus(onStatus, "Authorizing Xbox security token...");
        XstsToken xstsToken = authorizeXsts(xboxToken.token);

        emitStatus(onStatus, "Logging into Minecraft services...");
        String mcAccessToken = authenticateMinecraft(xstsToken.uhs, xstsToken.token);

        emitStatus(onStatus, "Checking Minecraft ownership...");
        verifyMinecraftOwnership(mcAccessToken);

        emitStatus(onStatus, "Fetching Minecraft profile...");
        MinecraftProfile profile = fetchMinecraftProfile(mcAccessToken);

        Session session = new Session(
                profile.name,
                parseUuid(profile.id),
                mcAccessToken,
                Optional.ofNullable(xboxToken.xuid),
                Optional.of(clientId),
                Session.AccountType.MSA
        );

        emitStatus(onStatus, "Logged in as " + profile.name);
        return new LoginResult(session, profile.name);
    }

    private static DeviceCode requestDeviceCode(String clientId) throws IOException, InterruptedException, AuthException {
        HttpJsonResponse resp = postForm(DEVICE_CODE_URL, Map.of(
                "client_id", clientId,
                "scope", OAUTH_SCOPE
        ));
        if (!isSuccess(resp.statusCode)) {
            throw new AuthException("Microsoft device-code request failed: HTTP " + resp.statusCode + errorSuffix(resp.body));
        }

        JsonObject body = resp.body;
        String deviceCode = requiredString(body, "device_code", "device code");
        String userCode = requiredString(body, "user_code", "user code");
        String verificationUri = requiredString(body, "verification_uri", "verification uri");
        String verificationUriComplete = optionalString(body, "verification_uri_complete");
        String message = optionalString(body, "message");
        int expiresIn = requiredInt(body, "expires_in", "expires_in");
        int interval = Math.max(1, requiredInt(body, "interval", "interval"));
        return new DeviceCode(deviceCode, userCode, verificationUri, verificationUriComplete, message, expiresIn, interval);
    }

    private static OAuthToken pollMicrosoftToken(String clientId, DeviceCode device, Consumer<String> onStatus)
            throws IOException, InterruptedException, AuthException {
        long deadline = System.currentTimeMillis() + (Math.max(10, device.expiresIn) * 1000L);
        int intervalSeconds = Math.max(1, device.interval);

        while (System.currentTimeMillis() < deadline) {
            HttpJsonResponse resp = postForm(TOKEN_URL, Map.of(
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id", clientId,
                    "device_code", device.deviceCode
            ));

            JsonObject body = resp.body;
            if (isSuccess(resp.statusCode) && body.has("access_token")) {
                return new OAuthToken(requiredString(body, "access_token", "access_token"));
            }

            String error = optionalString(body, "error");
            if ("authorization_pending".equalsIgnoreCase(error)) {
                emitStatus(onStatus, "Waiting for confirmation in browser...");
                Thread.sleep(intervalSeconds * 1000L);
                continue;
            }
            if ("slow_down".equalsIgnoreCase(error)) {
                intervalSeconds = Math.min(15, intervalSeconds + 2);
                emitStatus(onStatus, "Microsoft asked to slow down, retrying...");
                Thread.sleep(intervalSeconds * 1000L);
                continue;
            }
            if ("authorization_declined".equalsIgnoreCase(error)) {
                throw new AuthException("Login was denied in browser.");
            }
            if ("expired_token".equalsIgnoreCase(error) || "bad_verification_code".equalsIgnoreCase(error)) {
                throw new AuthException("Login code expired. Please start again.");
            }
            throw new AuthException("Microsoft token request failed: " + readableError(body, "unknown_error"));
        }

        throw new AuthException("Microsoft login timed out.");
    }

    private static XboxToken authenticateXbox(String microsoftAccessToken)
            throws IOException, InterruptedException, AuthException {
        JsonObject props = new JsonObject();
        props.addProperty("AuthMethod", "RPS");
        props.addProperty("SiteName", "user.auth.xboxlive.com");
        props.addProperty("RpsTicket", "d=" + microsoftAccessToken);

        JsonObject payload = new JsonObject();
        payload.add("Properties", props);
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        HttpJsonResponse resp = postJson(XBL_AUTH_URL, payload);
        if (!isSuccess(resp.statusCode)) {
            throw new AuthException("Xbox Live authentication failed: HTTP " + resp.statusCode + errorSuffix(resp.body));
        }

        JsonObject body = resp.body;
        String token = requiredString(body, "Token", "Xbox token");
        JsonObject claims = requiredObject(body, "DisplayClaims", "DisplayClaims");
        JsonArray xui = requiredArray(claims, "xui", "DisplayClaims.xui");
        if (xui.isEmpty() || !xui.get(0).isJsonObject()) {
            throw new AuthException("Xbox Live response missing user claims.");
        }
        JsonObject user = xui.get(0).getAsJsonObject();
        String uhs = requiredString(user, "uhs", "user hash");
        String xuid = optionalString(user, "xid");
        return new XboxToken(token, uhs, xuid);
    }

    private static XstsToken authorizeXsts(String xboxToken)
            throws IOException, InterruptedException, AuthException {
        JsonObject props = new JsonObject();
        props.addProperty("SandboxId", "RETAIL");
        JsonArray userTokens = new JsonArray();
        userTokens.add(xboxToken);
        props.add("UserTokens", userTokens);

        JsonObject payload = new JsonObject();
        payload.add("Properties", props);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        HttpJsonResponse resp = postJson(XSTS_AUTH_URL, payload);
        JsonObject body = resp.body;
        if (!isSuccess(resp.statusCode)) {
            String xErr = optionalString(body, "XErr");
            String xErrMsg = mapXstsError(xErr);
            if (xErrMsg != null) {
                throw new AuthException(xErrMsg);
            }
            throw new AuthException("XSTS authorization failed: HTTP " + resp.statusCode + errorSuffix(body));
        }

        String token = requiredString(body, "Token", "XSTS token");
        JsonObject claims = requiredObject(body, "DisplayClaims", "DisplayClaims");
        JsonArray xui = requiredArray(claims, "xui", "DisplayClaims.xui");
        if (xui.isEmpty() || !xui.get(0).isJsonObject()) {
            throw new AuthException("XSTS response missing user claims.");
        }
        JsonObject user = xui.get(0).getAsJsonObject();
        String uhs = requiredString(user, "uhs", "user hash");
        return new XstsToken(token, uhs);
    }

    private static String authenticateMinecraft(String uhs, String xstsToken)
            throws IOException, InterruptedException, AuthException {
        JsonObject payload = new JsonObject();
        payload.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);

        HttpJsonResponse resp = postJson(MC_AUTH_URL, payload);
        if (!isSuccess(resp.statusCode)) {
            throw new AuthException("Minecraft authentication failed: HTTP " + resp.statusCode + errorSuffix(resp.body));
        }
        return requiredString(resp.body, "access_token", "Minecraft access token");
    }

    private static void verifyMinecraftOwnership(String mcAccessToken)
            throws IOException, InterruptedException, AuthException {
        HttpJsonResponse resp = getJson(MC_ENTITLEMENTS_URL, mcAccessToken);
        if (!isSuccess(resp.statusCode)) {
            throw new AuthException("Could not verify Minecraft ownership: HTTP " + resp.statusCode + errorSuffix(resp.body));
        }
        JsonArray items = requiredArray(resp.body, "items", "entitlements.items");
        if (items.isEmpty()) {
            throw new AuthException("This Microsoft account does not own Minecraft Java Edition.");
        }
    }

    private static MinecraftProfile fetchMinecraftProfile(String mcAccessToken)
            throws IOException, InterruptedException, AuthException {
        HttpJsonResponse resp = getJson(MC_PROFILE_URL, mcAccessToken);
        if (!isSuccess(resp.statusCode)) {
            throw new AuthException("Could not fetch Minecraft profile: HTTP " + resp.statusCode + errorSuffix(resp.body));
        }
        JsonObject body = resp.body;
        String id = requiredString(body, "id", "profile id");
        String name = requiredString(body, "name", "profile name");
        return new MinecraftProfile(id, name);
    }

    private static HttpJsonResponse postForm(String url, Map<String, String> form)
            throws IOException, InterruptedException, AuthException {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (!encoded.isEmpty()) encoded.append('&');
            encoded.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            encoded.append('=');
            encoded.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(encoded.toString()))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(response.statusCode(), parseBody(response.body()));
    }

    private static HttpJsonResponse postJson(String url, JsonObject payload)
            throws IOException, InterruptedException, AuthException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(response.statusCode(), parseBody(response.body()));
    }

    private static HttpJsonResponse getJson(String url, String bearerToken)
            throws IOException, InterruptedException, AuthException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + bearerToken)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResponse(response.statusCode(), parseBody(response.body()));
    }

    private static JsonObject parseBody(String raw) throws AuthException {
        if (raw == null || raw.isBlank()) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
            throw new AuthException("Unexpected JSON response type.");
        } catch (AuthException e) {
            throw e;
        } catch (Throwable t) {
            throw new AuthException("Failed to parse JSON response.");
        }
    }

    private static boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private static String requiredString(JsonObject obj, String key, String label) throws AuthException {
        String val = optionalString(obj, key);
        if (val == null || val.isBlank()) {
            throw new AuthException("Missing " + label + " in authentication response.");
        }
        return val;
    }

    private static int requiredInt(JsonObject obj, String key, String label) throws AuthException {
        if (!obj.has(key)) {
            throw new AuthException("Missing " + label + " in authentication response.");
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Throwable t) {
            throw new AuthException("Invalid " + label + " in authentication response.");
        }
    }

    private static JsonObject requiredObject(JsonObject obj, String key, String label) throws AuthException {
        if (!obj.has(key) || !obj.get(key).isJsonObject()) {
            throw new AuthException("Missing " + label + " in authentication response.");
        }
        return obj.getAsJsonObject(key);
    }

    private static JsonArray requiredArray(JsonObject obj, String key, String label) throws AuthException {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            throw new AuthException("Missing " + label + " in authentication response.");
        }
        return obj.getAsJsonArray(key);
    }

    private static String optionalString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String readableError(JsonObject body, String fallback) {
        String error = optionalString(body, "error");
        String desc = optionalString(body, "error_description");
        if (error == null || error.isBlank()) error = fallback;
        if (desc == null || desc.isBlank()) return error;
        return error + " (" + desc + ")";
    }

    private static String errorSuffix(JsonObject body) {
        if (body == null || body.entrySet().isEmpty()) return "";
        String error = optionalString(body, "error");
        String desc = optionalString(body, "error_description");
        String message = optionalString(body, "message");
        if (error != null && !error.isBlank() && desc != null && !desc.isBlank()) {
            return " - " + error + " (" + desc + ")";
        }
        if (error != null && !error.isBlank()) {
            return " - " + error;
        }
        if (message != null && !message.isBlank()) {
            return " - " + message;
        }
        return "";
    }

    private static String mapXstsError(String xErr) {
        if (xErr == null || xErr.isBlank()) return null;
        return switch (xErr) {
            case "2148916233" -> "This Microsoft account has no Xbox profile. Sign in once on xbox.com and create one.";
            case "2148916235" -> "Xbox Live is unavailable in this account region.";
            case "2148916236", "2148916237", "2148916238" ->
                    "This Xbox account needs adult/family permissions before Minecraft login works.";
            default -> null;
        };
    }

    private static UUID parseUuid(String raw) throws AuthException {
        if (raw == null) {
            throw new AuthException("Minecraft profile returned no UUID.");
        }
        String hex = raw.replace("-", "").trim();
        if (hex.length() != 32) {
            throw new AuthException("Minecraft profile returned an invalid UUID.");
        }
        String withDashes = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-"
                + hex.substring(16, 20) + "-" + hex.substring(20);
        try {
            return UUID.fromString(withDashes);
        } catch (Throwable t) {
            throw new AuthException("Minecraft profile returned an invalid UUID.");
        }
    }

    private static void emitStatus(Consumer<String> onStatus, String message) {
        if (onStatus != null && message != null) {
            onStatus.accept(message);
        }
    }

    private static String resolveClientId(String preferredClientId) {
        if (preferredClientId != null && !preferredClientId.isBlank()) {
            return preferredClientId.trim();
        }
        String sysProp = System.getProperty("bladeclient.microsoft.clientId");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String env = System.getenv("BLADECLIENT_MICROSOFT_CLIENT_ID");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return "";
    }

    public record DeviceCode(
            String deviceCode,
            String userCode,
            String verificationUri,
            String verificationUriComplete,
            String message,
            int expiresIn,
            int interval
    ) {}

    public record LoginResult(Session session, String username) {}

    public static final class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }

    private record OAuthToken(String accessToken) {}
    private record XboxToken(String token, String uhs, String xuid) {}
    private record XstsToken(String token, String uhs) {}
    private record MinecraftProfile(String id, String name) {}
    private record HttpJsonResponse(int statusCode, JsonObject body) {}
}
