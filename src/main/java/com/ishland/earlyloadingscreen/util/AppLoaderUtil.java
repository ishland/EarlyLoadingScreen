package com.ishland.earlyloadingscreen.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppLoaderUtil {


    public static void init() {
        try {
//            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(FabricLoader.class, MethodHandles.lookup());
//            lookup.defineClass(getClassFile("com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport"));
//            lookup.defineClass(getClassFile("com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport$LoadingScreenAccessor"));
//            lookup.defineClass(getClassFile("com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport$ProgressHolderAccessor"));

            defineClass("com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport");
            defineClass("com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport$LoadingScreenAccessor");
            defineClass("com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport$ProgressHolderAccessor");
        } catch (Throwable t) {
            throw new RuntimeException("Failed to init AppLoaderUtil", t);
        }
    }

    private static void defineClass(String name) throws IllegalAccessException, InvocationTargetException, IOException, NoSuchMethodException {
        if (isAlreadyLoaded(name)) {
            return;
        }
        final Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        defineClass.setAccessible(true);
        defineClass.invoke(
                FabricLoader.class.getClassLoader(),
                name,
                getClassFile(name),
                0,
                getClassFile(name).length
        );
    }

    private static boolean isAlreadyLoaded(String name) {
        try {
            Class.forName(name, false, FabricLoader.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static byte[] getClassFile(String name) throws IOException {
        try (InputStream in = AppLoaderUtil.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            return in.readAllBytes();
        }
    }

}
