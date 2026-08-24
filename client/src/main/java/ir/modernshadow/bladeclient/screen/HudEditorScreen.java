package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.config.BladeClientConfig;
import ir.modernshadow.bladeclient.config.ConfigManager;
import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {
    private enum Target {
        NONE,
        FPS,
        KEYSTROKES,
        PING,
        CPS,
        ARMOR,
        POTION,
        APPLE_SKIN,
        COORDS,
        DIRECTION,
        TOGGLE_SPRINT
    }

    private final Screen parent;

    private Target dragging = Target.NONE;
    private Target resizing = Target.NONE;
    private double dragOffsetX;
    private double dragOffsetY;
    private double resizeStartMouseX;
    private double resizeStartMouseY;
    private float resizeStartScale;

    private static final int HANDLE_SIZE = 10;
    private static final int HANDLE_PADDING = 2;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public void blur() {
        // Disable background blur for HUD Editor.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Target handle = hitTestHandle((int) mouseX, (int) mouseY);
            if (handle != Target.NONE) {
                resizing = handle;
                resizeStartMouseX = mouseX;
                resizeStartMouseY = mouseY;
                resizeStartScale = getScale(handle);
                return true;
            }
            Target target = hitTest((int) mouseX, (int) mouseY);
            if (target != Target.NONE) {
                dragging = target;
                int[] bounds = getBounds(target);
                dragOffsetX = mouseX - bounds[0];
                dragOffsetY = mouseY - bounds[1];
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button != 0) return super.mouseDragged(mouseX, mouseY, button, dx, dy);

        if (dragging != Target.NONE) {
            int nx = (int) Math.round(mouseX - dragOffsetX);
            int ny = (int) Math.round(mouseY - dragOffsetY);
            setPos(dragging, nx, ny);
            return true;
        }

        if (resizing != Target.NONE) {
            double dxv = mouseX - resizeStartMouseX;
            double dyv = mouseY - resizeStartMouseY;
            double mag = Math.abs(dxv) + Math.abs(dyv);
            double sign;
            if (Math.abs(dyv) > 0.001) {
                sign = Math.signum(dyv);
            } else {
                sign = Math.signum(dxv);
            }
            float newScale = (float) Math.max(0.5, Math.min(3.0, resizeStartScale + (sign * mag * 0.0025)));
            setScale(resizing, newScale);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = Target.NONE;
            resizing = Target.NONE;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == 256) {
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(parent));
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        BladeFonts.drawUiCentered(ctx, "HUD Editor", this.width / 2f, 8, 0xFFFFFFFF, BladeFonts.UI_SIZE, true);
        BladeFonts.drawUiCentered(ctx, "LMB: move | Drag handle to resize | ESC: exit", this.width / 2f, 24, 0xFFCCCCCC, 9, true);

        drawPreview(ctx, Target.FPS, 0x66000000, 0x66FFFFFF, "FPS");
        drawPreview(ctx, Target.KEYSTROKES, 0x66000000, 0x66FFFFFF, "Keystrokes");
        drawPreview(ctx, Target.PING, 0x66000000, 0x66FFFFFF, "Ping");
        drawPreview(ctx, Target.CPS, 0x66000000, 0x66FFFFFF, "CPS");
        drawPreview(ctx, Target.ARMOR, 0x66000000, 0x66FFFFFF, "Armor");
        drawPreview(ctx, Target.POTION, 0x66000000, 0x66FFFFFF, "Potion");
        drawPreview(ctx, Target.APPLE_SKIN, 0x66000000, 0x66FFFFFF, "Apple Skin");
        drawPreview(ctx, Target.COORDS, 0x66000000, 0x66FFFFFF, "Coords");
        drawPreview(ctx, Target.DIRECTION, 0x66000000, 0x66FFFFFF, "Direction");
        drawPreview(ctx, Target.TOGGLE_SPRINT, 0x66000000, 0x66FFFFFF, "Sprint");

        if (dragging != Target.NONE || resizing != Target.NONE) {
            Target t = dragging != Target.NONE ? dragging : resizing;
            int[] pos = getPos(t);
            String info = t.name() + " X=" + pos[0] + " Y=" + pos[1] + " S=" + String.format("%.2f", getScale(t));
            int tw = BladeFonts.uiWidth(info, 9);
            int tx = Math.min(this.width - tw - 6, (int) this.client.mouse.getX() + 12);
            int ty = Math.max(6, (int) this.client.mouse.getY() - 12);
            ctx.fill(tx - 4, ty - 4, tx + tw + 4, ty + 12, 0xCC000000);
            BladeFonts.drawUi(ctx, info, tx, ty, 0xFFFFFF, 9, false);
        }
    }

    private void drawPreview(DrawContext ctx, Target t, int fillColor, int borderColor, String label) {
        int[] b = getBounds(t);
        ctx.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], fillColor);
        ctx.fill(b[0], b[1], b[0] + b[2], b[1] + 1, borderColor);
        ctx.fill(b[0], b[1] + b[3] - 1, b[0] + b[2], b[1] + b[3], borderColor);

        BladeFonts.drawUiCentered(ctx, label, b[0] + b[2] / 2f, b[1] + b[3] / 2f - 4, 0xFFFFFFFF, 8, true);

        int hx = b[0] + b[2] - HANDLE_SIZE - HANDLE_PADDING;
        int hy = b[1] + b[3] - HANDLE_SIZE - HANDLE_PADDING;
        boolean hovered = hitTestHandle((int) this.client.mouse.getX(), (int) this.client.mouse.getY()) == t;
        int handleColor = resizing == t ? 0xFFE1E5EC : (hovered ? 0xFFB6BDC8 : 0xFF7A808A);
        ctx.fill(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, handleColor);
        ctx.fill(hx + 2, hy + 2, hx + HANDLE_SIZE - 2, hy + HANDLE_SIZE - 2, 0x40FFFFFF);
    }

    private int[] getPos(Target t) {
        BladeClientConfig cfg = ConfigManager.get();
        return switch (t) {
            case FPS -> new int[]{cfg.fps.x, cfg.fps.y};
            case KEYSTROKES -> new int[]{cfg.keystrokes.x, cfg.keystrokes.y};
            case PING -> new int[]{cfg.ping.x, cfg.ping.y};
            case CPS -> new int[]{cfg.cps.x, cfg.cps.y};
            case ARMOR -> new int[]{cfg.armor.x, cfg.armor.y};
            case POTION -> new int[]{cfg.potion.x, cfg.potion.y};
            case APPLE_SKIN -> new int[]{cfg.appleSkin.x, cfg.appleSkin.y};
            case COORDS -> new int[]{cfg.coords.x, cfg.coords.y};
            case DIRECTION -> new int[]{cfg.direction.x, cfg.direction.y};
            case TOGGLE_SPRINT -> new int[]{cfg.toggleSprint.x, cfg.toggleSprint.y};
            default -> new int[]{0, 0};
        };
    }

    private void setPos(Target t, int x, int y) {
        Bounds b = boundsFor(t);
        int nx = Math.max(0, Math.min(x, this.width - b.w));
        int ny = Math.max(0, Math.min(y, this.height - b.h));

        int cfgX = nx - b.offX;
        int cfgY = ny - b.offY;

        BladeClientConfig cfg = ConfigManager.get();
        switch (t) {
            case FPS -> { cfg.fps.x = cfgX; cfg.fps.y = cfgY; }
            case KEYSTROKES -> { cfg.keystrokes.x = cfgX; cfg.keystrokes.y = cfgY; }
            case PING -> { cfg.ping.x = cfgX; cfg.ping.y = cfgY; }
            case CPS -> { cfg.cps.x = cfgX; cfg.cps.y = cfgY; }
            case ARMOR -> { cfg.armor.x = cfgX; cfg.armor.y = cfgY; }
            case POTION -> { cfg.potion.x = cfgX; cfg.potion.y = cfgY; }
            case APPLE_SKIN -> { cfg.appleSkin.x = cfgX; cfg.appleSkin.y = cfgY; }
            case COORDS -> { cfg.coords.x = cfgX; cfg.coords.y = cfgY; }
            case DIRECTION -> { cfg.direction.x = cfgX; cfg.direction.y = cfgY; }
            case TOGGLE_SPRINT -> { cfg.toggleSprint.x = cfgX; cfg.toggleSprint.y = cfgY; }
            default -> {}
        }
        ConfigManager.saveQuiet();
    }

    private float getScale(Target t) {
        BladeClientConfig cfg = ConfigManager.get();
        return switch (t) {
            case FPS -> cfg.fps.scale;
            case KEYSTROKES -> cfg.keystrokes.scale;
            case PING -> cfg.ping.scale;
            case CPS -> cfg.cps.scale;
            case ARMOR -> cfg.armor.scale;
            case POTION -> cfg.potion.scale;
            case APPLE_SKIN -> cfg.appleSkin.scale;
            case COORDS -> cfg.coords.scale;
            case DIRECTION -> cfg.direction.scale;
            case TOGGLE_SPRINT -> cfg.toggleSprint.scale;
            default -> 1f;
        };
    }

    private void setScale(Target t, float scale) {
        float s = Math.max(0.5f, Math.min(3.0f, scale));
        BladeClientConfig cfg = ConfigManager.get();
        switch (t) {
            case FPS -> cfg.fps.scale = s;
            case KEYSTROKES -> cfg.keystrokes.scale = s;
            case PING -> cfg.ping.scale = s;
            case CPS -> cfg.cps.scale = s;
            case ARMOR -> cfg.armor.scale = s;
            case POTION -> cfg.potion.scale = s;
            case APPLE_SKIN -> cfg.appleSkin.scale = s;
            case COORDS -> cfg.coords.scale = s;
            case DIRECTION -> cfg.direction.scale = s;
            case TOGGLE_SPRINT -> cfg.toggleSprint.scale = s;
            default -> {}
        }
        ConfigManager.saveQuiet();
    }

    private int[] getBounds(Target t) {
        Bounds b = boundsFor(t);
        return new int[]{b.x, b.y, b.w, b.h};
    }

    private Bounds boundsFor(Target t) {
        BladeClientConfig cfg = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        int fontH = this.textRenderer != null ? this.textRenderer.fontHeight : 9;

        return switch (t) {
            case FPS -> boundsText(cfg.fps.x, cfg.fps.y,
                    (cfg.fps.showLabel ? "FPS: " : "") + (mc != null ? mc.getCurrentFps() : 60),
                    cfg.fps.scale, cfg.fps.showBackground);
            case PING -> {
                int ping = 0;
                if (mc != null && mc.getNetworkHandler() != null && mc.player != null) {
                    var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                    if (entry != null) ping = entry.getLatency();
                }
                String text = (cfg.ping.showLabel ? "Ping: " : "") + ping + " ms";
                yield boundsText(cfg.ping.x, cfg.ping.y, text, cfg.ping.scale, cfg.ping.showBackground);
            }
            case CPS -> {
                String prefix = cfg.cps.showLabel ? "CPS: " : "";
                String text = cfg.cps.showRight ? (prefix + "0 | 0") : (prefix + "0");
                yield boundsText(cfg.cps.x, cfg.cps.y, text, cfg.cps.scale, cfg.cps.showBackground);
            }
            case TOGGLE_SPRINT -> {
                String label = cfg.toggleSprint.mode == BladeClientConfig.SprintMode.AUTO
                        ? "Auto Sprint"
                        : (cfg.toggleSprint.active ? "Sprint: ON" : "Sprint: OFF");
                yield boundsText(cfg.toggleSprint.x, cfg.toggleSprint.y, label, cfg.toggleSprint.scale, cfg.toggleSprint.showBackground);
            }
            case DIRECTION -> {
                String facing = "N";
                float yaw = 0f;
                if (mc != null && mc.player != null) {
                    facing = mc.player.getHorizontalFacing().asString().toUpperCase();
                    yaw = mc.player.getYaw();
                    if (yaw < 0) yaw += 360f;
                }
                String text = cfg.direction.showAngle
                        ? ("Facing: " + facing + " (" + Math.round(yaw) + " deg)")
                        : ("Facing: " + facing);
                yield boundsText(cfg.direction.x, cfg.direction.y, text, cfg.direction.scale, cfg.direction.showBackground);
            }
            case COORDS -> {
                int px = 0, py = 0, pz = 0;
                String biome = "plains";
                if (mc != null && mc.player != null) {
                    px = (int) Math.floor(mc.player.getX());
                    py = (int) Math.floor(mc.player.getY());
                    pz = (int) Math.floor(mc.player.getZ());
                    if (mc.world != null) {
                        biome = mc.world.getBiome(mc.player.getBlockPos())
                                .getKey()
                                .map(key -> key.getValue().getPath())
                                .orElse("unknown");
                    }
                }
                String line1 = "XYZ: " + px + ", " + py + ", " + pz;
                String line2 = "Biome: " + biome;
                float scale = RenderUtil.clamp(cfg.coords.scale, 0.5f, 3.0f);
                int pad = cfg.coords.showBackground ? 6 : 0;

                int w1 = scaledTextWidth(line1, scale);
                int h1 = scaledTextHeight(scale, fontH);
                int w2 = cfg.coords.showBiome ? scaledTextWidth(line2, scale) : 0;
                int h2 = cfg.coords.showBiome ? scaledTextHeight(scale, fontH) : 0;
                int lineGap = RenderUtil.scale(fontH + 4, scale);

                int width = Math.max(w1, w2) + pad * 2;
                int height = h1 + pad * 2;
                if (cfg.coords.showBiome) {
                    height = lineGap + h2 + pad * 2;
                }

                yield new Bounds(cfg.coords.x - pad, cfg.coords.y - pad, width, height, -pad, -pad);
            }
            case POTION -> {
                float scale = RenderUtil.clamp(cfg.potion.scale, 0.5f, 3.0f);
                int pad = cfg.potion.showBackground ? 4 : 0;
                int iconSize = cfg.potion.showIcons ? Math.max(10, Math.round(12 * scale)) : 0;
                int iconGap = cfg.potion.showIcons ? Math.max(3, Math.round(4 * scale)) : 0;
                int baseLineH = Math.round(fontH * scale) + 2;
                int lineStep = Math.max(baseLineH, iconSize) + 1;

                List<String> lines = new ArrayList<>();
                if (mc != null && mc.player != null) {
                    var effects = mc.player.getStatusEffects();
                    for (var effect : effects) {
                        String name = effect.getEffectType().value().getName().getString();
                        int amp = effect.getAmplifier();
                        String ampText = amp > 0 ? " " + (amp + 1) : "";
                        String time = "0:00";
                        lines.add(name + ampText + " " + time);
                    }
                }
                if (lines.isEmpty()) {
                    lines.add("Speed II 0:00");
                }

                int maxLineW = 0;
                int lineH = Math.max(scaledTextHeight(scale, fontH), iconSize);
                for (String line : lines) {
                    int scaledW = scaledTextWidth(line, scale);
                    int lineW = scaledW + (iconSize > 0 ? iconSize + iconGap : 0);
                    if (lineW > maxLineW) maxLineW = lineW;
                }

                int width = maxLineW + pad * 2;
                int height = (lines.size() - 1) * lineStep + lineH + pad * 2;
                yield new Bounds(cfg.potion.x - pad, cfg.potion.y - pad, width, height, -pad, -pad);
            }
            case APPLE_SKIN -> {
                float scale = RenderUtil.clamp(cfg.appleSkin.scale, 0.5f, 3.0f);
                int pad = cfg.appleSkin.showBackground ? 6 : 0;
                List<String> lines = new ArrayList<>();
                lines.add("Hunger: 20/20");
                if (cfg.appleSkin.showSaturation) {
                    lines.add("Saturation: 20.0");
                }
                if (cfg.appleSkin.showExhaustion) {
                    lines.add("Exhaustion: 0.00");
                }

                int maxW = 0;
                int textH = scaledTextHeight(scale, fontH);
                for (String line : lines) {
                    maxW = Math.max(maxW, scaledTextWidth(line, scale));
                }
                int width = maxW + pad * 2;
                int height = textH * lines.size() + pad * 2;
                yield new Bounds(cfg.appleSkin.x - pad, cfg.appleSkin.y - pad, width, height, -pad, -pad);
            }
            case ARMOR -> {
                float scale = RenderUtil.clamp(cfg.armor.scale, 0.5f, 3.0f);
                int slot = 18;
                int step = slot + 2;
                int cell = slot + 1;
                int count = 5;
                int cellPx = Math.round(cell * scale);
                int stepPx = Math.round(step * scale);
                int size = cellPx + (count - 1) * stepPx;
                int off = -Math.round(1 * scale);
                if (cfg.armor.layout == BladeClientConfig.ArmorLayout.VERTICAL) {
                    yield new Bounds(cfg.armor.x + off, cfg.armor.y + off, cellPx, size, off, off);
                }
                yield new Bounds(cfg.armor.x + off, cfg.armor.y + off, size, cellPx, off, off);
            }
            case KEYSTROKES -> {
                float scale = RenderUtil.clamp(cfg.keystrokes.scale, 0.5f, 3.0f);
                int key = Math.max(8, Math.round(24 * scale));
                int gap = 0;
                int pad = Math.max(4, Math.round(4 * scale));
                int rowW = key * 3 + gap * 2;
                int kx = cfg.keystrokes.x;
                int ky = cfg.keystrokes.y;
                int rowX = kx + pad;
                int rowY = ky + pad;

                int contentH = key + gap + key + gap;
                if (cfg.keystrokes.showSpace) {
                    int spaceH = Math.max(14, Math.round(16 * scale));
                    contentH += spaceH + gap;
                }
                if (cfg.keystrokes.showMouse) {
                    int mouseH = Math.max(18, Math.round(22 * scale));
                    contentH += mouseH + gap;
                }

                int minX = rowX;
                int maxX = rowX + rowW;
                if (cfg.keystrokes.showMouse) {
                    int mouseExtra = Math.max(0, (int) Math.round(1.2 * scale));
                    int mouseW = (rowW - gap) / 2 + mouseExtra;
                    int lmbW = mouseW - 1;
                    int rmbW = mouseW - 1;
                    int mouseX = rowX - mouseExtra;
                    int lmbLeft = mouseX + 1;
                    int rmbRight = mouseX + 1 + lmbW + gap + rmbW;
                    minX = Math.min(minX, lmbLeft);
                    maxX = Math.max(maxX, rmbRight);
                }

                int width = maxX - minX + pad * 2;
                int height = contentH + pad * 2;
                int offX = minX - kx - pad;
                int offY = rowY - ky - pad;
                yield new Bounds(minX - pad, ky, width, height, offX, 0);
            }
            default -> new Bounds(0, 0, 40, 12, 0, 0);
        };
    }

    private Bounds boundsText(int x, int y, String text, float scale, boolean showBackground) {
        int pad = showBackground ? 6 : 0;
        int w = scaledTextWidth(text, scale) + pad * 2;
        int h = scaledTextHeight(scale, this.textRenderer != null ? this.textRenderer.fontHeight : 9) + pad * 2;
        return new Bounds(x - pad, y - pad, w, h, -pad, -pad);
    }

    private int scaledTextWidth(String text, float scale) {
        if (this.textRenderer == null) return 0;
        return RenderUtil.scale(this.textRenderer.getWidth(text), scale);
    }

    private int scaledTextHeight(float scale, int fontH) {
        return RenderUtil.scale(fontH, scale);
    }

    private static final class Bounds {
        final int x;
        final int y;
        final int w;
        final int h;
        final int offX;
        final int offY;

        Bounds(int x, int y, int w, int h, int offX, int offY) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.offX = offX;
            this.offY = offY;
        }
    }

    private Target hitTest(int mx, int my) {
        Target[] order = new Target[]{Target.TOGGLE_SPRINT, Target.DIRECTION, Target.COORDS, Target.APPLE_SKIN, Target.POTION, Target.ARMOR, Target.CPS, Target.PING, Target.KEYSTROKES, Target.FPS};
        for (Target t : order) {
            if (!isEnabled(t)) continue;
            int[] b = getBounds(t);
            if (mx >= b[0] && my >= b[1] && mx <= b[0] + b[2] && my <= b[1] + b[3]) return t;
        }
        return Target.NONE;
    }

    private Target hitTestHandle(int mx, int my) {
        Target[] order = new Target[]{Target.TOGGLE_SPRINT, Target.DIRECTION, Target.COORDS, Target.APPLE_SKIN, Target.POTION, Target.ARMOR, Target.CPS, Target.PING, Target.KEYSTROKES, Target.FPS};
        for (Target t : order) {
            if (!isEnabled(t)) continue;
            int[] b = getBounds(t);
            int hx = b[0] + b[2] - HANDLE_SIZE - HANDLE_PADDING;
            int hy = b[1] + b[3] - HANDLE_SIZE - HANDLE_PADDING;
            if (mx >= hx && my >= hy && mx <= hx + HANDLE_SIZE && my <= hy + HANDLE_SIZE) return t;
        }
        return Target.NONE;
    }

    private boolean isEnabled(Target t) {
        BladeClientConfig cfg = ConfigManager.get();
        return switch (t) {
            case FPS -> cfg.fps.enabled;
            case KEYSTROKES -> cfg.keystrokes.enabled;
            case PING -> cfg.ping.enabled;
            case CPS -> cfg.cps.enabled;
            case ARMOR -> cfg.armor.enabled;
            case POTION -> cfg.potion.enabled;
            case APPLE_SKIN -> cfg.appleSkin.enabled;
            case COORDS -> cfg.coords.enabled;
            case DIRECTION -> cfg.direction.enabled;
            case TOGGLE_SPRINT -> cfg.toggleSprint.enabled;
            default -> false;
        };
    }
}
