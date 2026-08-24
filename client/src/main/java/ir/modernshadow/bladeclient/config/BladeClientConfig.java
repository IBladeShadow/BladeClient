package ir.modernshadow.bladeclient.config;

import java.util.ArrayList;
import java.util.List;

public final class BladeClientConfig {
    public int configVersion;

    public Fps fps = new Fps();
    public Keystrokes keystrokes = new Keystrokes();
    public Ping ping = new Ping();
    public Cps cps = new Cps();
    public Armor armor = new Armor();
    public Potion potion = new Potion();
    public AppleSkin appleSkin = new AppleSkin();
    public Coords coords = new Coords();
    public Direction direction = new Direction();
    public BlockOverlay blockOverlay = new BlockOverlay();
    public Crosshair crosshair = new Crosshair();
    public Zoom zoom = new Zoom();
    public FreeLook freeLook = new FreeLook();
    public NightVision nightVision = new NightVision();
    public Saturation saturation = new Saturation();
    public MotionBlur motionBlur = new MotionBlur();
    public TimeChanger timeChanger = new TimeChanger();
    public MiniFov miniFov = new MiniFov();
    public FontPack fontPack = new FontPack();
    public ToggleSprint toggleSprint = new ToggleSprint();
    public AutoText autoText = new AutoText();
    public SkinChanger skin = new SkinChanger();
    public Ui ui = new Ui();
    public Account account = new Account();
    public boolean launchFullscreen = false;

    public static final class Fps {
        public boolean enabled = true;
        public boolean colorByFps = true;
        public boolean showLabel = true;
        public boolean showBackground = true;
        public float backgroundOpacity = 0.5f;
        public int x = 10;
        public int y = 10;
        public float posX = 0.01f;
        public float posY = 0.01f;
        public float scale = 1.0f;
    }

    public static final class Keystrokes {
        public boolean enabled = true;
        public boolean showBackground = false;
        public boolean showSpace = true;
        public boolean showMouse = true;
        public boolean showCps = true;
        public int x = 10;
        public int y = 40;
        public float posX = 0.01f;
        public float posY = 0.05f;
        public float scale = 1.0f;
    }

    public static final class Ping {
        public boolean enabled = true;
        public boolean colorByLatency = true;
        public boolean showLabel = true;
        public boolean showBackground = true;
        public float backgroundOpacity = 0.5f;
        public int color = 0xFFFFFFFF;
        public int x = 10;
        public int y = 130;
        public float posX = 0.01f;
        public float posY = 0.18f;
        public float scale = 1.0f;
    }

    public static final class Cps {
        public boolean enabled = true;
        public boolean showRight = true;
        public boolean showLabel = true;
        public boolean showBackground = true;
        public float backgroundOpacity = 0.5f;
        public int x = 10;
        public int y = 160;
        public float posX = 0.01f;
        public float posY = 0.22f;
        public float scale = 1.0f;
    }

    public static final class Armor {
        public boolean enabled = true;
        public boolean showDurability = true;
        public Boolean showBackground = null;
        public ArmorLayout layout = ArmorLayout.HORIZONTAL;
        public int x = 10;
        public int y = 190;
        public float posX = 0.01f;
        public float posY = 0.26f;
        public float scale = 1.0f;
    }

    public static final class Potion {
        public boolean enabled = true;
        public boolean showBackground = true;
        public float backgroundOpacity = 0.5f;
        public boolean showIcons = true;
        public int x = 10;
        public int y = 250;
        public float posX = 0.01f;
        public float posY = 0.35f;
        public float scale = 1.0f;
    }

    public static final class AppleSkin {
        public boolean enabled = false;
        public boolean showLabel = false;
        public boolean showBackground = true;
        public float backgroundOpacity = 0.5f;
        public boolean showSaturation = true;
        public boolean showExhaustion = false;
        public int x = 10;
        public int y = 280;
        public float posX = 0.01f;
        public float posY = 0.39f;
        public float scale = 1.0f;
    }

    public static final class Coords {
        public boolean enabled = true;
        public boolean showBiome = false;
        public boolean showBackground = true;
        public float backgroundOpacity = 0.5f;
        public int x = 10;
        public int y = 300;
        public float posX = 0.01f;
        public float posY = 0.42f;
        public float scale = 1.0f;
    }

    public static final class Direction {
        public boolean enabled = true;
        public boolean showAngle = true;
        public boolean showBackground = true;
        public float backgroundOpacity = 0.5f;
        public int x = 10;
        public int y = 330;
        public float posX = 0.01f;
        public float posY = 0.46f;
        public float scale = 1.0f;
    }

    public static final class BlockOverlay {
        public boolean enabled = false;
        public int hue = 210;        // 0-360
        public int saturation = 70;  // 0-100
        public int value = 100;      // 0-100
        public int alpha = 180;
        public float thickness = 1.5f;
        public boolean fill = false;
        public int fillAlpha = 64;
        public int fillHue = 210;
        public int fillSaturation = 70;
        public int fillValue = 100;
        // Legacy RGB fields (kept for backward compatibility with older configs)
        public int red = 76;
        public int green = 163;
        public int blue = 255;
    }

