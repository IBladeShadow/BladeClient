package ir.modernshadow.bladeclient.screen.widget;

import ir.modernshadow.bladeclient.font.BladeFonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class SettingRowButtonWidget extends ButtonWidget {
    private static final float HOVER_SPEED = 1.0f;
    private static final int MAX_ALPHA = 0x4D; // ~30%
    private static final int ARROW_W = 18;

    private final String label;
    private final Supplier<String> value;
    private final boolean showArrow;
    private float hoverProgress = 0f;

    public SettingRowButtonWidget(int x, int y, int width, int height, String label, Supplier<String> value,
                                  PressAction onPress, boolean showArrow) {
        super(x, y, width, height, Text.literal(""), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.value = value;
        this.showArrow = showArrow;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        boolean hovered = isMouseOver(mouseX, mouseY);
        if (hovered) {
            hoverProgress = Math.min(1f, hoverProgress + delta * HOVER_SPEED);
        } else {
            hoverProgress = Math.max(0f, hoverProgress - delta * HOVER_SPEED);
        }

        int labelX = x + 8;
        int textCol = this.active ? 0xFFB6BDC8 : 0xFF7A808A;
        BladeFonts.drawUi(context, label, labelX, y + (h - BladeFonts.UI_SIZE) / 2.0f + 1,
                textCol, BladeFonts.UI_SIZE, true);

        String valueText = value.get();
        int valueW = BladeFonts.uiWidth(valueText, BladeFonts.UI_SMALL);
        int rightPad = showArrow ? ARROW_W + 6 : 8;
        int valueX = x + w - rightPad - valueW;
        BladeFonts.drawUi(context, valueText, valueX, y + (h - BladeFonts.UI_SMALL) / 2.0f + 1,
                0xFFFFFFFF, BladeFonts.UI_SMALL, true);

        if (showArrow) {
            int arrowX = x + w - ARROW_W;
            int arrowY = y + (int) ((h - BladeFonts.UI_SMALL) / 2.0f) + 1;
            BladeFonts.drawUi(context, ">", arrowX + 6, arrowY, 0xFFB6BDC8, BladeFonts.UI_SMALL, true);
        }
    }

    private boolean isMouseOver(int mx, int my) {
        return mx >= this.getX() && my >= this.getY() && mx < this.getX() + this.getWidth() && my < this.getY() + this.getHeight();
    }
}
