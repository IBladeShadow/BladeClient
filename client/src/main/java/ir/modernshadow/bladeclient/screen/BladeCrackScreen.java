package ir.modernshadow.bladeclient.screen;

import ir.modernshadow.bladeclient.font.BladeFonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class BladeCrackScreen extends Screen {
    private static final int BG_COLOR = 0xFF0D0D1A;
    private static final int ACCENT_COLOR = 0xFF4AA3FF;
    private static final int WARN_COLOR = 0xFFFF4444;

    public BladeCrackScreen() {
        super(Text.literal("Unauthorized"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, BG_COLOR);

        int cx = this.width / 2;
        int cy = this.height / 2;

        String title = "Unauthorized";
        int titleW = BladeFonts.titleWidth(title, 2.5f);
        BladeFonts.drawTitle(context, title, (cx - titleW / 2f), cy - 60, WARN_COLOR, 2.5f, true);

        String msg1 = "BladeClient is protected by launcher verification.";
        int m1w = BladeFonts.uiWidth(msg1, BladeFonts.UI_SIZE);
        BladeFonts.drawUi(context, msg1, (cx - m1w / 2f), cy - 15, 0xFFB6BDC8, BladeFonts.UI_SIZE, true);

        String msg2 = "Please launch the game using the official BladeClient Launcher.";
        int m2w = BladeFonts.uiWidth(msg2, BladeFonts.UI_SIZE);
        BladeFonts.drawUi(context, msg2, (cx - m2w / 2f), cy + 5, 0xFFB6BDC8, BladeFonts.UI_SIZE, true);

        String exit = "This window will close automatically.";
        int exw = BladeFonts.uiWidth(exit, BladeFonts.UI_SMALL);
        BladeFonts.drawUi(context, exit, (cx - exw / 2f), cy + 35, 0xFF8B929E, BladeFonts.UI_SMALL, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
