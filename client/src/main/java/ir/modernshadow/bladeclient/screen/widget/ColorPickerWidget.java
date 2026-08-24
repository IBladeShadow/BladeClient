package ir.modernshadow.bladeclient.screen.widget;

import ir.modernshadow.bladeclient.module.setting.IntSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ColorPickerWidget extends ClickableWidget {
    private static final int PADDING = 6;
    private static final int HUE_BAR_W = 12;
    private static final int HUE_GAP = 8;
    private static final int CURSOR_SIZE = 4;
    private static int ID_SEQ = 0;

    private final IntSetting hue;
    private final IntSetting saturation;
    private final IntSetting value;

    private boolean draggingSV = false;
    private boolean draggingHue = false;

    private final Identifier svTexId;
    private final Identifier hueTexId;
    private NativeImageBackedTexture svTexture;
    private NativeImageBackedTexture hueTexture;
    private int lastHue = -1;
    private int lastSvSize = -1;

    public ColorPickerWidget(int x, int y, int width, int height,
                             IntSetting hue, IntSetting saturation, IntSetting value) {
        super(x, y, width, height, Text.literal("Color"));
        this.hue = hue;
        this.saturation = saturation;
        this.value = value;
        int id = ID_SEQ++;
        this.svTexId = Identifier.of("bladeclient", "colorpicker/sv_" + id);
        this.hueTexId = Identifier.of("bladeclient", "colorpicker/hue_" + id);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int svSize = Math.min(h - PADDING * 2, w - (PADDING * 2 + HUE_BAR_W + HUE_GAP));
        if (svSize <= 0) {
            return;
        }

        int svX = x + PADDING;
        int svY = y + PADDING;
        int hueX = svX + svSize + HUE_GAP;
        int hueY = svY;

        // No panel background: blend directly with module settings container

        // Cached textures
        int hueVal = clamp(hue.get(), 0, 360);
        ensureTextures(svSize, hueVal);
        ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, svTexId,
                svX, svY, 0.0f, 0.0f, svSize, svSize, svSize, svSize);
        ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, hueTexId,
                hueX, hueY, 0.0f, 0.0f, HUE_BAR_W, svSize, HUE_BAR_W, svSize);

        // SV cursor
        float sVal = clamp(saturation.get(), 0, 100) / 100.0f;
        float vVal = clamp(value.get(), 0, 100) / 100.0f;
        int cx = svX + Math.round(sVal * (svSize - 1));
        int cy = svY + Math.round((1.0f - vVal) * (svSize - 1));
        drawCursor(ctx, cx, cy, 0xFFFFFFFF);

        // Hue cursor (simple white marker)
        float hPos = 1.0f - (clamp(hueVal, 0, 360) / 360.0f);
        int hy = hueY + Math.round(hPos * (svSize - 1));
        ctx.fill(hueX - 1, hy - 1, hueX + HUE_BAR_W + 1, hy + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (handlePick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && (draggingSV || draggingHue)) {
            handlePick(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSV = false;
            draggingHue = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean handlePick(double mouseX, double mouseY) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int svSize = Math.min(h - PADDING * 2, w - (PADDING * 2 + HUE_BAR_W + HUE_GAP));
        if (svSize <= 0) {
            return false;
        }
        int svX = x + PADDING;
        int svY = y + PADDING;
        int hueX = svX + svSize + HUE_GAP;
        int hueY = svY;

        if (mouseX >= svX && mouseX <= svX + svSize && mouseY >= svY && mouseY <= svY + svSize) {
            int px = clamp((int) Math.round(mouseX) - svX, 0, svSize - 1);
            int py = clamp((int) Math.round(mouseY) - svY, 0, svSize - 1);
            int sat = Math.round((px / (float) (svSize - 1)) * 100.0f);
            int val = Math.round((1.0f - (py / (float) (svSize - 1))) * 100.0f);
            saturation.set(clamp(sat, 0, 100));
            value.set(clamp(val, 0, 100));
            draggingSV = true;
            return true;
        }

        if (mouseX >= hueX && mouseX <= hueX + HUE_BAR_W && mouseY >= hueY && mouseY <= hueY + svSize) {
            int py = clamp((int) Math.round(mouseY) - hueY, 0, svSize - 1);
            int hVal = Math.round((1.0f - (py / (float) (svSize - 1))) * 360.0f);
            hue.set(clamp(hVal, 0, 360));
            draggingHue = true;
            return true;
        }

        return false;
    }

    private void drawCursor(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x - CURSOR_SIZE, y - 1, x + CURSOR_SIZE + 1, y + 1, color);
        ctx.fill(x - 1, y - CURSOR_SIZE, x + 1, y + CURSOR_SIZE + 1, color);
    }

    private void ensureTextures(int svSize, int hueVal) {
        if (svSize <= 0) return;
        if (svTexture == null || hueTexture == null || lastSvSize != svSize) {
            rebuildTextures(svSize, hueVal);
            return;
        }
        if (lastHue != hueVal) {
            updateSvTexture(svSize, hueVal);
        }
    }

    private void rebuildTextures(int svSize, int hueVal) {
        closeTextures();
        lastSvSize = svSize;
        lastHue = hueVal;

        NativeImage svImg = new NativeImage(svSize, svSize, false);
        fillSvImage(svImg, hueVal);
        svTexture = new NativeImageBackedTexture(() -> "bladeclient/sv", svImg);

        NativeImage hueImg = new NativeImage(HUE_BAR_W, svSize, false);
        fillHueImage(hueImg);
        hueTexture = new NativeImageBackedTexture(() -> "bladeclient/hue", hueImg);

        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        tm.registerTexture(svTexId, svTexture);
        tm.registerTexture(hueTexId, hueTexture);
    }

    private void updateSvTexture(int svSize, int hueVal) {
        lastHue = hueVal;
        NativeImage img = svTexture.getImage();
        if (img == null || img.getWidth() != svSize || img.getHeight() != svSize) {
            rebuildTextures(svSize, hueVal);
            return;
        }
        fillSvImage(img, hueVal);
        svTexture.upload();
    }

    private void fillSvImage(NativeImage img, int hueVal) {
        int size = img.getWidth();
        for (int py = 0; py < size; py++) {
            float v = 1.0f - (py / (float) (size - 1));
            for (int px = 0; px < size; px++) {
                float s = px / (float) (size - 1);
                int argb = hsvToArgb(hueVal, s, v, 1.0f);
                img.setColor(px, py, argb);
            }
        }
    }

    private void fillHueImage(NativeImage img) {
        int h = img.getHeight();
        int w = img.getWidth();
        for (int py = 0; py < h; py++) {
            float hNorm = 1.0f - (py / (float) (h - 1));
            int hue = (Math.round(hNorm * 360.0f) + 240) % 360; // rotate so blue is where expected
            int argb = hsvToArgb(hue, 1.0f, 1.0f, 1.0f);
            for (int px = 0; px < w; px++) {
                img.setColor(px, py, argb);
            }
        }
    }

    private void closeTextures() {
        if (svTexture != null) {
            svTexture.close();
            svTexture = null;
        }
        if (hueTexture != null) {
            hueTexture.close();
            hueTexture = null;
        }
    }

    private static int hsvToArgb(int hue, float saturation, float value, float alpha) {
        float h = ((hue % 360) + 360) % 360;
        float s = clamp01(saturation);
        float v = clamp01(value);

        float c = v * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;

        float r1, g1, b1;
        if (h < 60) {
            r1 = c; g1 = x; b1 = 0;
        } else if (h < 120) {
            r1 = x; g1 = c; b1 = 0;
        } else if (h < 180) {
            r1 = 0; g1 = c; b1 = x;
        } else if (h < 240) {
            r1 = 0; g1 = x; b1 = c;
        } else if (h < 300) {
            r1 = x; g1 = 0; b1 = c;
        } else {
            r1 = c; g1 = 0; b1 = x;
        }

        int r = Math.round((r1 + m) * 255.0f);
        int g = Math.round((g1 + m) * 255.0f);
        int b = Math.round((b1 + m) * 255.0f);
        int a = Math.round(clamp01(alpha) * 255.0f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
