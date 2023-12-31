package com.ishland.earlyloadingscreen.render.simpledraw;

import static org.lwjgl.opengl.GL32.*;

public class BufferBuilder {

    private final Shader shader;
    private final VertexFormat format;
    private final int drawMode;
    private static final int INITIAL_SIZE = 1024;
    private float[] buffer = new float[INITIAL_SIZE];
    private final int vbo, vao;
    private boolean destroyed = false;
    private boolean building = false;
    private int bufPos = 0;
    private int currentElement = 0;

    public BufferBuilder(Shader shader, int drawMode) {
        this.shader = shader;
        this.format = shader.format;
        this.drawMode = drawMode;
        vbo = glGenBuffers();
        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        for (int i = 0; i < format.elements.length; i ++) {
            glVertexAttribPointer(i, format.elements[i].size, GL_FLOAT, false, format.stride * Float.BYTES, (long) format.offsets[i] * Float.BYTES);
            glEnableVertexAttribArray(i);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void ensureCapacity(int capacity) {
        if (buffer.length < capacity) {
            float[] newBuffer = new float[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }
    }

    public BufferBuilder begin() {
        if (building) throw new IllegalStateException("Already building");
        building = true;
        bufPos = 0;
        currentElement = 0;
        return this;
    }

    public BufferBuilder pos(float x, float y, float z) {
        if (!building) throw new IllegalStateException("Not building");
        if (currentElement >= format.elements.length) throw new IllegalStateException("No more elements");
        if (format.elements[currentElement] != VertexElement.POSITION) throw new IllegalStateException("Not a position element");
        ensureCapacity(bufPos + 3);
        buffer[bufPos ++] = x;
        buffer[bufPos ++] = y;
        buffer[bufPos ++] = z;
        currentElement ++;
        return this;
    }

    public BufferBuilder pos(float x, float y) {
        return pos(x, y, 0);
    }

    public BufferBuilder color(float r, float g, float b, float a) {
        if (!building) throw new IllegalStateException("Not building");
        if (currentElement >= format.elements.length) throw new IllegalStateException("No more elements");
        if (format.elements[currentElement] != VertexElement.COLOR) throw new IllegalStateException("Not a color element");
        ensureCapacity(bufPos + 4);
        buffer[bufPos ++] = r;
        buffer[bufPos ++] = g;
        buffer[bufPos ++] = b;
        buffer[bufPos ++] = a;
        currentElement ++;
        return this;
    }

    public BufferBuilder tex(float u, float v) {
        if (!building) throw new IllegalStateException("Not building");
        if (currentElement >= format.elements.length) throw new IllegalStateException("No more elements");
        if (format.elements[currentElement] != VertexElement.TEXTURE) throw new IllegalStateException("Not a texture element");
        ensureCapacity(bufPos + 2);
        buffer[bufPos ++] = u;
        buffer[bufPos ++] = v;
        currentElement ++;
        return this;
    }

    public BufferBuilder next() {
        if (!building) throw new IllegalStateException("Not building");
        if (currentElement != format.elements.length) throw new IllegalStateException("Not enough elements");
        currentElement = 0;
        return this;
    }

    public BufferBuilder upload() {
        if (!building) throw new IllegalStateException("Not building");
        if (currentElement != 0) throw new IllegalStateException("Not enough elements");
        building = false;
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return this;
    }

    public void draw() {
        if (building) throw new IllegalStateException("Still building");
        if (destroyed) throw new IllegalStateException("Already destroyed");
        shader.bind();
        glBindVertexArray(vao);
        if (bufPos % format.stride != 0) throw new AssertionError("Invalid buffer size");
        glDrawArrays(GL_TRIANGLES, 0, bufPos / format.stride);
        glBindVertexArray(0);
        shader.unbind();
    }

}
