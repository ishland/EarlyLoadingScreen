package com.ishland.earlyloadingscreen.render.simpledraw;

public class RenderUtils {

    public static void rectPosColorTriangles(BufferBuilder builder, float x, float y, float width, float height, float r, float g, float b, float a) {
        builder
                .pos(x, y).color(r, g, b, a).next()
                .pos(x + width, y + height).color(r, g, b, a).next()
                .pos(x + width, y).color(r, g, b, a).next()
                .pos(x, y).color(r, g, b, a).next()
                .pos(x, y + height).color(r, g, b, a).next()
                .pos(x + width, y + height).color(r, g, b, a).next();
    }

    public static void rectPosTexTriangles(BufferBuilder builder, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight) {
        // for x and y, top-left is origin
        // for u and v, top-left is origin
        builder
                .pos(x, y).tex(u, v).next()
                .pos(x + width, y + height).tex(u + uWidth, v + vHeight).next()
                .pos(x + width, y).tex(u + uWidth, v).next()
                .pos(x, y).tex(u, v).next()
                .pos(x, y + height).tex(u, v + vHeight).next()
                .pos(x + width, y + height).tex(u + uWidth, v + vHeight).next();

    }

    public static void textureFillScreen(BufferBuilder bufferBuilder, Texture tex, int width, int height) {
        // keep aspect ratio, fill the screen
        float aspectRatio = (float) tex.width / tex.height;
        float screenAspectRatio = (float) width / height;
        float x, y, w, h;
        if (aspectRatio > screenAspectRatio) {
            // image is wider than screen
            w = height * aspectRatio;
            h = height;
            x = (width - w) / 2;
            y = 0;
        } else {
            // image is taller than screen
            w = width;
            h = width / aspectRatio;
            x = 0;
            y = (height - h) / 2;
        }
        rectPosTexTriangles(bufferBuilder, x, y, w, h, 0, 0, 1, 1);
    }

}
