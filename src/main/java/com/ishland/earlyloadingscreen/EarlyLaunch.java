package com.ishland.earlyloadingscreen;

import com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport;
import com.ishland.earlyloadingscreen.platform_cl.Config;
import com.ishland.earlyloadingscreen.util.AppLoaderUtil;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EarlyLaunch {

    public static final String SMALL_REMINDER = "The following \"Unable to register injection point\" can be safely ignored. ";

    static {
        load0();
    }

    private static void load0() {
        final ClassLoader classLoader = EarlyLaunch.class.getClassLoader();
        System.out.println(String.format("Loading EarlyLoadingScreen early on ClassLoader %s", classLoader.getClass().getName()));
        Config.init();

        if (!Config.HACK_VERY_EARLY_LOAD) {
            System.out.println("[EarlyLoadingScreen] Skipping very early load");
            System.out.println(SMALL_REMINDER);
            return;
        }

        try {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            unlockLibraryOnKnot(contextClassLoader);
            if (contextClassLoader.getClass().isInstance(classLoader)) {
                contextClassLoader.loadClass("com.ishland.earlyloadingscreen.Launch").getMethod("init").invoke(null);
            } else {
                System.out.println("Relaunching on context classloader");
                final Instrumentation inst = ByteBuddyAgent.install();
                inst.redefineModule(
                        ModuleLayer.boot().findModule("java.base").get(),
                        Set.of(),
                        Map.of(),
                        Map.of("java.lang", Set.of(EarlyLaunch.class.getModule())),
                        Set.of(),
                        Map.of()
                );
                AppLoaderAccessSupport.class.getName();
                AppLoaderAccessSupport.LoadingScreenAccessor.class.getName();
                AppLoaderAccessSupport.ProgressHolderAccessor.class.getName();
                final Class<?> launchClass = defineClass(contextClassLoader, "com.ishland.earlyloadingscreen.Launch");
                launchClass.getMethod("init").invoke(null);
                System.out.println("[EarlyLoadingScreen] Relaunched on context classloader");
            }
        } catch (Throwable t) {
            System.out.println("[EarlyLoadingScreen] Failed to launch early");
            t.printStackTrace();
        }
        System.out.println(SMALL_REMINDER);
    }

    private static void unlockLibraryOnKnot(ClassLoader knotClassLoader) {
        try {
            final Method getDelegate = knotClassLoader.getClass().getDeclaredMethod("getDelegate");
            getDelegate.setAccessible(true);
            final Object knotClassLoaderDelegate = getDelegate.invoke(knotClassLoader);
            final Class<?> delegateClazz = Class.forName("net.fabricmc.loader.impl.launch.knot.KnotClassDelegate");

            final MinecraftGameProvider gameProvider = (MinecraftGameProvider) FabricLoaderImpl.INSTANCE.getGameProvider();
//            final Field getLogJars = MinecraftGameProvider.class.getDeclaredField("logJars");
//            getLogJars.setAccessible(true);
//            Set<Path> logJars = (Set<Path>) getLogJars.get(gameProvider);
            final Field getMiscGameLibraries = MinecraftGameProvider.class.getDeclaredField("miscGameLibraries");
            getMiscGameLibraries.setAccessible(true);
            List<Path> miscGameLibraries = (List<Path>) getMiscGameLibraries.get(gameProvider);
            final Method setAllowedPrefixes = delegateClazz.getDeclaredMethod("setAllowedPrefixes", Path.class, String[].class);
            setAllowedPrefixes.setAccessible(true);
            final Method addCodeSource = delegateClazz.getDeclaredMethod("addCodeSource", Path.class);
            addCodeSource.setAccessible(true);
            for (Path library : miscGameLibraries) {
                setAllowedPrefixes.invoke(knotClassLoaderDelegate, library, new String[0]);
                addCodeSource.invoke(knotClassLoaderDelegate, library);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to unlock library on knot", t);
        }
    }

    // have to duplicate code here because of classloader issues
    public static Class<?> defineClass(ClassLoader classLoader, String name) throws IllegalAccessException, InvocationTargetException, IOException, NoSuchMethodException {
        final Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        defineClass.setAccessible(true);
        return (Class<?>) defineClass.invoke(
                classLoader,
                name,
                getClassFile(name),
                0,
                getClassFile(name).length
        );
    }

    private static byte[] getClassFile(String name) throws IOException {
        try (InputStream in = AppLoaderUtil.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            return in.readAllBytes();
        }
    }

}
