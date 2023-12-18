package com.ishland.earlyloadingscreen.render;

import static org.lwjgl.opengl.GL32.*;

public class Simple2DDraw {

    private final int shaderProgram;
    private final int projectionUniformLocation;

    public Simple2DDraw() {

        // format: pos, color

        final int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, """
                #version 150 core
                in vec2 position;
                in vec4 color;
                
                uniform mat4 projection;
                
                out vec4 fColor;
                
                void main() {
                    gl_Position = projection * vec4(position, 0.0, 1.0);
                    fColor = color;
                }
                """);
        glCompileShader(vertexShader);
        final int[] success = new int[1];
        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, success);
        if (success[0] == 0) {
            RuntimeException e = new RuntimeException("Failed to compile vertex shader: " + glGetShaderInfoLog(vertexShader));
            glDeleteShader(vertexShader);
            throw e;
        }

        final int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, """
                #version 150 core
                in vec4 fColor;
                
                out vec4 FragColor;
                
                void main() {
                    if (fColor.a < 0.01)
                        discard;
                    FragColor = fColor;
                }
                """);
        glCompileShader(fragmentShader);
        glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, success);
        if (success[0] == 0) {
            RuntimeException e = new RuntimeException("Failed to compile fragment shader: " + glGetShaderInfoLog(fragmentShader));
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            throw e;
        }

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        glGetProgramiv(shaderProgram, GL_LINK_STATUS, success);
        if (success[0] == 0) {
            RuntimeException e = new RuntimeException("Failed to link shader program: " + glGetProgramInfoLog(shaderProgram));
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(shaderProgram);
            throw e;
        }
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        projectionUniformLocation = glGetUniformLocation(shaderProgram, "projection");
    }

    public void viewport(int width, int height) {
        // update projection: top-left is (0, 0)
        glUseProgram(shaderProgram);
        glUniformMatrix4fv(projectionUniformLocation, false, new float[] {
                2f / width, 0, 0, 0,
                0, -2f / height, 0, 0,
                0, 0, 1, 0,
                -1, 1, 0, 1
        });
        glUseProgram(0);
    }

    public void destroy() {
        glDeleteProgram(shaderProgram);
    }

    public class BufferBuilder {

        private static final int INITIAL_SIZE = 1024;
        private float[] buffer = new float[INITIAL_SIZE];
        private final int vbo, vao;
        private boolean destroyed = false;
        private boolean building = false;
        private int pos = 0;

        public BufferBuilder() {
            vbo = glGenBuffers();
            vao = glGenVertexArrays();
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 6 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 4, GL_FLOAT, false, 6 * Float.BYTES, 2 * Float.BYTES);
            glEnableVertexAttribArray(1);
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
            pos = 0;
            return this;
        }

        public BufferBuilder vertex(float x, float y, float r, float g, float b, float a) {
            ensureCapacity(pos + 6);
            buffer[pos ++] = x;
            buffer[pos ++] = y;
            buffer[pos ++] = r;
            buffer[pos ++] = g;
            buffer[pos ++] = b;
            buffer[pos ++] = a;
            return this;
        }

        public BufferBuilder triangle(float x1, float y1, float x2, float y2, float x3, float y3, float r, float g, float b, float a) {
            ensureCapacity(pos + 18);
            this.vertex(x1, y1, r, g, b, a);
            this.vertex(x2, y2, r, g, b, a);
            this.vertex(x3, y3, r, g, b, a);
            return this;
        }

        public BufferBuilder rect(float x, float y, float width, float height, float r, float g, float b, float a) {
            ensureCapacity(pos + 24);
            this.triangle(x, y, x, y + height, x + width, y, r, g, b, a);
            this.triangle(x + width, y + height, x + width, y, x, y + height, r, g, b, a);
            return this;
        }

        public void end() {
            if (destroyed) throw new IllegalStateException("Already destroyed");
            if (!building) throw new IllegalStateException("Not building");
            building = false;
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        public void draw() {
            if (destroyed) throw new IllegalStateException("Already destroyed");
            if (building) throw new IllegalStateException("Still building");
            glUseProgram(shaderProgram);
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, pos / 6);
            glBindVertexArray(0);
            glUseProgram(0);
        }

        public void destroy() {
            glDeleteBuffers(vbo);
            glDeleteVertexArrays(vao);
            destroyed = true;
        }

    }

}
