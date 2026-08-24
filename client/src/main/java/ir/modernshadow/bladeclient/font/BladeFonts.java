package ir.modernshadow.bladeclient.font;

import net.minecraft.client.gui.DrawContext;

public final class BladeFonts {
    private BladeFonts() {}

    public static final float UI_SIZE = 11.0f;
    public static final float UI_SMALL = 9.0f;
    public static final float TITLE_SIZE = 20.0f;
    public static final float PERSIAN_SIZE = 11.0f;

    private static final String UI_FONT_PATH = "/assets/bladeclient/font/regular.otf";
    private static final String TITLE_FONT_PATH = "/assets/bladeclient/font/fjallaone-regular.ttf";
    private static final String PERSIAN_FONT_PATH = "/assets/bladeclient/font/vazirmatn.ttf";

    public static final BladeFont UI = new BladeFont(UI_FONT_PATH, UI_SIZE);
    public static final BladeFont TITLE = new BladeFont(TITLE_FONT_PATH, TITLE_SIZE);
    public static final BladeFont PERSIAN = new BladeFont(PERSIAN_FONT_PATH, PERSIAN_SIZE);

    private static boolean containsArabic(String text) {
        if (text == null || text.isEmpty()) return false;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);

            if (
                    (cp >= 0x0600 && cp <= 0x06FF) || // Arabic
                            (cp >= 0x0750 && cp <= 0x077F) || // Arabic Supplement
                            (cp >= 0x08A0 && cp <= 0x08FF) || // Arabic Extended-A
                            (cp >= 0xFB50 && cp <= 0xFDFF) || // Arabic Presentation Forms-A
                            (cp >= 0xFE70 && cp <= 0xFEFF)    // Arabic Presentation Forms-B
            ) {
                return true;
            }
        }

        return false;
    }

    private static BladeFont chooseUiFont(String text) {
        return containsArabic(text) ? PERSIAN : UI;
    }

    private static BladeFont chooseTitleFont(String text) {
        return containsArabic(text) ? PERSIAN : TITLE;
    }

    public static void drawUi(DrawContext ctx, String text, float x, float y, int color) {
        chooseUiFont(text).draw(ctx, text, x, y, color, UI_SIZE, false);
    }

    public static void drawUi(DrawContext ctx, String text, float x, float y, int color, float size, boolean shadow) {
        chooseUiFont(text).draw(ctx, text, x, y, color, size, shadow);
    }

    public static void drawUiCentered(DrawContext ctx, String text, float cx, float cy, int color, float size, boolean shadow) {
        chooseUiFont(text).drawCentered(ctx, text, cx, cy, color, size, shadow);
    }

    public static int uiWidth(String text, float size) {
        return chooseUiFont(text).width(text, size);
    }

    public static int uiHeight(String text, float size) {
        return chooseUiFont(text).height(text, size);
    }

    public static void drawTitle(DrawContext ctx, String text, float x, float y, int color, float size, boolean shadow) {
        chooseTitleFont(text).draw(ctx, text, x, y, color, size, shadow);
    }

    public static void drawTitleCentered(DrawContext ctx, String text, float cx, float cy, int color, float size, boolean shadow) {
        chooseTitleFont(text).drawCentered(ctx, text, cx, cy, color, size, shadow);
    }

    public static int titleWidth(String text, float size) {
        return chooseTitleFont(text).width(text, size);
    }

    public static int titleHeight(String text, float size) {
        return chooseTitleFont(text).height(text, size);
    }

    public static void shutdown() {
        try {
            UI.close();
        } catch (Throwable ignored) {
        }
        try {
            TITLE.close();
        } catch (Throwable ignored) {
        }
        try {
            PERSIAN.close();
        } catch (Throwable ignored) {
        }
    }
}