package com.ishland.earlyloadingscreen.render.simpledraw;

import static org.lwjgl.opengl.GL32.*;

public class ShaderUtils {

    public static int createProgram(VertexFormat format) {
        int program = glCreateProgram();
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, format.vertexShaderSource);
        glCompileShader(vertexShader);
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
            final RuntimeException e = new RuntimeException("Failed to compile vertex shader: " + glGetShaderInfoLog(vertexShader));
            glDeleteShader(vertexShader);
            throw e;
        }
        glAttachShader(program, vertexShader);
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, format.fragmentShaderSource);
        glCompileShader(fragmentShader);
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
            final RuntimeException e = new RuntimeException("Failed to compile fragment shader: " + glGetShaderInfoLog(fragmentShader));
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            throw e;
        }
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE) {
            final RuntimeException e = new RuntimeException("Failed to link program: " + glGetProgramInfoLog(program));
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            throw e;
        }
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return program;
    }

}
