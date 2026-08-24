package ir.modernshadow.bladeclient.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class BladeFont implements AutoCloseable {

    // کیفیت بیشتر
    private static final int BAKED_SCALE = 4;
    private static final int GLYPH_PADDING = 4;

    private final Identifier texturePrefix;
    private final Font awtFont;
    private final float baseSize;
    private final int lineHeight;
    private final int spaceAdvance;

    private final Map<Integer, Glyph> glyphCache = new ConcurrentHashMap<>();

    public BladeFont(String resourcePath, float size) {

        this.texturePrefix = Identifier.of(
                "bladeclient",
                "font/" +
                        Integer.toHexString(
                                Objects.requireNonNull(resourcePath).hashCode()
                        ) +
                        "_" +
                        Integer.toHexString(Float.floatToIntBits(size))
        );

        this.baseSize = size * BAKED_SCALE;

        this.awtFont = loadFont(resourcePath, this.baseSize);

        int[] metrics = measureFontMetrics(this.awtFont);

        this.lineHeight = metrics[0];
        this.spaceAdvance = metrics[1];
    }

    private static Font loadFont(String path, float size) {

        try (InputStream in = BladeFont.class.getResourceAsStream(path)) {

            if (in == null) {
                throw new IllegalStateException("Font not found: " + path);
            }

            Font base = Font.createFont(Font.TRUETYPE_FONT, in);

            return base.deriveFont(Font.PLAIN, size);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int[] measureFontMetrics(Font font) {

        BufferedImage img =
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();

        g.setFont(font);

        applyQuality(g);

        FontMetrics fm = g.getFontMetrics();

        int h = fm.getHeight();
        int s = fm.stringWidth(" ");

        g.dispose();

        return new int[]{h, s};
    }

    private static void applyQuality(Graphics2D g) {

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        g.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON
        );
    }

    public void draw(
            DrawContext ctx,
            String text,
            float x,
            float y,
            int color,
            float size,
            boolean shadow
    ) {

        if (text == null || text.isEmpty()) return;

        float targetScale =
                size / (this.baseSize / BAKED_SCALE);

        if (!(targetScale > 0f)) return;

        // حذف کامل سایه
        shadow = false;

        Matrix3x2fStack matrices = ctx.getMatrices();

        matrices.pushMatrix();

        float scale = targetScale / BAKED_SCALE;

        matrices.scale(scale, scale);

        float inv = 1f / scale;

        float penX = x * inv;
        float penY = y * inv;

        for (int i = 0; i < text.length(); ) {

            int cp = text.codePointAt(i);

            i += Character.charCount(cp);

            if (cp == '\n') {
                penX = x * inv;
                penY += lineHeight;
                continue;
            }

            Glyph glyph =
                    glyphCache.computeIfAbsent(cp, this::createGlyph);

            if (glyph == null) continue;

            ctx.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    glyph.textureId,
                    Math.round(penX),
                    Math.round(penY),
                    0,
                    0,
                    glyph.width,
                    glyph.height,
                    glyph.width,
                    glyph.height,
                    color
            );

            penX += glyph.advance;
        }

        matrices.popMatrix();
    }

    public void drawCentered(
            DrawContext ctx,
            String text,
            float cx,
            float cy,
            int color,
            float size,
            boolean shadow
    ) {

        int w = width(text, size);
        int h = height(text, size);

        draw(
                ctx,
                text,
                cx - w / 2f,
                cy - h / 2f,
                color,
                size,
                false
        );
    }

    public int width(String text, float size) {

        if (text == null || text.isEmpty()) return 0;

        float scale =
                (size / (this.baseSize / BAKED_SCALE)) / BAKED_SCALE;

        float width = 0;

        for (int i = 0; i < text.length(); ) {

            int cp = text.codePointAt(i);

            i += Character.charCount(cp);

            Glyph glyph =
                    glyphCache.computeIfAbsent(cp, this::createGlyph);

            if (glyph != null) {
                width += glyph.advance;
            }
        }

        return Math.round(width * scale);
    }

    public int height(String text, float size) {

        float scale =
                (size / (this.baseSize / BAKED_SCALE)) / BAKED_SCALE;

        return Math.round(lineHeight * scale);
    }

    private Glyph createGlyph(int codePoint) {

        try {

            String s =
                    new String(Character.toChars(codePoint));

            BufferedImage tmp =
                    new BufferedImage(
                            32,
                            32,
                            BufferedImage.TYPE_INT_ARGB
                    );

            Graphics2D mg = tmp.createGraphics();

            mg.setFont(awtFont);

            applyQuality(mg);

            FontMetrics fm = mg.getFontMetrics();

            int advance = fm.stringWidth(s);

            int width =
                    Math.max(1, advance + GLYPH_PADDING * 2);

            int height =
                    Math.max(1, fm.getHeight() + GLYPH_PADDING * 2);

            mg.dispose();

            BufferedImage image =
                    new BufferedImage(
                            width,
                            height,
                            BufferedImage.TYPE_INT_ARGB
                    );

            Graphics2D g = image.createGraphics();

            g.setFont(awtFont);

            applyQuality(g);

            g.setColor(Color.WHITE);

            g.drawString(
                    s,
                    GLYPH_PADDING,
                    GLYPH_PADDING + fm.getAscent()
            );

            g.dispose();

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            javax.imageio.ImageIO.write(image, "png", out);

            NativeImage nativeImage;

            try (ByteArrayInputStream in =
                         new ByteArrayInputStream(out.toByteArray())) {

                nativeImage = NativeImage.read(in);
            }

            Identifier textureId =
                    Identifier.of(
                            "bladeclient",
                            texturePrefix.getPath() +
                                    "/" +
                                    Integer.toHexString(codePoint)
                    );

            NativeImageBackedTexture texture =
                    new NativeImageBackedTexture(
                            () -> textureId.toString(),
                            nativeImage
                    );

            // کیفیت بهتر
            texture.setFilter(true, true);

            MinecraftClient.getInstance()
                    .getTextureManager()
                    .registerTexture(textureId, texture);

            return new Glyph(
                    textureId,
                    texture,
                    width,
                    height,
                    advance
            );

        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void close() {

        for (Glyph glyph : glyphCache.values()) {

            try {
                glyph.close();
            } catch (Throwable ignored) {
            }
        }

        glyphCache.clear();
    }

    private static final class Glyph implements AutoCloseable {

        private final Identifier textureId;
        private final NativeImageBackedTexture texture;

        private final int width;
        private final int height;
        private final int advance;

        private Glyph(
                Identifier textureId,
                NativeImageBackedTexture texture,
                int width,
                int height,
                int advance
        ) {

            this.textureId = textureId;
            this.texture = texture;

            this.width = width;
            this.height = height;
            this.advance = advance;
        }

        @Override
        public void close() {

            if (texture != null) {
                texture.close();
            }
        }
    }
}