package ir.modernshadow.bladeclient.module;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.module.setting.BaseSetting;
import ir.modernshadow.bladeclient.module.setting.BooleanSetting;
import ir.modernshadow.bladeclient.module.setting.EnumSetting;
import ir.modernshadow.bladeclient.module.setting.FloatSetting;
import ir.modernshadow.bladeclient.module.setting.IntSetting;
import ir.modernshadow.bladeclient.module.setting.KeybindSetting;
import ir.modernshadow.bladeclient.module.setting.StringSetting;
import ir.modernshadow.bladeclient.freelook.FreeLookModule;
import ir.modernshadow.bladeclient.zoom.ZoomModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ModuleManager {
    private ModuleManager() {}

    private static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.clear();
        BladeClientConfig cfg = ConfigManager.get();

        // HUD
        modules.add(module("fps", "FPS", "Show frames per second", ModuleCategory.HUD,
                bool("Enabled", "Show the FPS counter", () -> cfg.fps.enabled, v -> cfg.fps.enabled = v),
                List.of(
                        bool("Color by FPS", "Colorize FPS by performance", () -> cfg.fps.colorByFps, v -> cfg.fps.colorByFps = v),
                        bool("Label", "Show the FPS label", () -> cfg.fps.showLabel, v -> cfg.fps.showLabel = v),
                        bool("Background", "Draw a panel behind text", () -> cfg.fps.showBackground, v -> cfg.fps.showBackground = v),
                        fsetting("BG Opacity", "Background opacity", 0.15f, 0.9f, 0.05f, () -> cfg.fps.backgroundOpacity, v -> cfg.fps.backgroundOpacity = v),
                        fsetting("Scale", "Size of the FPS text", 0.5f, 3.0f, 0.05f, () -> cfg.fps.scale, v -> cfg.fps.scale = v)
                )));

        modules.add(module("keystrokes", "Keystrokes", "WASD and mouse buttons", ModuleCategory.HUD,
                bool("Enabled", "Show keystrokes HUD", () -> cfg.keystrokes.enabled, v -> cfg.keystrokes.enabled = v),
                List.of(
                        bool("Show Space", "Show space bar", () -> cfg.keystrokes.showSpace, v -> cfg.keystrokes.showSpace = v),
                        bool("Show Mouse", "Show mouse buttons and CPS", () -> cfg.keystrokes.showMouse, v -> cfg.keystrokes.showMouse = v),
                        bool("Show CPS", "Show CPS numbers", () -> cfg.keystrokes.showCps, v -> cfg.keystrokes.showCps = v),
                        fsetting("Scale", "Overall size", 0.5f, 3.0f, 0.05f, () -> cfg.keystrokes.scale, v -> cfg.keystrokes.scale = v)
                )));

        modules.add(module("ping", "Ping", "Show latency in ms", ModuleCategory.HUD,
                bool("Enabled", "Show ping HUD", () -> cfg.ping.enabled, v -> cfg.ping.enabled = v),
                List.of(
                        bool("Color by Latency", "Colorize ping by ranges", () -> cfg.ping.colorByLatency, v -> cfg.ping.colorByLatency = v),
                        bool("Label", "Show the ping label", () -> cfg.ping.showLabel, v -> cfg.ping.showLabel = v),
                        bool("Background", "Draw a panel behind text", () -> cfg.ping.showBackground, v -> cfg.ping.showBackground = v),
                        fsetting("BG Opacity", "Background opacity", 0.15f, 0.9f, 0.05f, () -> cfg.ping.backgroundOpacity, v -> cfg.ping.backgroundOpacity = v),
                        fsetting("Scale", "Size of the ping text", 0.5f, 3.0f, 0.05f, () -> cfg.ping.scale, v -> cfg.ping.scale = v)
                )));

        modules.add(module("cps", "CPS", "Clicks per second", ModuleCategory.HUD,
                bool("Enabled", "Show CPS HUD", () -> cfg.cps.enabled, v -> cfg.cps.enabled = v),
                List.of(
                        bool("Show Right", "Show right-click CPS", () -> cfg.cps.showRight, v -> cfg.cps.showRight = v),
                        bool("Label", "Show the CPS label", () -> cfg.cps.showLabel, v -> cfg.cps.showLabel = v),
                        bool("Background", "Draw a panel behind text", () -> cfg.cps.showBackground, v -> cfg.cps.showBackground = v),
                        fsetting("BG Opacity", "Background opacity", 0.15f, 0.9f, 0.05f, () -> cfg.cps.backgroundOpacity, v -> cfg.cps.backgroundOpacity = v),
                        fsetting("Scale", "Size of the CPS text", 0.5f, 3.0f, 0.05f, () -> cfg.cps.scale, v -> cfg.cps.scale = v)
                )));

        modules.add(module("armor", "Armor", "Armor HUD with durability", ModuleCategory.HUD,
                bool("Enabled", "Show armor HUD", () -> cfg.armor.enabled, v -> cfg.armor.enabled = v),
                List.of(
                        bool("Durability", "Show durability percent", () -> cfg.armor.showDurability, v -> cfg.armor.showDurability = v),
                        bool("Background", "Draw a panel behind items", () -> cfg.armor.showBackground != null ? cfg.armor.showBackground : true, v -> cfg.armor.showBackground = v),
                        enumSetting("Layout", "Horizontal or vertical", BladeClientConfig.ArmorLayout.class, () -> cfg.armor.layout, v -> cfg.armor.layout = v),
                        fsetting("Scale", "Size of armor HUD", 0.5f, 3.0f, 0.05f, () -> cfg.armor.scale, v -> cfg.armor.scale = v)
                )));

        modules.add(module("potion", "Potion", "Active potion effects", ModuleCategory.HUD,
                bool("Enabled", "Show potion HUD", () -> cfg.potion.enabled, v -> cfg.potion.enabled = v),
                List.of(
                        bool("Background", "Draw a panel behind text", () -> cfg.potion.showBackground, v -> cfg.potion.showBackground = v),
                        bool("Icons", "Show effect icons", () -> cfg.potion.showIcons, v -> cfg.potion.showIcons = v),
                        fsetting("BG Opacity", "Background opacity", 0.15f, 0.9f, 0.05f, () -> cfg.potion.backgroundOpacity, v -> cfg.potion.backgroundOpacity = v),
                        fsetting("Scale", "Size of potion text", 0.5f, 3.0f, 0.05f, () -> cfg.potion.scale, v -> cfg.potion.scale = v)
                )));

        modules.add(module("apple_skin", "Apple Skin", "Hunger, saturation, and exhaustion info", ModuleCategory.HUD,
                bool("Enabled", "Show Apple Skin HUD", () -> cfg.appleSkin.enabled, v -> cfg.appleSkin.enabled = v),
                List.of(
                        bool("Label", "Show labels", () -> cfg.appleSkin.showLabel, v -> cfg.appleSkin.showLabel = v),
                        bool("Background", "Draw a panel behind text", () -> cfg.appleSkin.showBackground, v -> cfg.appleSkin.showBackground = v),
                        bool("Saturation", "Show saturation value", () -> cfg.appleSkin.showSaturation, v -> cfg.appleSkin.showSaturation = v),
                        bool("Exhaustion", "Show exhaustion value", () -> cfg.appleSkin.showExhaustion, v -> cfg.appleSkin.showExhaustion = v),
                        fsetting("BG Opacity", "Background opacity", 0.15f, 0.9f, 0.05f, () -> cfg.appleSkin.backgroundOpacity, v -> cfg.appleSkin.backgroundOpacity = v),
                        fsetting("Scale", "Size of Apple Skin text", 0.5f, 3.0f, 0.05f, () -> cfg.appleSkin.scale, v -> cfg.appleSkin.scale = v)
                )));

        modules.add(module("coords", "Coords", "Player coordinates", ModuleCategory.HUD,
                bool("Enabled", "Show XYZ coordinates", () -> cfg.coords.enabled, v -> cfg.coords.enabled = v),
                List.of(
                        bool("Biome", "Show biome name", () -> cfg.coords.showBiome, v -> cfg.coords.showBiome = v),
                        bool("Background", "Draw a panel behind text", () -> cfg.coords.showBackground, v -> cfg.coords.showBackground = v),
                        fsetting("BG Opacity", "Background opacity", 0.15f, 0.9f, 0.05f, () -> cfg.coords.backgroundOpacity, v -> cfg.coords.backgroundOpacity = v),
                        fsetting("Scale", "Size of coordinates text", 0.5f, 3.0f, 0.05f, () -> cfg.coords.scale, v -> cfg.coords.scale = v)
                )));

        modules.add(module("direction", "Direction", "Facing direction", ModuleCategory.HUD,
                bool("Enabled", "Show facing direction", () -> cfg.direction.enabled, v -> cfg.direction.enabled = v),
                List.of(
                        bool("Angle", "Show yaw angle", () -> cfg.direction.showAngle, v -> cfg.direction.showAngle = v),
                        bool("Background", "Draw a panel behind text", () -> cfg.direction.showBackground, v -> cfg.direction.showBackground = v),
                        fsetting("BG Opacity", "Background opacity", 0.15f, 0.9f, 0.05f, () -> cfg.direction.backgroundOpacity, v -> cfg.direction.backgroundOpacity = v),
                        fsetting("Scale", "Size of direction text", 0.5f, 3.0f, 0.05f, () -> cfg.direction.scale, v -> cfg.direction.scale = v)
                )));

        modules.add(module("toggle_sprint", "Toggle Sprint", "Sprint toggling (client-side)", ModuleCategory.HUD,
                bool("Enabled", "Enable toggle sprint", () -> cfg.toggleSprint.enabled, v -> cfg.toggleSprint.enabled = v),
                List.of(
                        enumSetting("Mode", "Toggle or auto sprint", BladeClientConfig.SprintMode.class, () -> cfg.toggleSprint.mode, v -> cfg.toggleSprint.mode = v),
                        bool("Show HUD", "Show sprint status label", () -> cfg.toggleSprint.showHud, v -> cfg.toggleSprint.showHud = v),
                        bool("Background", "Draw a panel behind label", () -> cfg.toggleSprint.showBackground, v -> cfg.toggleSprint.showBackground = v),
                        fsetting("BG Opacity", "Background opacity", 0.15f, 0.9f, 0.05f, () -> cfg.toggleSprint.backgroundOpacity, v -> cfg.toggleSprint.backgroundOpacity = v),
                        fsetting("Scale", "Size of sprint label", 0.5f, 3.0f, 0.05f, () -> cfg.toggleSprint.scale, v -> cfg.toggleSprint.scale = v)
                )));

        // Visual
        modules.add(module("crosshair", "Crosshair", "Custom crosshair", ModuleCategory.VISUAL,
                bool("Enabled", "Enable custom crosshair", () -> cfg.crosshair.enabled, v -> cfg.crosshair.enabled = v),
                List.of(
                        bool("Hide in F5", "Hide crosshair in third person", () -> cfg.crosshair.hideInF5, v -> cfg.crosshair.hideInF5 = v),
                        enumSetting("Style", "Crosshair style", BladeClientConfig.CrosshairStyle.class,
                                () -> cfg.crosshair.style, v -> cfg.crosshair.style = v),
                        intSetting("Size", "Crosshair size", 2, 24, 1,
                                () -> cfg.crosshair.size, v -> cfg.crosshair.size = v),
                        fsetting("Opacity", "Crosshair opacity", 0.2f, 1.0f, 0.05f,
                                () -> cfg.crosshair.opacity, v -> cfg.crosshair.opacity = v),
                        intSetting("Custom Hue", "Custom crosshair hue (0-360)", 0, 360, 1,
                                () -> cfg.crosshair.customHue, v -> cfg.crosshair.customHue = v),
                        intSetting("Custom Saturation", "Custom crosshair saturation (0-100)", 0, 100, 1,
                                () -> cfg.crosshair.customSaturation, v -> cfg.crosshair.customSaturation = v),
                        intSetting("Custom Value", "Custom crosshair value (0-100)", 0, 100, 1,
                                () -> cfg.crosshair.customValue, v -> cfg.crosshair.customValue = v)
                )));

        modules.add(module("blockoverlay", "Block Overlay", "Highlight the targeted block", ModuleCategory.VISUAL,
                bool("Enabled", "Enable block overlay", () -> cfg.blockOverlay.enabled, v -> cfg.blockOverlay.enabled = v),
                List.of(
                        intSetting("Hue", "Color hue (0-360)", 0, 360, 1,
                                () -> cfg.blockOverlay.hue, v -> cfg.blockOverlay.hue = v),
                        intSetting("Saturation", "Color saturation (0-100)", 0, 100, 1,
                                () -> cfg.blockOverlay.saturation, v -> cfg.blockOverlay.saturation = v),
                        intSetting("Value", "Color value (0-100)", 0, 100, 1,
                                () -> cfg.blockOverlay.value, v -> cfg.blockOverlay.value = v),
                        intSetting("Alpha", "Outline alpha (0-255)", 0, 255, 1,
                                () -> cfg.blockOverlay.alpha, v -> cfg.blockOverlay.alpha = v),
                        fsetting("Thickness", "Outline thickness", 0.5f, 10.0f, 0.1f,
                                () -> cfg.blockOverlay.thickness, v -> cfg.blockOverlay.thickness = v),
                        bool("Fill", "Fill the block shape", () -> cfg.blockOverlay.fill, v -> cfg.blockOverlay.fill = v),
                        intSetting("Fill Hue", "Fill hue (0-360)", 0, 360, 1,
                                () -> cfg.blockOverlay.fillHue, v -> cfg.blockOverlay.fillHue = v),
                        intSetting("Fill Saturation", "Fill saturation (0-100)", 0, 100, 1,
                                () -> cfg.blockOverlay.fillSaturation, v -> cfg.blockOverlay.fillSaturation = v),
                        intSetting("Fill Value", "Fill value (0-100)", 0, 100, 1,
                                () -> cfg.blockOverlay.fillValue, v -> cfg.blockOverlay.fillValue = v),
                        intSetting("Fill Alpha", "Fill alpha (0-255)", 0, 255, 1,
                                () -> cfg.blockOverlay.fillAlpha, v -> cfg.blockOverlay.fillAlpha = v)
                )));

        modules.add(module("zoom", "Zoom", "Hold to zoom the camera", ModuleCategory.VISUAL,
                bool("Enabled", "Enable zoom key", () -> cfg.zoom.enabled, v -> cfg.zoom.enabled = v),
                List.of(
                        keybind("Key", "Zoom keybind", ZoomModule.getKeyBinding()),
                        bool("Smooth Zoom", "Smooth in/out transition", () -> cfg.zoom.smoothZoom, v -> cfg.zoom.smoothZoom = v),
                        bool("Smooth Camera", "Smooth mouse movement while zooming", () -> cfg.zoom.smoothLook != null ? cfg.zoom.smoothLook : true, v -> cfg.zoom.smoothLook = v),
                        bool("Scroll Zoom", "Adjust zoom with mouse wheel", () -> cfg.zoom.scrollZoom, v -> cfg.zoom.scrollZoom = v),
                        fsetting("Zoom", "Zoom level (x)", 2.0f, 50.0f, 0.5f, () -> cfg.zoom.zoom, v -> cfg.zoom.zoom = v)
                )));

        modules.add(module("motion_blur", "Motion Blur", "Blend previous frame for a blur effect", ModuleCategory.VISUAL,
                bool("Enabled", "Enable motion blur", () -> cfg.motionBlur.enabled, v -> cfg.motionBlur.enabled = v),
                List.of(
                        fsetting("Strength", "Blur strength", 0.0f, 0.9f, 0.05f, () -> cfg.motionBlur.strength, v -> cfg.motionBlur.strength = v)
                )));

        modules.add(module("free_look", "Free Look", "Hold to look around in third person", ModuleCategory.VISUAL,
                bool("Enabled", "Enable free look", () -> cfg.freeLook.enabled, v -> cfg.freeLook.enabled = v),
                List.of(
                        keybind("Key", "Free look keybind", FreeLookModule.getKeyBinding())
                )));

        modules.add(module("night_vision", "Night Vision", "Fullbright night vision", ModuleCategory.VISUAL,
                bool("Enabled", "Enable night vision", () -> cfg.nightVision.enabled, v -> cfg.nightVision.enabled = v),
                List.of(
                        intSetting("Block Light", "Client-side block light level (1-15)", 1, 15, 1,
                                () -> cfg.nightVision.blockLight, v -> cfg.nightVision.blockLight = v)
                )));

        modules.add(module("saturation", "Color Saturation", "Adjust color saturation", ModuleCategory.VISUAL,
                bool("Enabled", "Enable saturation filter", () -> cfg.saturation.enabled, v -> cfg.saturation.enabled = v),
                List.of(
                        fsetting("Amount", "Saturation amount", 0.0f, 2.0f, 0.05f,
                                () -> cfg.saturation.amount, v -> cfg.saturation.amount = v)
                )));

        modules.add(module("mini_fov", "Mini FOV", "Override FOV and smooth camera effects", ModuleCategory.VISUAL,
                bool("Enabled", "Enable Mini FOV", () -> cfg.miniFov.enabled, v -> cfg.miniFov.enabled = v),
                List.of(
                        fsetting("Default FOV", "Base FOV while Mini FOV is enabled", 30.0f, 120.0f, 1.0f,
                                () -> cfg.miniFov.fov, v -> cfg.miniFov.fov = v),
                        bool("Smooth FOV Transition", "Smooth FOV changes", () -> cfg.miniFov.smoothEffects, v -> cfg.miniFov.smoothEffects = v),
                        bool("Dynamic Aiming FOV", "Apply aiming FOV while drawing a bow", () -> cfg.miniFov.dynamicAiming, v -> cfg.miniFov.dynamicAiming = v),
                        fsetting("Aiming Multiplier", "Scale bow FOV effect", 0.0f, 3.0f, 0.05f,
                                () -> cfg.miniFov.aimingMultiplier, v -> cfg.miniFov.aimingMultiplier = v),
                        fsetting("Aiming Minimum FOV", "Min FOV offset while aiming", -30.0f, 30.0f, 0.5f,
                                () -> cfg.miniFov.aimingMinOffset, v -> cfg.miniFov.aimingMinOffset = v),
                        fsetting("Aiming Maximum FOV", "Max FOV offset while aiming", -30.0f, 30.0f, 0.5f,
                                () -> cfg.miniFov.aimingMaxOffset, v -> cfg.miniFov.aimingMaxOffset = v),
                        bool("Dynamic Effect FOV", "Enable potion/water FOV effects", () -> !cfg.miniFov.disableDynamicFov, v -> cfg.miniFov.disableDynamicFov = !v),
                        bool("Dynamic Sprinting FOV", "Apply sprinting FOV offsets", () -> cfg.miniFov.dynamicSprinting, v -> cfg.miniFov.dynamicSprinting = v),
                        fsetting("Sprinting FOV", "Target sprinting FOV", 30.0f, 120.0f, 1.0f,
                                () -> cfg.miniFov.sprintFov, v -> cfg.miniFov.sprintFov = v),
                        fsetting("Sprint Multiplier", "Scale sprinting FOV offset", 0.0f, 3.0f, 0.05f,
                                () -> cfg.miniFov.sprintMultiplier, v -> cfg.miniFov.sprintMultiplier = v),
                        fsetting("Sprint Minimum FOV", "Min FOV offset while sprinting", -30.0f, 30.0f, 0.5f,
                                () -> cfg.miniFov.sprintMinOffset, v -> cfg.miniFov.sprintMinOffset = v),
                        fsetting("Sprint Maximum FOV", "Max FOV offset while sprinting", -30.0f, 30.0f, 0.5f,
                                () -> cfg.miniFov.sprintMaxOffset, v -> cfg.miniFov.sprintMaxOffset = v),
                        bool("Dynamic Flying FOV", "Apply flying FOV effect", () -> cfg.miniFov.dynamicFlying, v -> cfg.miniFov.dynamicFlying = v),
                        bool("Disable Hurt Shake", "Disable camera shake on hit", () -> cfg.miniFov.disableHurtCamera, v -> cfg.miniFov.disableHurtCamera = v),
                        bool("Disable View Bob", "Disable view bobbing", () -> cfg.miniFov.disableViewBobbing, v -> cfg.miniFov.disableViewBobbing = v)
                )));

        modules.add(module("time_changer", "Time Changer", "Client-side time of day", ModuleCategory.VISUAL,
                bool("Enabled", "Enable time changer", () -> cfg.timeChanger.enabled, v -> cfg.timeChanger.enabled = v),
                List.of(
                        intSetting("Time", "Client-side time of day (0-23999)", 0, 23999, 1,
                                () -> cfg.timeChanger.time, v -> cfg.timeChanger.time = v)
                )));

        // Utility
        modules.add(module("auto_text", "Auto Text", "Send a chat message repeatedly", ModuleCategory.UTILITY,
                bool("Enabled", "Enable auto text", () -> cfg.autoText.enabled, v -> cfg.autoText.enabled = v),
                List.of()));

        // UI
        modules.add(module("title_screen", "Title Screen", "Custom BladeClient title", ModuleCategory.UI,
                bool("Enabled", "Use custom title screen", () -> cfg.ui.replaceTitleScreen, v -> cfg.ui.replaceTitleScreen = v),
                List.of()));

        modules.add(module("fonts", "Fonts", "Load external fonts for UI and title", ModuleCategory.UI,
                bool("Enabled", "Enable external fonts", () -> cfg.fontPack.enabled, v -> cfg.fontPack.enabled = v),
                List.of(
                        stringSetting("UI Font", "Path to UI .ttf", 260, () -> cfg.fontPack.uiFontPath, v -> cfg.fontPack.uiFontPath = v),
                        stringSetting("Title Font", "Path to Title .ttf", 260, () -> cfg.fontPack.titleFontPath, v -> cfg.fontPack.titleFontPath = v),
                        fsetting("UI Size", "UI font size", 8.0f, 24.0f, 0.5f, () -> cfg.fontPack.uiSize, v -> cfg.fontPack.uiSize = v),
                        fsetting("Title Size", "Title font size", 14.0f, 48.0f, 1.0f, () -> cfg.fontPack.titleSize, v -> cfg.fontPack.titleSize = v)
                )));

    }

    private static Module module(String id, String name, String description, ModuleCategory category,
                                 BooleanSetting enabled, List<BaseSetting<?>> settings) {
        return new Module(id, name, description, category, enabled, settings);
    }

    private static BooleanSetting bool(String name, String description, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
        return new BooleanSetting(name, description, getter, setter, ConfigManager::saveQuiet);
    }

    private static FloatSetting fsetting(String name, String description, float min, float max, float step,
                                         java.util.function.Supplier<Float> getter, java.util.function.Consumer<Float> setter) {
        return new FloatSetting(name, description, min, max, step, getter, setter, ConfigManager::saveQuiet);
    }

    private static IntSetting intSetting(String name, String description, int min, int max, int step,
                                         java.util.function.Supplier<Integer> getter, java.util.function.Consumer<Integer> setter) {
        return new IntSetting(name, description, min, max, step, getter, setter, ConfigManager::saveQuiet);
    }

    private static <E extends Enum<E>> EnumSetting<E> enumSetting(String name, String description, Class<E> enumClass,
                                                                  java.util.function.Supplier<E> getter, java.util.function.Consumer<E> setter) {
        return new EnumSetting<>(name, description, enumClass, getter, setter, ConfigManager::saveQuiet);
    }

    private static KeybindSetting keybind(String name, String description, net.minecraft.client.option.KeyBinding binding) {
        return new KeybindSetting(name, description, binding);
    }

    private static StringSetting stringSetting(String name, String description, int maxLength,
                                               java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter) {
        return new StringSetting(name, description, maxLength, getter, setter, ConfigManager::saveQuiet);
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(modules);
    }

    public static List<Module> filtered(ModuleCategory category, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module module : modules) {
            if (category != ModuleCategory.ALL && module.category() != category) continue;
            if (!q.isEmpty()) {
                String hay = (module.name() + " " + module.description()).toLowerCase(Locale.ROOT);
                if (!hay.contains(q)) continue;
            }
            out.add(module);
        }
        return out;
    }
}
