package com.ishland.earlyloadingscreen.render.simpledraw;

import static org.lwjgl.opengl.GL32.*;

public class Shader {

    public final VertexFormat format;
    public final int program;
    public final int projectionUniformLocation;
    public final int textureUniformLocation;

    public Shader(VertexFormat format) {
        this.format = format;
        program = ShaderUtils.createProgram(format);
        projectionUniformLocation = glGetUniformLocation(program, "projection");
        textureUniformLocation = format.hasTex ? glGetUniformLocation(program, "tex") : -1;
    }

    public void bind() {
        glUseProgram(program);
    }

    public void unbind() {
        glUseProgram(0);
    }

}
