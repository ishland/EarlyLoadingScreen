package com.ishland.earlyloadingscreen.platform_cl;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.io.Closeable;
import java.util.List;
import java.util.ListIterator;

public class AppLoaderAccessSupport {

    static {
        if (AppLoaderAccessSupport.class.getClassLoader() != FabricLoader.class.getClassLoader()) {
            System.err.println(String.format("[EarlyLoadingScreen] AppLoaderAccessSupport is loaded by %s but expected to be loaded by %s! Entrypoint information may not be available. ", AppLoaderAccessSupport.class.getClassLoader(), FabricLoader.class.getClassLoader()));
        }
        LoadingScreenAccessor.class.getName();
        ProgressHolderAccessor.class.getName();
    }

    private static LoadingScreenAccessor accessor;

    public static void setAccess(LoadingScreenAccessor access) {
        accessor = access;
    }

    // called by generated code
    @SuppressWarnings("unused")
    public static ProgressHolderAccessor tryCreateProgressHolder() {
        if (accessor == null) return null;
        return accessor.tryCreateProgressHolder();
    }

    // called by generated code
    @SuppressWarnings("unused")
    public static <T> void onEntrypointInvoke(EntrypointContainer<T> container, ProgressHolderAccessor progressHolder, List<?> list, ListIterator<?> listIterator, String entrypointName) {
        if (progressHolder != null) {
            progressHolder.update(String.format("Running entrypoint %s (%d/%d) for mod %s", entrypointName, listIterator.previousIndex(), list.size(), container.getProvider().getMetadata().getId()));
        }
    }

    public interface LoadingScreenAccessor {
        public ProgressHolderAccessor tryCreateProgressHolder();
    }

    public interface ProgressHolderAccessor extends Closeable {
        public void update(String text);
    }

}
