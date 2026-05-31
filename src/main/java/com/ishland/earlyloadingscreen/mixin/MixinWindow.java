package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.Launch;
import com.ishland.earlyloadingscreen.platform_cl.Config;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import com.ishland.earlyloadingscreen.platform_cl.LaunchPoint;
import com.ishland.earlyloadingscreen.util.WindowCreationUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.GpuBackend;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class MixinWindow {

    @Shadow private int width;

    @Shadow private int height;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;createWindow(Lcom/mojang/blaze3d/systems/GpuBackend;IILjava/lang/String;J)J"))
    private long redirectCreateWindow(Window window, GpuBackend backend, int width, int height, String title, long monitor) {
        if (Config.WINDOW_CREATION_POINT.ordinal() <= LaunchPoint.off.ordinal()) {
            Launch.init();
        }
        if (Config.WINDOW_CREATION_POINT == LaunchPoint.off) {
            final long newHandle = WindowCreationUtil.warpGlfwCreateWindow(width, height, title, monitor, 0L);
            initGLFWHandle(newHandle);
            return newHandle;
        }
        final long context = LoadingScreenManager.takeContext();
        if (context != 0L) {
            if (Config.REUSE_EARLY_WINDOW) {
                GLFW.glfwSetWindowTitle(context, title);
                return context;
            } else {
                final long newHandle = WindowCreationUtil.warpGlfwCreateWindow(width, height, title, monitor, 0L);
                initGLFWHandle(newHandle);
                SharedConstants.LOGGER.info("Destroying early window");
                GLFW.glfwDestroyWindow(context);
                return newHandle;
            }
        } else {
            return WindowCreationUtil.warpGlfwCreateWindow(width, height, title, monitor, 0L);
        }
    }

    @Unique
    private static void initGLFWHandle(long newHandle) {
        GLFW.glfwMakeContextCurrent(newHandle);
        GL.createCapabilities();
        GLFW.glfwSwapBuffers(newHandle);
        LoadingScreenManager.reInitLoop();
        GLFW.glfwMakeContextCurrent(0L);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void syncSettingsFromEarlyWindow(CallbackInfo ci) {
        if (Config.REUSE_EARLY_WINDOW) {
            final LoadingScreenManager.WindowSettings settings = LoadingScreenManager.getWindowSettings();
            this.width = settings.windowWidth();
            this.height = settings.windowHeight();
        }
    }

}
