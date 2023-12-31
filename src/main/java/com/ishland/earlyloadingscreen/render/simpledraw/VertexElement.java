package com.ishland.earlyloadingscreen.render.simpledraw;

public enum VertexElement {

    POSITION("aPos", 3),
    COLOR("aColor", 4),
    TEXTURE("aTexCoord", 2),
    ;

    public final String name;
    public final int size;

    VertexElement(String name, int size) {
        this.name = name;
        this.size = size;
    }

}
