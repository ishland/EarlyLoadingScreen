package com.ishland.earlyloadingscreen.render.simpledraw;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL32.*;

public class Texture {

    private final int texture;
    public final int width;
    public final int height;

    public Texture(byte[] read) {
        final int[] x = new int[1];
        final int[] y = new int[1];
        final int[] channelsInFile = new int[1];
        final ByteBuffer nativeBuf = MemoryUtil.memAlloc(read.length);
        nativeBuf.put(read);
        nativeBuf.position(0);
        final ByteBuffer buffer = STBImage.stbi_load_from_memory(nativeBuf, x, y, channelsInFile, 3);
        MemoryUtil.memFree(nativeBuf);
        if (buffer == null) throw new RuntimeException("Failed to load texture: " + STBImage.stbi_failure_reason());
        this.width = x[0];
        this.height = y[0];
        this.texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[] {0, 0, 0, 0});
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, x[0], y[0], 0, GL_RGB, GL_UNSIGNED_BYTE, buffer);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        STBImage.stbi_image_free(buffer);
    }

    public Texture(Path path) throws IOException {
        this(Files.readAllBytes(path));
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, texture);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void destroy() {
        glDeleteTextures(texture);
    }

}
