package com.ishland.earlyloadingscreen.render.simpledraw;

import java.util.Arrays;

public enum VertexFormat {
    POS_COLOR(
            """
            #version 150 core
            in vec3 aPos;
            in vec4 aColor;
            
            uniform mat4 projection;
            
            out vec4 fColor;
            
            void main() {
                gl_Position = projection * vec4(aPos, 1.0);
                fColor = aColor;
            }
            """,
            """
            #version 150 core
            in vec4 fColor;
            
            out vec4 FragColor;
            
            void main() {
                if (fColor.a < 0.01)
                    discard;
                FragColor = fColor;
            }
            """,
            new VertexElement[] {
                    VertexElement.POSITION,
                    VertexElement.COLOR,
            }
    ),
    POS_TEX(
            """
            #version 150 core
            in vec3 aPos;
            in vec2 aTexCoord;
            
            uniform mat4 projection;
            
            out vec2 fTexCoord;
            
            void main() {
                gl_Position = projection * vec4(aPos, 1.0);
                fTexCoord = aTexCoord;
            }
            """,
            """
            #version 150 core
            in vec2 fTexCoord;
            
            out vec4 FragColor;
            
            uniform sampler2D tex;
            
            void main() {
                FragColor = texture(tex, fTexCoord);
            }
            """,
            new VertexElement[] {
                    VertexElement.POSITION,
                    VertexElement.TEXTURE,
            }
    )
    ;

    public final String vertexShaderSource;
    public final String fragmentShaderSource;
    public final VertexElement[] elements;
    public final boolean hasTex;
    public final int stride;
    public final int[] offsets;

    VertexFormat(String vertexShaderSource, String fragmentShaderSource, VertexElement[] elements) {
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
        this.elements = elements;
        this.hasTex = Arrays.stream(elements).anyMatch(e -> e == VertexElement.TEXTURE);
        this.stride = Arrays.stream(elements).mapToInt(e -> e.size).sum();
        this.offsets = new int[elements.length];
        int offset = 0;
        for (int i = 0; i < elements.length; i ++) {
            offsets[i] = offset;
            offset += elements[i].size;
        }
    }
}
