package com.ishland.earlyloadingscreen.render.simpledraw;

import java.util.Arrays;

import static org.lwjgl.opengl.GL32.*;

public class SimpleDraw {

    public final Shader[] shaders;

    public SimpleDraw() {
        shaders = Arrays.stream(VertexFormat.values()).map(Shader::new).toArray(Shader[]::new);
    }

    public void viewport2D(int width, int height) {
        // update projection: top-left is (0, 0)
        for (Shader shader : shaders) {
            glUseProgram(shader.program);
            glUniformMatrix4fv(shader.projectionUniformLocation, false, new float[] {
                    2f / width, 0, 0, 0,
                    0, -2f / height, 0, 0,
                    0, 0, 1, 0,
                    -1, 1, 0, 1,
            });
        }
        glUseProgram(0);
    }

    public Shader getShader(VertexFormat format) {
        return shaders[format.ordinal()];
    }

    public void destroy() {
        for (Shader shader : shaders) {
            glDeleteProgram(shader.program);
        }
    }

}
