package com.ishland.earlyloadingscreen.patch;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;

public class SodiumOSDetectionPatch implements BytecodeTransformer {

    private static final SodiumOSDetectionPatch INSTANCE = new SodiumOSDetectionPatch();

    private static final String[] SODIUM_WORKAROUNDS_CLASSES = new String[] {
            "me/jellysquid/mods/sodium/client/util/workarounds/Workarounds",
            "me/jellysquid/mods/sodium/client/compatibility/workarounds/Workarounds",
            "net/caffeinemc/mods/sodium/client/compatibility/workarounds/Workarounds",
            "me/jellysquid/mods/sodium/client/util/workarounds/driver/nvidia/NvidiaWorkarounds",
            "me/jellysquid/mods/sodium/client/compatibility/workarounds/nvidia/NvidiaWorkarounds",
            "net/caffeinemc/mods/sodium/client/compatibility/workarounds/nvidia/NvidiaWorkarounds",
            "me/jellysquid/mods/sodium/client/util/workarounds/driver/nvidia/NvidiaWorkarounds$1",
            "me/jellysquid/mods/sodium/client/compatibility/workarounds/nvidia/NvidiaWorkarounds$1",
            "net/caffeinemc/mods/sodium/client/compatibility/workarounds/nvidia/NvidiaWorkarounds$1",
            "me/jellysquid/mods/sodium/client/util/workarounds/probe/GraphicsAdapterProbe",
            "me/jellysquid/mods/sodium/client/compatibility/environment/probe/GraphicsAdapterProbe",
            "net/caffeinemc/mods/sodium/client/compatibility/environment/probe/GraphicsAdapterProbe",
    };

    public static final boolean INITIALIZED;

    private static final String UTIL_OS = "net/minecraft/Util$OperatingSystem";
    private static final String UTIL = "net/minecraft/Util";

    private SodiumOSDetectionPatch() {
    }

    static {
        INITIALIZED = initTransformer();
    }

    public static void init() {
    }

    private static boolean initTransformer() {
        Instrumentation inst = PatchUtil.instrumentation;
        if (inst == null) {
            SharedConstants.LOGGER.warn("Instrumentation unavailable, sodium workarounds will not be available");
            LoadingProgressManager.showMessageAsProgress("Instrumentation unavailable, sodium workarounds may not be available");
            return false;
        }
        PatchUtil.transformers.add(INSTANCE);
        if (!Arrays.stream(SODIUM_WORKAROUNDS_CLASSES).allMatch(name -> tryRetransform(inst, name))) {
            SharedConstants.LOGGER.warn("Failed to retransform sodium workarounds, sodium workarounds will not be available");
            LoadingProgressManager.showMessageAsProgress("Failed to retransform sodium workarounds, sodium workarounds may not be available");
            PatchUtil.transformers.remove(INSTANCE);
            for (String s : SODIUM_WORKAROUNDS_CLASSES) {
                tryRetransform(inst, s);
            }
            return false;
        }
        return true;
    }

    private static boolean tryRetransform(Instrumentation inst, String name) {
        try {
            inst.retransformClasses(Class.forName(name.replace('/', '.')));
            return true;
        } catch (ClassNotFoundException e) {
            return true;
        } catch (Throwable t) {
            SharedConstants.LOGGER.warn("Failed to retransform class %s".formatted(name), t);
            return false;
        }
    }

    @Override
    public boolean transform(String className, ClassNode node) {
        if (Arrays.asList(SODIUM_WORKAROUNDS_CLASSES).contains(className)) {
            SharedConstants.LOGGER.info("Patching %s to allow early usage".formatted(className));

            for (MethodNode method : node.methods) {

                {
                    final Iterator<LocalVariableNode> iterator = method.localVariables.iterator();

                    while (iterator.hasNext()) {
                        final LocalVariableNode localVariableNode = iterator.next();

                        if (localVariableNode.desc.equals(String.format("L%s;", UTIL_OS))) {
                            localVariableNode.desc = "Lcom/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem;";
                        }
                    }
                }

                method.desc = method.desc.replace(String.format("L%s;", UTIL_OS), "Lcom/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem;");
                if (method.signature != null) {
                    method.signature = method.signature.replace(String.format("L%s;", UTIL_OS), "Lcom/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem;");
                }

                {
                    final ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

                    while (iterator.hasNext()) {
                        final AbstractInsnNode insn = iterator.next();

                        if (insn instanceof MethodInsnNode methodInsnNode) {
                            // replace Util.getOperatingSystem() with OSDetectionUtil.getOperatingSystem()
                            if (methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC &&
                                methodInsnNode.owner.equals(UTIL) &&
                                methodInsnNode.name.equals("getOperatingSystem") &&
                                methodInsnNode.desc.equals("()L" + UTIL_OS + ";")) {
                                methodInsnNode.owner = "com/ishland/earlyloadingscreen/util/OSDetectionUtil";
                                methodInsnNode.name = "getOperatingSystem";
                                methodInsnNode.desc = "()Lcom/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem;";
                            }

                            // replace Util$OperatingSystem.ordinal() with OSDetectionUtil$OperatingSystem.ordinal()
                            if (methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL &&
                                methodInsnNode.owner.equals(UTIL_OS) &&
                                methodInsnNode.name.equals("ordinal") &&
                                methodInsnNode.desc.equals("()I")) {
                                methodInsnNode.owner = "com/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem";
                            }

                            // replace Util$OperatingSystem.values() with OSDetectionUtil$OperatingSystem.values()
                            if (methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC &&
                                methodInsnNode.owner.equals(UTIL_OS) &&
                                methodInsnNode.name.equals("values") &&
                                methodInsnNode.desc.equals("()[L" + UTIL_OS + ";")) {
                                methodInsnNode.owner = "com/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem";
                                methodInsnNode.desc = "()[Lcom/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem;";
                            }

                            methodInsnNode.desc = methodInsnNode.desc.replace(String.format("L%s;", UTIL_OS), "Lcom/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem;");
                        }

                        if (insn instanceof FieldInsnNode fieldInsnNode) {
                            // replace Util$OperatingSystem with OSDetectionUtil$OperatingSystem
                            if (fieldInsnNode.getOpcode() == Opcodes.GETSTATIC &&
                                fieldInsnNode.owner.equals(UTIL_OS) &&
                                fieldInsnNode.desc.equals("L" + UTIL_OS + ";")) {
                                fieldInsnNode.owner = "com/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem";
                                fieldInsnNode.desc = "Lcom/ishland/earlyloadingscreen/util/OSDetectionUtil$OperatingSystem;";
                                if (fieldInsnNode.name.equals("LINUX")) {
                                    fieldInsnNode.name = "LINUX";
                                } else if (fieldInsnNode.name.equals("SOLARIS")) {
                                    fieldInsnNode.name = "SOLARIS";
                                } else if (fieldInsnNode.name.equals("WINDOWS")) {
                                    fieldInsnNode.name = "WINDOWS";
                                } else if (fieldInsnNode.name.equals("OSX")) {
                                    fieldInsnNode.name = "OSX";
                                } else if (fieldInsnNode.name.equals("UNKNOWN")) {
                                    fieldInsnNode.name = "UNKNOWN";
                                } else {
                                    throw new RuntimeException("Unknown field name: " + fieldInsnNode.name);
                                }
                            }
                        }
                    }
                }

            }


            return true;
        }


        return false;
    }
}
