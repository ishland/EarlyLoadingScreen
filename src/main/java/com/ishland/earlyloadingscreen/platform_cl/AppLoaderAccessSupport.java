package com.ishland.earlyloadingscreen.platform_cl;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.io.Closeable;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

public class AppLoaderAccessSupport {

    private static final String shareKey = "early-loadingscreen:accessor";

    static {
        if (AppLoaderAccessSupport.class.getClassLoader() != FabricLoader.class.getClassLoader()) {
            System.err.println(String.format("[EarlyLoadingScreen] AppLoaderAccessSupport is loaded by %s but expected to be loaded by %s! Entrypoint information may not be available. ", AppLoaderAccessSupport.class.getClassLoader(), FabricLoader.class.getClassLoader()));
        }
        LoadingScreenAccessor.class.getName();
        ProgressHolderAccessor.class.getName();
    }

    public static void setAccess(LoadingScreenAccessor access) {
        FabricLoader.getInstance().getObjectShare().put(shareKey, access);
    }

    // called by generated code
    @SuppressWarnings("unused")
    public static ProgressHolderAccessor tryCreateProgressHolder() {
        final Object o = FabricLoader.getInstance().getObjectShare().get(shareKey);
        if (o instanceof LoadingScreenAccessor accessor) {
            return accessor.tryCreateProgressHolder();
        } else if (o != null) {
            System.err.println(String.format("[EarlyLoadingScreen] Share %s exists but is not a LoadingScreenAccessor (found %s), entrypoint information will not be available.", shareKey, o));
        } else {
            System.err.println(String.format("[EarlyLoadingScreen] Share %s does not exist, entrypoint information will not be available.", shareKey));
        }
        return null;
    }

    // called by generated code
    @SuppressWarnings("unused")
    public static <T> void onEntrypointInvoke(EntrypointContainer<T> container, ProgressHolderAccessor progressHolder, List<?> list, ListIterator<?> listIterator, String entrypointName) {
        if (progressHolder != null) {
            progressHolder.update(() -> String.format("Running entrypoint %s (%d/%d) for mod %s", entrypointName, listIterator.previousIndex(), list.size(), container.getProvider().getMetadata().getId()));
        }
    }

    public interface LoadingScreenAccessor {
        public ProgressHolderAccessor tryCreateProgressHolder();
    }

    public interface ProgressHolderAccessor extends Closeable {
        public void update(Supplier<String> text);
    }

}
