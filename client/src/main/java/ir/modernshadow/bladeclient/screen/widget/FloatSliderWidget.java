package ir.modernshadow.bladeclient.screen.widget;

import ir.modernshadow.bladeclient.font.BladeFonts;
import ir.modernshadow.bladeclient.module.setting.FloatSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class FloatSliderWidget extends SliderWidget {
    private final FloatSetting setting;

    public FloatSliderWidget(int x, int y, int width, int height, FloatSetting setting) {
        super(x, y, width, height, Text.literal(""), 0.0);
        this.setting = setting;
        this.value = toSlider(setting.get());
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        float val = toValue(this.value);
        String label = setting.name() + ": " + String.format(java.util.Locale.US, "%.2f", val);
        this.setMessage(Text.literal(label));
    }

    @Override
    protected void applyValue() {
        float val = toValue(this.value);
        setting.set(val);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        float val = toValue(this.value);
        String label = setting.name();
        String valueText = String.format(java.util.Locale.US, "%.2f", val);

        float textY = y + 2;
        BladeFonts.drawUi(context, label, x + 2, textY, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        int valueW = BladeFonts.uiWidth(valueText, BladeFonts.UI_SMALL);
        BladeFonts.drawUi(context, valueText, x + w - 2 - valueW, textY, 0xFFFFFFFF, BladeFonts.UI_SMALL, true);

        int trackH = 4;
        int trackY = y + h - 8;
        int trackColor = 0x5510151D;
        context.fill(x, trackY, x + w, trackY + trackH, trackColor);
        context.fill(x, trackY, x + w, trackY + 1, 0x22FFFFFF);
        context.fill(x, trackY + trackH - 1, x + w, trackY + trackH, 0x33000000);

        int knobR = 6;
        int knobCx = x + (int) Math.round(this.value * (w - knobR * 2)) + knobR;
        int knobCy = trackY + trackH / 2;
        int knobColor = 0xFF4AA3FF;
        drawCircle(context, knobCx, knobCy, knobR, knobColor);
        drawCircle(context, knobCx, knobCy - 1, knobR, 0x66FFFFFF);
    }

    private static void drawCircle(DrawContext ctx, int cx, int cy, int r, int color) {
        int aBase = (color >>> 24) & 0xFF;
        int rC = (color >>> 16) & 0xFF;
        int gC = (color >>> 8) & 0xFF;
        int bC = color & 0xFF;
        float rr = r + 0.5f;
        int min = -r - 1;
        int max = r + 1;
        for (int dy = min; dy <= max; dy++) {
            for (int dx = min; dx <= max; dx++) {
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float edge = rr - dist;
                if (edge <= 0.0f) continue;
                float alpha = edge >= 1.0f ? aBase : (aBase * edge);
                int a = Math.min(255, Math.max(0, Math.round(alpha)));
                if (a == 0) continue;
                int c = (a << 24) | (rC << 16) | (gC << 8) | bC;
                ctx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, c);
            }
        }
    }

    private double toSlider(float value) {
        float min = setting.min();
        float max = setting.max();
        if (max <= min) return 0.0;
        float clamped = Math.max(min, Math.min(max, value));
        return (clamped - min) / (max - min);
    }

    private float toValue(double slider) {
        float min = setting.min();
        float max = setting.max();
        float raw = (float) (min + slider * (max - min));
        float stepped = step(raw, setting.step());
        return Math.max(min, Math.min(max, stepped));
    }

    private float step(float value, float step) {
        if (step <= 0f) return value;
        return Math.round(value / step) * step;
    }
}
