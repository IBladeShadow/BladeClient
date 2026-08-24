package ir.modernshadow.bladeclient.screen.widget;

import io.wispforest.owo.ui.component.ButtonComponent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import ir.modernshadow.bladeclient.font.BladeFonts;

import java.util.function.Consumer;

public class GlassOwoButton extends ButtonComponent {
    private float hoverProgress = 0f;
    private static final float HOVER_SPEED = 1.0f;
    private static final int MAX_ALPHA = 0x4D; // ~30%
    private final float textScale;
    private final boolean hoverWhite;

    public GlassOwoButton(Text message, Consumer<ButtonComponent> onPress, float textScale, boolean hoverWhite) {
        super(message, onPress);
        this.textScale = Math.max(0.5f, Math.min(1.2f, textScale));
        this.hoverWhite = hoverWhite;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
        float step = 0.15f * HOVER_SPEED;
        if (hovered) {
            hoverProgress = Math.min(1f, hoverProgress + step);
        } else {
            hoverProgress = Math.max(0f, hoverProgress - step);
        }

        int bg = 0x88222233;
        context.fill(x, y, x + w, y + h, bg);
        context.fill(x, y, x + w, y + 1, 0x44FFFFFF);
        context.fill(x, y + h - 1, x + w, y + h, 0x22000000);

        if (hoverWhite) {
            int hoverBg = lerpColor(0x88222233, 0x80FFFFFF, hoverProgress);
            context.fill(x, y, x + w, y + h, hoverBg);
        } else {
            int a = Math.round(MAX_ALPHA * hoverProgress);
            if (a > 0) {
                int overlay = (a << 24) | 0x00FFFFFF;
                context.fill(x, y, x + w, y + h, overlay);
            }
        }

        int textCol;
        if (this.active()) {
            textCol = 0xFFFFFFFF;
        } else {
            textCol = 0xFFA0A0A0;
        }

        String label = this.getMessage().getString();
        float size = BladeFonts.UI_SIZE * textScale;
        BladeFonts.drawUiCentered(context, label, x + w / 2.0f, y + h / 2.0f, textCol, size, true);
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a1 = (from >>> 24) & 0xFF;
        int r1 = (from >>> 16) & 0xFF;
        int g1 = (from >>> 8) & 0xFF;
        int b1 = from & 0xFF;
        int a2 = (to >>> 24) & 0xFF;
        int r2 = (to >>> 16) & 0xFF;
        int g2 = (to >>> 8) & 0xFF;
        int b2 = to & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
