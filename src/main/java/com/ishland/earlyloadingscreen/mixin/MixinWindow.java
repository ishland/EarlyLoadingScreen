package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.Launch;
import com.ishland.earlyloadingscreen.platform_cl.Config;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import com.ishland.earlyloadingscreen.platform_cl.LaunchPoint;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Final;
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

    @Shadow private int windowedHeight;

    @Shadow private int windowedWidth;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private long redirectCreateWindow(int width, int height, CharSequence title, long monitor, long share) {
//        if (true) {
//            while (true) {
//                LockSupport.park();
//            }
//        }
        if (Config.WINDOW_CREATION_POINT.ordinal() <= LaunchPoint.off.ordinal()) {
            Launch.init();
        }
        if (Config.WINDOW_CREATION_POINT == LaunchPoint.off) {
            final long newHandle = GLFW.glfwCreateWindow(width, height, title, monitor, share);
            initGLFWHandle(newHandle);
            return newHandle;
        }
        final long context = LoadingScreenManager.takeContext();
        if (context != 0L) {
            if (Config.REUSE_EARLY_WINDOW) {
//                GLFW.glfwSetWindowSize(context, width, height);
                GLFW.glfwSetWindowTitle(context, title);
                return context;
            } else {
                final long newHandle = GLFW.glfwCreateWindow(width, height, title, monitor, share);
                initGLFWHandle(newHandle);
                SharedConstants.LOGGER.info("Destroying early window");
                GLFW.glfwDestroyWindow(context);
                return newHandle;
            }
        } else {
            return GLFW.glfwCreateWindow(width, height, title, monitor, share);
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

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;updateWindowRegion()V"))
    private void syncSettingsFromEarlyWindow(CallbackInfo ci) {
        if (Config.REUSE_EARLY_WINDOW) {
            final LoadingScreenManager.WindowSettings settings = LoadingScreenManager.getWindowSettings();
            this.windowedWidth = this.width = settings.windowWidth();
            this.windowedHeight = this.height = settings.windowHeight();
        }
    }

}
