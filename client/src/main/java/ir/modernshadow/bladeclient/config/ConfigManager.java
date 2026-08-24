package ir.modernshadow.bladeclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigManager {
    private ConfigManager() {}

    private static final Path CONFIG_PATH = Paths.get("config", "bladeclient.json");
    private static final Path LEGACY_PATH = Paths.get("config", "bladeclient.cfg");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static BladeClientConfig config = new BladeClientConfig();

    public static BladeClientConfig get() {
        return config;
    }

    public static synchronized void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                if (Files.exists(LEGACY_PATH)) {
                    if (migrateLegacyConfig()) {
                        return;
                    }
                }
                Files.createDirectories(CONFIG_PATH.getParent());
                save();
                return;
            }
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            BladeClientConfig loaded = GSON.fromJson(json, BladeClientConfig.class);
            if (loaded != null) {
                config = loaded;
            }
            sanitize();
        } catch (Throwable t) {
            System.err.println("[BladeClient] Failed to load config: " + t.getMessage());
            sanitize();
            try { save(); } catch (IOException ignore) {}
        }
    }

    public static synchronized void save() throws IOException {
        sanitize();
        Files.createDirectories(CONFIG_PATH.getParent());
        String json = GSON.toJson(config);
        Files.writeString(CONFIG_PATH, json, StandardCharsets.UTF_8);
    }

    public static void migratePositions(int screenW, int screenH) {
        if (config.configVersion >= 1 || screenW <= 0 || screenH <= 0) return;
        config.fps.posX = (float) config.fps.x / screenW;
        config.fps.posY = (float) config.fps.y / screenH;
        config.keystrokes.posX = (float) config.keystrokes.x / screenW;
        config.keystrokes.posY = (float) config.keystrokes.y / screenH;
        config.ping.posX = (float) config.ping.x / screenW;
        config.ping.posY = (float) config.ping.y / screenH;
        config.cps.posX = (float) config.cps.x / screenW;
        config.cps.posY = (float) config.cps.y / screenH;
        config.armor.posX = (float) config.armor.x / screenW;
        config.armor.posY = (float) config.armor.y / screenH;
        config.potion.posX = (float) config.potion.x / screenW;
        config.potion.posY = (float) config.potion.y / screenH;
        config.appleSkin.posX = (float) config.appleSkin.x / screenW;
        config.appleSkin.posY = (float) config.appleSkin.y / screenH;
        config.coords.posX = (float) config.coords.x / screenW;
        config.coords.posY = (float) config.coords.y / screenH;
        config.direction.posX = (float) config.direction.x / screenW;
        config.direction.posY = (float) config.direction.y / screenH;
        config.toggleSprint.posX = (float) config.toggleSprint.x / screenW;
        config.toggleSprint.posY = (float) config.toggleSprint.y / screenH;
        config.configVersion = 1;
        saveQuiet();
    }

    public static void saveQuiet() {
        try {
            save();
        } catch (IOException e) {
            System.err.println("[BladeClient] Failed to save config: " + e.getMessage());
        }
    }

    private static void sanitize() {
        if (config == null) {
            config = new BladeClientConfig();
        }
        if (config.fps == null) config.fps = new BladeClientConfig.Fps();
        if (config.keystrokes == null) config.keystrokes = new BladeClientConfig.Keystrokes();
        if (config.ping == null) config.ping = new BladeClientConfig.Ping();
        if (config.cps == null) config.cps = new BladeClientConfig.Cps();
        if (config.armor == null) config.armor = new BladeClientConfig.Armor();
        if (config.potion == null) config.potion = new BladeClientConfig.Potion();
        if (config.appleSkin == null) config.appleSkin = new BladeClientConfig.AppleSkin();
        if (config.coords == null) config.coords = new BladeClientConfig.Coords();
        if (config.direction == null) config.direction = new BladeClientConfig.Direction();
        if (config.crosshair == null) config.crosshair = new BladeClientConfig.Crosshair();
        if (config.zoom == null) config.zoom = new BladeClientConfig.Zoom();
        if (config.freeLook == null) config.freeLook = new BladeClientConfig.FreeLook();
        if (config.nightVision == null) config.nightVision = new BladeClientConfig.NightVision();
        if (config.saturation == null) config.saturation = new BladeClientConfig.Saturation();
        if (config.motionBlur == null) config.motionBlur = new BladeClientConfig.MotionBlur();
        if (config.timeChanger == null) config.timeChanger = new BladeClientConfig.TimeChanger();
        if (config.miniFov == null) config.miniFov = new BladeClientConfig.MiniFov();
        if (config.fontPack == null) config.fontPack = new BladeClientConfig.FontPack();
        if (config.toggleSprint == null) config.toggleSprint = new BladeClientConfig.ToggleSprint();
        if (config.autoText == null) config.autoText = new BladeClientConfig.AutoText();
        if (config.skin == null) config.skin = new BladeClientConfig.SkinChanger();
        if (config.ui == null) config.ui = new BladeClientConfig.Ui();
        if (config.account == null) config.account = new BladeClientConfig.Account();

        config.fps.scale = clamp(config.fps.scale, 0.5f, 3.0f);
        config.fps.backgroundOpacity = clamp(config.fps.backgroundOpacity, 0.15f, 0.9f);
        config.keystrokes.scale = clamp(config.keystrokes.scale, 0.5f, 3.0f);
        config.ping.scale = clamp(config.ping.scale, 0.5f, 3.0f);
        config.ping.backgroundOpacity = clamp(config.ping.backgroundOpacity, 0.15f, 0.9f);
        config.cps.scale = clamp(config.cps.scale, 0.5f, 3.0f);
        config.cps.backgroundOpacity = clamp(config.cps.backgroundOpacity, 0.15f, 0.9f);
        config.armor.scale = clamp(config.armor.scale, 0.5f, 3.0f);
        if (config.armor.layout == null) config.armor.layout = BladeClientConfig.ArmorLayout.HORIZONTAL;
        config.potion.scale = clamp(config.potion.scale, 0.5f, 3.0f);
        config.potion.backgroundOpacity = clamp(config.potion.backgroundOpacity, 0.15f, 0.9f);
        config.appleSkin.scale = clamp(config.appleSkin.scale, 0.5f, 3.0f);
        config.appleSkin.backgroundOpacity = clamp(config.appleSkin.backgroundOpacity, 0.15f, 0.9f);
        config.coords.scale = clamp(config.coords.scale, 0.5f, 3.0f);
        config.coords.backgroundOpacity = clamp(config.coords.backgroundOpacity, 0.15f, 0.9f);
        config.direction.scale = clamp(config.direction.scale, 0.5f, 3.0f);
        config.direction.backgroundOpacity = clamp(config.direction.backgroundOpacity, 0.15f, 0.9f);
        config.toggleSprint.scale = clamp(config.toggleSprint.scale, 0.5f, 3.0f);
        config.toggleSprint.backgroundOpacity = clamp(config.toggleSprint.backgroundOpacity, 0.15f, 0.9f);
        if (config.armor.showBackground == null) config.armor.showBackground = true;
        config.autoText.intervalSeconds = clampInt(config.autoText.intervalSeconds, 0, 300);
        config.crosshair.size = clampInt(config.crosshair.size, 2, 24);
        config.crosshair.gap = clampInt(config.crosshair.gap, 0, 10);
        config.crosshair.thickness = clampInt(config.crosshair.thickness, 1, 5);
        config.crosshair.opacity = clamp(config.crosshair.opacity, 0.2f, 1.0f);
        config.crosshair.customHue = clampInt(config.crosshair.customHue, 0, 360);
        config.crosshair.customSaturation = clampInt(config.crosshair.customSaturation, 0, 100);
        config.crosshair.customValue = clampInt(config.crosshair.customValue, 0, 100);
        if (config.crosshair.customPixels == null || config.crosshair.customPixels.length != 16) {
            config.crosshair.customPixels = defaultCrosshairPixels();
        }
        config.zoom.zoom = clamp(config.zoom.zoom, 2.0f, 50.0f);
        if (config.zoom.smoothLook == null) config.zoom.smoothLook = true;
        config.saturation.amount = clamp(config.saturation.amount, 0.0f, 2.0f);
        config.motionBlur.strength = clamp(config.motionBlur.strength, 0.0f, 0.9f);
        config.timeChanger.time = clampInt(config.timeChanger.time, 0, 23999);
        config.miniFov.fov = clamp(config.miniFov.fov, 30.0f, 120.0f);
        config.fontPack.uiSize = clamp(config.fontPack.uiSize, 8.0f, 24.0f);
        config.fontPack.titleSize = clamp(config.fontPack.titleSize, 14.0f, 48.0f);
        if (config.fontPack.uiFontPath == null) config.fontPack.uiFontPath = "";
        if (config.fontPack.titleFontPath == null) config.fontPack.titleFontPath = "";
        if (config.autoText.message == null) config.autoText.message = "";
        if (config.toggleSprint.mode == null) config.toggleSprint.mode = BladeClientConfig.SprintMode.TOGGLE;
        if (config.crosshair.style == null) config.crosshair.style = BladeClientConfig.CrosshairStyle.PLUS;
        if (config.crosshair.color == null) config.crosshair.color = BladeClientConfig.CrosshairColor.NEON_BLUE;
        if (config.skin.mode == null) config.skin.mode = BladeClientConfig.SkinMode.FILE;
        if (config.skin.model == null) config.skin.model = BladeClientConfig.SkinModel.AUTO;
        if (config.skin.showMojangSkins == null) config.skin.showMojangSkins = true;
        if (config.skin.showVanillaCape == null) config.skin.showVanillaCape = true;
        if (config.skin.showOptifineCape == null) config.skin.showOptifineCape = true;
        if (config.skin.presets == null) config.skin.presets = new java.util.ArrayList<>();
        if (config.account.offlineName == null) config.account.offlineName = "";
        if (config.account.offlineAccounts == null) config.account.offlineAccounts = new java.util.ArrayList<>();
        if (config.account.microsoftClientId == null) config.account.microsoftClientId = "";
        sanitizeOfflineAccounts();
        sanitizeAutoText();
        for (BladeClientConfig.SkinPreset preset : config.skin.presets) {
            if (preset == null) continue;
            if (preset.mode == null) preset.mode = BladeClientConfig.SkinMode.FILE;
            if (preset.model == null) preset.model = BladeClientConfig.SkinModel.AUTO;
            if (preset.filePath == null) preset.filePath = "";
            if (preset.username == null) preset.username = "";
            if (preset.name == null) preset.name = "";
        }

    }

    private static int[] defaultCrosshairPixels() {
        int[] rows = new int[16];
        int mid = 8;
        for (int i = 0; i < 16; i++) {
            rows[mid] |= (1 << i);
            rows[i] |= (1 << mid);
        }
        return rows;
    }

    private static void sanitizeAutoText() {
        if (config.autoText.entries == null) {
            config.autoText.entries = new java.util.ArrayList<>();
        }

        // Migrate legacy single message into list if list is empty
        if (config.autoText.entries.isEmpty()) {
            BladeClientConfig.AutoTextEntry entry = new BladeClientConfig.AutoTextEntry();
            entry.message = config.autoText.message == null ? "" : config.autoText.message;
            entry.intervalSeconds = clampInt(config.autoText.intervalSeconds, 1, 300);
            config.autoText.entries.add(entry);
        }

        java.util.ArrayList<BladeClientConfig.AutoTextEntry> cleaned = new java.util.ArrayList<>();
        for (BladeClientConfig.AutoTextEntry entry : config.autoText.entries) {
            if (entry == null) continue;
            if (entry.message == null) entry.message = "";
            entry.intervalSeconds = clampInt(entry.intervalSeconds, 0, 300);
            if (entry.keyType < 0 || entry.keyType > 2) {
                entry.keyType = 0;
            }
            cleaned.add(entry);
        }

        if (cleaned.isEmpty()) {
            cleaned.add(new BladeClientConfig.AutoTextEntry());
        }
        config.autoText.entries = cleaned;
    }

    private static boolean migrateLegacyConfig() {
        try {
            config = new BladeClientConfig();
            for (String raw : Files.readAllLines(LEGACY_PATH, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                applyLegacy(key, val);
            }
            sanitize();
            save();
            return true;
        } catch (Throwable t) {
            System.err.println("[BladeClient] Failed to migrate legacy config: " + t.getMessage());
            return false;
        }
    }

    private static void applyLegacy(String key, String val) {
        switch (key) {
            case "showFps" -> config.fps.enabled = parseBool(val, config.fps.enabled);
            case "colorChangeEnabled" -> config.fps.colorByFps = parseBool(val, config.fps.colorByFps);
            case "fpsPosX" -> config.fps.x = parseInt(val, config.fps.x);
            case "fpsPosY" -> config.fps.y = parseInt(val, config.fps.y);
            case "fpsScale" -> config.fps.scale = parseFloat(val, config.fps.scale);

            case "keystrokesEnabled" -> config.keystrokes.enabled = parseBool(val, config.keystrokes.enabled);
            case "keystrokesPosX" -> config.keystrokes.x = parseInt(val, config.keystrokes.x);
            case "keystrokesPosY" -> config.keystrokes.y = parseInt(val, config.keystrokes.y);
            case "keystrokesScale" -> config.keystrokes.scale = parseFloat(val, config.keystrokes.scale);
            case "keystrokesShowBackground" -> config.keystrokes.showBackground = parseBool(val, config.keystrokes.showBackground);
            case "keystrokesShowSpace" -> config.keystrokes.showSpace = parseBool(val, config.keystrokes.showSpace);
            case "keystrokesShowMouse" -> config.keystrokes.showMouse = parseBool(val, config.keystrokes.showMouse);

            case "showPing" -> config.ping.enabled = parseBool(val, config.ping.enabled);
            case "pingPosX" -> config.ping.x = parseInt(val, config.ping.x);
            case "pingPosY" -> config.ping.y = parseInt(val, config.ping.y);
            case "pingScale" -> config.ping.scale = parseFloat(val, config.ping.scale);
            case "pingColor" -> config.ping.color = parseColor(val, config.ping.color);
            case "pingColoringEnabled" -> config.ping.colorByLatency = parseBool(val, config.ping.colorByLatency);
            case "toggleSprintEnabled" -> config.toggleSprint.enabled = parseBool(val, config.toggleSprint.enabled);
            case "toggleSprintActive" -> config.toggleSprint.active = parseBool(val, config.toggleSprint.active);
            case "toggleSprintPosX" -> config.toggleSprint.x = parseInt(val, config.toggleSprint.x);
            case "toggleSprintPosY" -> config.toggleSprint.y = parseInt(val, config.toggleSprint.y);
            case "toggleSprintScale" -> config.toggleSprint.scale = parseFloat(val, config.toggleSprint.scale);
            case "sprintMode" -> {
                try { config.toggleSprint.mode = BladeClientConfig.SprintMode.valueOf(val.trim().toUpperCase()); }
                catch (Throwable ignored) {}
            }

            default -> {}
        }
    }

    private static boolean parseBool(String s, boolean def) {
        if (s == null) return def;
        return Boolean.parseBoolean(s.trim());
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (Throwable ignored) { return def; }
    }

    private static float parseFloat(String s, float def) {
        if (s == null) return def;
        try { return Float.parseFloat(s.trim()); } catch (Throwable ignored) { return def; }
    }

    private static int parseColor(String s, int def) {
        if (s == null) return def;
        String v = s.trim();
        try {
            if (v.startsWith("#")) return (int) Long.parseLong(v.substring(1), 16);
            if (v.startsWith("0x") || v.startsWith("0X")) return (int) Long.parseLong(v.substring(2), 16);
            return Integer.parseInt(v);
        } catch (Throwable ignored) {
            return def;
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static void sanitizeOfflineAccounts() {
        String current = config.account.offlineName == null ? "" : config.account.offlineName.trim();
        if (current.isEmpty()) {
            current = "";
        }

        java.util.ArrayList<String> cleaned = new java.util.ArrayList<>();
        for (String raw : config.account.offlineAccounts) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty()) continue;
            if (findIgnoreCase(cleaned, name) == null) {
                cleaned.add(name);
            }
        }

        if (!current.isEmpty()) {
            String existing = findIgnoreCase(cleaned, current);
            if (existing != null) {
                current = existing;
            } else {
                cleaned.add(current);
            }
        }

        config.account.offlineName = current;
        config.account.offlineAccounts = cleaned;
    }

    private static String findIgnoreCase(java.util.List<String> list, String name) {
        if (name == null) return null;
        for (String entry : list) {
            if (entry != null && entry.equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }
}