    public static final class Crosshair {
        public boolean enabled = false;
        public boolean hideInF5 = false;
        public CrosshairStyle style = CrosshairStyle.PLUS;
        public CrosshairColor color = CrosshairColor.NEON_BLUE;
        public int size = 6;
        public int gap = 3;
        public int thickness = 2;
        public float opacity = 1.0f;
        public int customHue = 210;
        public int customSaturation = 70;
        public int customValue = 100;
        public int[] customPixels = new int[16];
    }


    public static final class Zoom {
        public boolean enabled = false;
        public boolean smoothZoom = true;
        public Boolean smoothLook = null;
        public boolean scrollZoom = true;
        public float zoom = 4.0f;
    }

    public static final class FreeLook {
        public boolean enabled = false;
    }

    public static final class NightVision {
        public boolean enabled = false;
        public int blockLight = 15;
    }

    public static final class Saturation {
        public boolean enabled = false;
        public float amount = 1.0f;
    }

    public static final class MotionBlur {
        public boolean enabled = false;
        public float strength = 0.5f;
    }

    public static final class MiniFov {
        public boolean enabled = false;
        public float fov = 70.0f; // Default FOV
        public boolean smoothEffects = true;
        public boolean dynamicAiming = true;
        public float aimingMultiplier = 1.0f;
        public float aimingMinOffset = -10.0f;
        public float aimingMaxOffset = 10.0f;
        public boolean dynamicSprinting = true;
        public float sprintFov = 70.0f;
        public float sprintMultiplier = 1.0f;
        public float sprintMinOffset = -10.0f;
        public float sprintMaxOffset = 10.0f;
        public boolean dynamicFlying = true;
        public boolean disableHurtCamera = true;
        public boolean disableViewBobbing = true;
        public boolean disableDynamicFov = true;
    }

    public static final class TimeChanger {
        public boolean enabled = false;
        public int time = 6000;
    }

    public static final class FontPack {
        public boolean enabled = false;
        public String uiFontPath = "";
        public String titleFontPath = "";
        public float uiSize = 11.0f;
        public float titleSize = 24.0f;
    }

    public static final class ToggleSprint {
        public boolean enabled = false;
        public SprintMode mode = SprintMode.TOGGLE;
        public boolean active = false;
        public boolean showHud = true;
        public boolean showBackground = true;
        public float backgroundOpacity = 0.5f;
        public int x = 10;
        public int y = 100;
        public float posX = 0.01f;
        public float posY = 0.14f;
        public float scale = 1.0f;
    }

    public static final class AutoText {
        public boolean enabled = false;
        // Legacy single message fields (kept for migration)
        public String message = "";
        public int intervalSeconds = 10;
        public List<AutoTextEntry> entries = new ArrayList<>();
    }

    public static final class AutoTextEntry {
        public String message = "";
        public int intervalSeconds = 10;
        public int keyType = 0; // InputUtil.Type.KEYSYM.ordinal()
        public int keyCode = -1; // -1 = unbound
    }

    public static final class Ui {
        public boolean replaceTitleScreen = true;
    }

    public static final class Account {
        public boolean useOffline = false;
        public String offlineName = "";
        public List<String> offlineAccounts = new ArrayList<>();
        public String microsoftClientId = "";
    }

    public static final class SkinChanger {
        public boolean enabled = false;
        public Boolean showMojangSkins = null;
        public Boolean showVanillaCape = null;
        public Boolean showOptifineCape = null;
        public SkinMode mode = SkinMode.FILE;
        public SkinModel model = SkinModel.AUTO;
        public String filePath = "";
        public String username = "";
        public int applyNonce = 0;
        public List<SkinPreset> presets = new ArrayList<>();
    }

    public enum CrosshairStyle {
        DOT,
        PLUS,
        PLUS_GAP,
        SQUARE,
        SQUARE_DOT,
        CIRCLE,
        CIRCLE_DOT,
        X,
        X_DOT,
        CHEVRON,
        X_STAR,
        WIDE_RING,
        TECH,
        CUSTOM
    }

    public enum SprintMode {
        TOGGLE,
        AUTO
    }

    public enum CrosshairColor {
        NEON_BLUE(0xFF4AA3FF),
        NEON_GREEN(0xFF4CFF9A),
        NEON_PINK(0xFFFF4CA8),
        WHITE(0xFFFFFFFF);

        public final int argb;

        CrosshairColor(int argb) {
            this.argb = argb;
        }
    }


    public enum ArmorLayout {
        HORIZONTAL,
        VERTICAL
    }

    public enum SkinMode {
        FILE,
        USERNAME
    }

    public enum SkinModel {
        AUTO,
        WIDE,
        SLIM
    }

    public static final class SkinPreset {
        public String name = "";
        public SkinMode mode = SkinMode.FILE;
        public SkinModel model = SkinModel.AUTO;
        public String filePath = "";
        public String username = "";
    }

}
