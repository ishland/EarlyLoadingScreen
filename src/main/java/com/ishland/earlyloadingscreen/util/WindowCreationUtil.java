package com.ishland.earlyloadingscreen.util;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import com.ishland.earlyloadingscreen.patch.SodiumOSDetectionPatch;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

public class WindowCreationUtil {

    public static long warpGlfwCreateWindow(int width, int height, CharSequence title, long monitor, long share) {
        FabricLoader.getInstance().invokeEntrypoints("els_before_window_creation", Runnable.class, Runnable::run);
        sodiumHookInit();
        sodiumHook(false);
        try {
            return GLFW.glfwCreateWindow(width, height, title, monitor, share);
        } finally {
            sodiumHook(true);
            FabricLoader.getInstance().invokeEntrypoints("els_after_window_creation", Runnable.class, Runnable::run);
        }
    }

    private static AtomicBoolean ranSodiumHookInit = new AtomicBoolean(false);
    private static boolean foundSodium = true;

    private static void sodiumHookInit() {
        if (Boolean.getBoolean("earlyloadingscreen.duringEarlyLaunch") && !SodiumOSDetectionPatch.INITIALIZED) {
            final String msg = "SodiumOSDetectionPatch unavailable, sodium workarounds may not work properly";
            SharedConstants.LOGGER.warn(msg);
            LoadingProgressManager.showMessageAsProgress(msg);
            return;
        }
        if (ranSodiumHookInit.compareAndSet(false, true)) {
            if (!FabricLoader.getInstance().isModLoaded("sodium")) {
                foundSodium = false;
                SharedConstants.LOGGER.info("Sodium not found, skipping sodium hook init");
                return;
            }
            final Class<?> graphicsAdapterProbeClazz;
            final Class<?> workaroundsClazz;
            try {
                graphicsAdapterProbeClazz = Class.forName("me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterProbe");
                workaroundsClazz = Class.forName("me.jellysquid.mods.sodium.client.util.workarounds.Workarounds");
            } catch (Throwable t) {
                final String msg = "Failed to find Sodium workarounds, skipping sodium hook init";
                if (FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("els.debug")) {
                    SharedConstants.LOGGER.warn(msg, t);
                } else {
                    SharedConstants.LOGGER.warn(msg);
                }
                LoadingProgressManager.showMessageAsProgress(msg);
                foundSodium = false;
                return;
            }
            try {
                graphicsAdapterProbeClazz.getMethod("findAdapters").invoke(null);
                workaroundsClazz.getMethod("init").invoke(null);
//                final Collection<?> adapters = (Collection<?>) graphicsAdapterProbeClazz.getMethod("getAdapters").invoke(null);
//                boolean foundNvidia = false;
//                for (Object adapter : adapters) {
//                    final Enum<?> vendor = (Enum<?>) graphicsAdapterInfoClazz.getMethod("vendor").invoke(adapter);
//                    if (vendor == Enum.valueOf(graphicsAdapterVendorClazz, "NVIDIA")) {
//                        foundNvidia = true;
//                        break;
//                    }
//                }
//                if (foundNvidia && (PlatformDependent.isWindows() || PlatformDependent.normalizedOs().equals("linux"))) {
//                    final Field activeWorkarounds = workaroundsClazz.getDeclaredField("ACTIVE_WORKAROUNDS");
//                    activeWorkarounds.setAccessible(true);
//                    final Enum<?> NVIDIA_THREADED_OPTIMIZATIONS = Enum.valueOf(workaroundsReferenceClazz, "NVIDIA_THREADED_OPTIMIZATIONS");
//                    ((AtomicReference<Collection<?>>) activeWorkarounds.get(null)).set(Set.of(NVIDIA_THREADED_OPTIMIZATIONS));
//                }
            } catch (Throwable t) {
                final String msg = "Failed to init Sodium workarounds, skipping sodium hook";
                if (FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("els.debug")) {
                    SharedConstants.LOGGER.warn(msg, t);
                } else {
                    SharedConstants.LOGGER.warn(msg);
                }
                LoadingProgressManager.showMessageAsProgress(msg);
                foundSodium = false;
                return;
            }
        }
    }

    private static void sodiumHook(boolean after) {
        if (!foundSodium) return;
        final Class<?> workaroundsClazz;
        final Class<? extends Enum> workaroundsReferenceClazz;
        final Class<?> nvidiaWorkaroundsClazz;
        try {
            workaroundsClazz = Class.forName("me.jellysquid.mods.sodium.client.util.workarounds.Workarounds");
            workaroundsReferenceClazz = (Class<? extends Enum<?>>) Class.forName("me.jellysquid.mods.sodium.client.util.workarounds.Workarounds$Reference");
            nvidiaWorkaroundsClazz = Class.forName("me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia.NvidiaWorkarounds");
        } catch (Throwable e) {
            final String msg = "Failed to find Sodium workarounds, skipping sodium hook";
            if (FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("els.debug")) {
                SharedConstants.LOGGER.warn(msg, e);
            } else {
                SharedConstants.LOGGER.warn(msg);
            }
            LoadingProgressManager.showMessageAsProgress(msg);
            foundSodium = false;
            return;
        }
        try {
            final Enum<?> NVIDIA_THREADED_OPTIMIZATIONS = Enum.valueOf(workaroundsReferenceClazz, "NVIDIA_THREADED_OPTIMIZATIONS");
            if ((boolean) workaroundsClazz.getMethod("isWorkaroundEnabled", workaroundsReferenceClazz).invoke(null, NVIDIA_THREADED_OPTIMIZATIONS)) {
                if (!after) {
                    nvidiaWorkaroundsClazz.getMethod("install").invoke(null);
                    LoadingProgressManager.showMessageAsProgress("Installed Nvidia workarounds from sodium", 5000L);
                } else {
                    nvidiaWorkaroundsClazz.getMethod("uninstall").invoke(null);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
