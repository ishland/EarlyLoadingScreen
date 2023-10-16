package com.ishland.earlyloadingscreen.patch;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport;
import com.ishland.earlyloadingscreen.util.AppLoaderUtil;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class FabricLoaderInvokePatch implements BytecodeTransformer {

    private static final FabricLoaderInvokePatch INSTANCE = new FabricLoaderInvokePatch();

    private FabricLoaderInvokePatch() {
    }

    static {
        initTransformer();
    }

    public static void init() {
        // intentionally empty, just to trigger static initialization
    }

    private static void initTransformer() {
        Instrumentation inst = PatchUtil.instrumentation;
        if (inst == null) {
            SharedConstants.LOGGER.warn("Instrumentation unavailable, entrypoint information will not be available");
            LoadingProgressManager.showMessageAsProgress("Instrumentation unavailable, entrypoint information will not be available");
            return;
        }
        try {
            updateAppLoaderAccess(inst);
        } catch (Throwable t) {
            SharedConstants.LOGGER.warn("Failed to update AppLoader access", t);
        }
        try {
            AppLoaderUtil.init();
        } catch (Throwable t) {
            SharedConstants.LOGGER.warn("Failed to define classes on AppClassLoader, entrypoint information will not be available", t);
            LoadingProgressManager.showMessageAsProgress("Failed to define classes on AppClassLoader, entrypoint information will not be available");
            return;
        }
        PatchUtil.transformers.add(INSTANCE);
        try {
            inst.retransformClasses(Class.forName("net.fabricmc.loader.impl.launch.knot.KnotClassDelegate"));
            inst.retransformClasses(FabricLoaderImpl.class);
        } catch (Throwable t) {
            SharedConstants.LOGGER.warn("Failed to retransform FabricLoaderImpl, attempting to revert changes", t);
            LoadingProgressManager.showMessageAsProgress("Failed to retransform EntrypointUtils, entrypoint information will not be available");
            PatchUtil.transformers.remove(INSTANCE);
            try {
                inst.retransformClasses(Class.forName("net.fabricmc.loader.impl.launch.knot.KnotClassDelegate"));
            } catch (Throwable t2) {
                SharedConstants.LOGGER.warn("Failed to revert changes to EntrypointUtils", t2);
            }
            try {
                inst.retransformClasses(FabricLoaderImpl.class);
            } catch (Throwable t2) {
                SharedConstants.LOGGER.warn("Failed to revert changes to FabricLoaderImpl", t2);
            }
        }
    }

    private static void updateAppLoaderAccess(Instrumentation inst) {
        inst.redefineModule(
                ModuleLayer.boot().findModule("java.base").get(),
                Set.of(),
                Map.of(),
                Map.of("java.lang", Set.of(PatchUtil.class.getModule())),
                Set.of(),
                Map.of()
        );
    }

    @Override
    public boolean transform(String className, ClassNode node) {

        // patch net/fabricmc/loader/impl/FabricLoaderImpl
        if (className.equals("net/fabricmc/loader/impl/FabricLoaderImpl")) {
            SharedConstants.LOGGER.info("Patching FabricLoaderImpl for entrypoint information");

            for (MethodNode method : node.methods) {
                // Lnet/fabricmc/loader/impl/FabricLoaderImpl;invokeEntrypoints(Ljava/lang/String;Ljava/lang/Class;Ljava/util/function/Consumer;)V
                if (method.name.equals("invokeEntrypoints") && method.desc.equals("(Ljava/lang/String;Ljava/lang/Class;Ljava/util/function/Consumer;)V")) {
                    SharedConstants.LOGGER.info("Patching FabricLoaderImpl.invokeEntrypoints");

                    // add local var for progress tracker
                    // use the same range as the first local var
                    final LocalVariableNode firstLocalVar = method.localVariables.get(0); // var0 is `this`
                    final int progressTrackerIndex = method.maxLocals++;
                    method.visitLocalVariable(
                            "early_loading_screen$progressTracker",
                            Type.getDescriptor(AppLoaderAccessSupport.ProgressHolderAccessor.class),
                            null,
                            firstLocalVar.start.getLabel(),
                            firstLocalVar.end.getLabel(),
                            progressTrackerIndex
                    );

                    int iteratorVarIndex = -1;
                    int listVarIndex = -1;

                    final ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
                    while (iterator.hasNext()) {
                        final AbstractInsnNode insn = iterator.next();

                        if (insn instanceof MethodInsnNode methodInsnNode) {

                            // wrap getEntryPointContainers return value with ArrayList
                            // target insn: INVOKEVIRTUAL net/fabricmc/loader/impl/FabricLoaderImpl.getEntrypointContainers (Ljava/lang/String;Ljava/lang/Class;)Ljava/util/List;
                            if (methodInsnNode.owner.equals("net/fabricmc/loader/impl/FabricLoaderImpl") && methodInsnNode.name.equals("getEntrypointContainers") && methodInsnNode.desc.equals("(Ljava/lang/String;Ljava/lang/Class;)Ljava/util/List;")) {
                                // add invoke after getEntrypointContainers
                                // Lcom/google/common/collect/Lists;newArrayList(Ljava/lang/Iterable;)Ljava/util/ArrayList;

                                iterator.add(new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "com/google/common/collect/Lists",
                                        "newArrayList",
                                        "(Ljava/lang/Iterable;)Ljava/util/ArrayList;",
                                        false
                                ));

                                // store the list
                                final AbstractInsnNode next = iterator.next();
                                if (next instanceof VarInsnNode varInsnNode && varInsnNode.getOpcode() == Opcodes.ASTORE) {
                                    listVarIndex = varInsnNode.var;
                                } else {
                                    throw new IllegalStateException("Expected VarInsnNode, got %s".formatted(next.getClass().getName()));
                                }
                                iterator.previous();
                            }

                            // replace iterator call with listIterator
                            // target insn: INVOKEINTERFACE java/util/Collection.iterator ()Ljava/util/Iterator; (itf)
                            if (methodInsnNode.owner.equals("java/util/Collection") && methodInsnNode.name.equals("iterator") && methodInsnNode.desc.equals("()Ljava/util/Iterator;")) {
                                // replace iterator with listIterator
                                // need to check cast to java/util/List first
                                // INVOKEINTERFACE java/util/List.listIterator ()Ljava/util/ListIterator; (itf)

                                iterator.previous();
                                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/util/List"));
                                iterator.next();
                                iterator.set(new MethodInsnNode(
                                        Opcodes.INVOKEINTERFACE,
                                        "java/util/List",
                                        "listIterator",
                                        "()Ljava/util/ListIterator;",
                                        true
                                ));
                                final AbstractInsnNode next = iterator.next();
                                if (next instanceof VarInsnNode varInsnNode) {
                                    iteratorVarIndex = varInsnNode.var;
                                } else {
                                    throw new IllegalStateException("Expected VarInsnNode after iterator call, but got %s".formatted(next.getClass().getName()));
                                }
                                iterator.previous();
                            }

                            // call handler with progress tracker
                            // target insn: INVOKEINTERFACE java/util/Iterator.next ()Ljava/lang/Object; (itf)
                            if (methodInsnNode.owner.equals("java/util/Iterator") && methodInsnNode.name.equals("next") && methodInsnNode.desc.equals("()Ljava/lang/Object;")) {

                                final AbstractInsnNode next = iterator.next();
                                if (next.getOpcode() != Opcodes.CHECKCAST) {
                                    throw new IllegalStateException("Expected CHECKCAST after iterator.next() call, but got %s".formatted(next.getClass().getName()));
                                }
                                if (listVarIndex == -1) {
                                    throw new IllegalStateException("listVarIndex not found");
                                }
                                if (iteratorVarIndex == -1) {
                                    throw new IllegalStateException("iteratorVarIndex not found");
                                }
                                // Lcom/ishland/earlyloadingscreen/platform_cl/AppLoaderAccessSupport;onEntrypointInvoke(Lnet/fabricmc/loader/api/entrypoint/EntrypointContainer;Lcom/ishland/earlyloadingscreen/platform_cl/AppLoaderAccessSupport$ProgressHolderAccessor;Ljava/util/List;Ljava/util/ListIterator;Ljava/lang/String;)V
                                iterator.add(new InsnNode(Opcodes.DUP));
                                iterator.add(new VarInsnNode(Opcodes.ALOAD, progressTrackerIndex));
                                iterator.add(new VarInsnNode(Opcodes.ALOAD, listVarIndex));
                                iterator.add(new VarInsnNode(Opcodes.ALOAD, iteratorVarIndex));
                                iterator.add(new VarInsnNode(Opcodes.ALOAD, 1)); // arg1
                                iterator.add(new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "com/ishland/earlyloadingscreen/platform_cl/AppLoaderAccessSupport",
                                        "onEntrypointInvoke",
                                        "(Lnet/fabricmc/loader/api/entrypoint/EntrypointContainer;Lcom/ishland/earlyloadingscreen/platform_cl/AppLoaderAccessSupport$ProgressHolderAccessor;Ljava/util/List;Ljava/util/ListIterator;Ljava/lang/String;)V",
                                        false
                                ));

                                iterator.previous();
                            }
                        }

                        if (insn instanceof LabelNode labelNode) {
                            if (labelNode == firstLocalVar.start) {
                                // initialize progress tracker
                                // use Lcom/ishland/earlyloadingscreen/platform_cl/AppLoaderAccessSupport;tryCreateProgressHolder()Lcom/ishland/earlyloadingscreen/platform_cl/AppLoaderAccessSupport$ProgressHolderAccessor;
                                iterator.add(
                                        new MethodInsnNode(
                                                Opcodes.INVOKESTATIC,
                                                "com/ishland/earlyloadingscreen/platform_cl/AppLoaderAccessSupport",
                                                "tryCreateProgressHolder",
                                                "()Lcom/ishland/earlyloadingscreen/platform_cl/AppLoaderAccessSupport$ProgressHolderAccessor;",
                                                false
                                        )
                                );
                                iterator.add(new VarInsnNode(Opcodes.ASTORE, progressTrackerIndex));
                            }
                        }

                        if (insn.getOpcode() == Opcodes.RETURN) {
                            // call close() on progress tracker

                            LabelNode continueLabel = new LabelNode();

                            iterator.previous();
                            iterator.add(new VarInsnNode(Opcodes.ALOAD, progressTrackerIndex));
                            iterator.add(new JumpInsnNode(Opcodes.IFNULL, continueLabel));
                            iterator.add(new VarInsnNode(Opcodes.ALOAD, progressTrackerIndex));
                            // Ljava/io/Closeable;close()V
                            iterator.add(new MethodInsnNode(
                                    Opcodes.INVOKEINTERFACE,
                                    "java/io/Closeable",
                                    "close",
                                    "()V",
                                    true
                            ));
                            iterator.add(continueLabel);
                            iterator.next();
                        }
                    }

                }
            }
            return true;
        }

        // patch net/fabricmc/loader/impl/launch/knot/KnotClassDelegate
        if (className.equals("net/fabricmc/loader/impl/launch/knot/KnotClassDelegate")) {
            SharedConstants.LOGGER.info("Patching KnotClassDelegate for class loader issues");

            for (MethodNode method : node.methods) {

                // Lnet/fabricmc/loader/impl/launch/knot/KnotClassDelegate;loadClass(Ljava/lang/String;Z)Ljava/lang/Class;
                if (method.name.equals("loadClass") && method.desc.equals("(Ljava/lang/String;Z)Ljava/lang/Class;")) {
                    SharedConstants.LOGGER.info("Patching KnotClassDelegate.loadClass");

                    final LocalVariableNode firstLocalVar = method.localVariables.get(0);
                    final ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
                    while (iterator.hasNext()) {
                        final AbstractInsnNode insn = iterator.next();

                        if (insn instanceof LabelNode labelNode) {
                            if (labelNode == firstLocalVar.start) {

                                LabelNode continueLabel = new LabelNode();
                                // add special override to delegate to parent
                                iterator.add(new VarInsnNode(Opcodes.ALOAD, 1));
                                iterator.add(new LdcInsnNode("com.ishland.earlyloadingscreen.platform_cl."));
                                // do String.startsWith
                                iterator.add(new MethodInsnNode(
                                        Opcodes.INVOKEVIRTUAL,
                                        "java/lang/String",
                                        "startsWith",
                                        "(Ljava/lang/String;)Z",
                                        false
                                ));
                                iterator.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));

                                // parent loader at field Lnet/fabricmc/loader/impl/launch/knot/KnotClassDelegate;parentClassLoader:Ljava/lang/ClassLoader;
                                // invoke INVOKEVIRTUAL java/lang/ClassLoader.loadClass (Ljava/lang/String;)Ljava/lang/Class;
                                iterator.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                iterator.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                iterator.add(new FieldInsnNode(
                                        Opcodes.GETFIELD,
                                        "net/fabricmc/loader/impl/launch/knot/KnotClassDelegate",
                                        "parentClassLoader",
                                        "Ljava/lang/ClassLoader;"
                                ));
                                iterator.add(new VarInsnNode(Opcodes.ALOAD, 1));
                                iterator.add(new MethodInsnNode(
                                        Opcodes.INVOKEVIRTUAL,
                                        "java/lang/ClassLoader",
                                        "loadClass",
                                        "(Ljava/lang/String;)Ljava/lang/Class;",
                                        false
                                ));

                                iterator.add(new InsnNode(Opcodes.ARETURN));

                                iterator.add(continueLabel);

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
