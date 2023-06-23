package com.ishland.earlyloadingscreen.patch;

import org.objectweb.asm.tree.ClassNode;

public interface BytecodeTransformer {

    boolean transform(String className, ClassNode node);

}
