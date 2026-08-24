package ir.modernshadow.bladeclient.font;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.resource.ResourcePackManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public final class FontPackManager {
    private FontPackManager() {}

    private static final String PACK_ID = "bladeclient-fonts";
    private static final long APPLY_DELAY_MS = 600L;
    private static String pendingKey = "";
    private static long pendingSince = 0L;
    private static String currentKey = "";
    private static boolean initialized = false;

    public static void tick(MinecraftClient client) {
        if (client == null) return;
        BladeClientConfig.FontPack cfg = ConfigManager.get().fontPack;
        String key = cfg.enabled + "|" + cfg.uiFontPath + "|" + cfg.titleFontPath + "|" + cfg.uiSize + "|" + cfg.titleSize;

        if (!initialized) {
            initialized = true;
            pendingKey = key;
            pendingSince = System.currentTimeMillis();
            boolean shouldBeEnabled = cfg.enabled;
            boolean isEnabled = client.options.resourcePacks.contains(PACK_ID);
            // Avoid unnecessary startup reload; only apply when pack state is actually mismatched.
            if (shouldBeEnabled == isEnabled) {
                currentKey = key;
                return;
            }
            // Force one apply pass to sync state.
            currentKey = "";
        }

        if (!key.equals(pendingKey)) {
            pendingKey = key;
            pendingSince = System.currentTimeMillis();
        }
        if (key.equals(currentKey)) return;
        if (System.currentTimeMillis() - pendingSince < APPLY_DELAY_MS) return;

        try {
            apply(client, cfg);
            currentKey = key;
        } catch (Throwable t) {
            System.err.println("[BladeClient] Font pack apply failed: " + t.getMessage());
        }
    }

    private static void apply(MinecraftClient client, BladeClientConfig.FontPack cfg) throws IOException {
        ResourcePackManager rpm = client.getResourcePackManager();
        GameOptions options = client.options;

        if (!cfg.enabled) {
            disablePack(rpm, options);
            client.reloadResources();
            return;
        }

        Path packRoot = client.getResourcePackDir().resolve(PACK_ID);
        Path assetsFontDir = packRoot.resolve("assets").resolve("bladeclient").resolve("font");
        Files.createDirectories(assetsFontDir);

        writePackMeta(packRoot);

        Path uiFont = resolveFontPath(cfg.uiFontPath);
        Path titleFont = resolveFontPath(cfg.titleFontPath);

        if (uiFont != null && Files.exists(uiFont)) {
            Files.copy(uiFont, assetsFontDir.resolve("ui.ttf"), StandardCopyOption.REPLACE_EXISTING);
        }
        if (titleFont != null && Files.exists(titleFont)) {
            Files.copy(titleFont, assetsFontDir.resolve("title.ttf"), StandardCopyOption.REPLACE_EXISTING);
        }

        writeFontJson(assetsFontDir.resolve("ui.json"), "bladeclient:ui.ttf", cfg.uiSize,
                uiFont != null && Files.exists(uiFont));
        writeFontJson(assetsFontDir.resolve("title.json"), "bladeclient:title.ttf", cfg.titleSize,
                titleFont != null && Files.exists(titleFont));

        rpm.scanPacks();
        if (!rpm.hasProfile(PACK_ID)) {
            System.err.println("[BladeClient] Font pack not detected in resource packs folder.");
        }
        enablePack(rpm, options);
        client.reloadResources();
    }

    private static Path resolveFontPath(String path) {
        if (path == null || path.isBlank()) return null;
        Path p = Path.of(path.trim());
        if (p.isAbsolute()) return p;
        return Path.of("config", "bladeclient", "fonts").resolve(p);
    }

    private static void writePackMeta(Path packRoot) throws IOException {
        String meta = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": " + SharedConstants.RESOURCE_PACK_VERSION + ",\n" +
                "    \"description\": \"BladeClient External Fonts\"\n" +
                "  }\n" +
                "}\n";
        Files.writeString(packRoot.resolve("pack.mcmeta"), meta, StandardCharsets.UTF_8);
    }

    private static void writeFontJson(Path path, String fontId, float size, boolean hasFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"providers\": [\n");
        if (hasFile) {
            sb.append("    {\n");
            sb.append("      \"type\": \"ttf\",\n");
            sb.append("      \"file\": \"").append(fontId).append("\",\n");
            sb.append("      \"shift\": [0, 0],\n");
            sb.append("      \"size\": ").append(String.format(java.util.Locale.ROOT, "%.2f", size)).append(",\n");
            sb.append("      \"oversample\": 8.0\n");
            sb.append("    },\n");
        }
        sb.append("    {\n");
        sb.append("      \"type\": \"reference\",\n");
        sb.append("      \"id\": \"minecraft:default\"\n");
        sb.append("    }\n");
        sb.append("  ]\n}\n");
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void enablePack(ResourcePackManager rpm, GameOptions options) {
        if (!options.resourcePacks.contains(PACK_ID)) {
            options.resourcePacks = new ArrayList<>(options.resourcePacks);
            options.resourcePacks.add(PACK_ID);
        }
        options.incompatibleResourcePacks = new ArrayList<>(options.incompatibleResourcePacks);
        options.incompatibleResourcePacks.remove(PACK_ID);
        rpm.setEnabledProfiles(options.resourcePacks);
        options.write();
    }

    private static void disablePack(ResourcePackManager rpm, GameOptions options) {
        if (options.resourcePacks.contains(PACK_ID)) {
            options.resourcePacks = new ArrayList<>(options.resourcePacks);
            options.resourcePacks.remove(PACK_ID);
        }
        rpm.setEnabledProfiles(options.resourcePacks);
        options.write();
    }
}
