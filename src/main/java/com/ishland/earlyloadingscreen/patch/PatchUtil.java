package com.ishland.earlyloadingscreen.patch;

import com.ishland.earlyloadingscreen.SharedConstants;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.commons.io.file.PathUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.concurrent.CopyOnWriteArrayList;

public class PatchUtil {

    private static final Path transformerOutputPath = Path.of(".", ".earlyloadingscreen-transformer-output");

    static final CopyOnWriteArrayList<BytecodeTransformer> transformers = new CopyOnWriteArrayList<>();
    static final Instrumentation instrumentation;

    static {
        if (Files.isDirectory(transformerOutputPath)) {
            try {
                PathUtils.deleteDirectory(transformerOutputPath);
            } catch (IOException e) {
                SharedConstants.LOGGER.warn("Failed to delete transformer output directory", e);
            }
        }
        try {
            Files.createDirectories(transformerOutputPath);
        } catch (IOException e) {
            SharedConstants.LOGGER.warn("Failed to create transformer output directory", e);
        }

        Instrumentation inst = null;
        try {
            inst = ByteBuddyAgent.install();
        } catch (Throwable t) {
            SharedConstants.LOGGER.warn("Failed to install ByteBuddyAgent, patching will not work", t);
        }
        instrumentation = inst;
        if (inst != null) {
            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    try {
                        ClassNode node = new ClassNode();
                        new ClassReader(classfileBuffer).accept(node, 0);
                        boolean transformed = false;
                        for (BytecodeTransformer transformer : transformers) {
                            transformed |= transformer.transform(className, node);
                        }
                        if (transformed) {
                            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                            node.accept(writer);
                            final byte[] buf = writer.toByteArray();
                            try {
                                final Path path = transformerOutputPath.resolve(className + ".class");
                                Files.createDirectories(path.getParent());
                                Files.write(path, buf);
                            } catch (Throwable t) {
                                SharedConstants.LOGGER.warn("Failed to write transformed class %s to disk".formatted(className), t);
                            }
                            return buf;
                        } else {
                            return null;
                        }
                    } catch (Throwable t) {
                        SharedConstants.LOGGER.warn("Failed to transform class " + className, t);
                        return null;
                    }
                }
            }, true);
        }
    }


}
