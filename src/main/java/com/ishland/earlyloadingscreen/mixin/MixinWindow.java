package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.Config;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public class MixinWindow {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private long redirectCreateWindow(int width, int height, CharSequence title, long monitor, long share) {
//        if (true) {
//            while (true) {
//                LockSupport.park();
//            }
//        }
        final long context = LoadingScreenManager.takeContext();
        if (context != 0L) {
            if (Config.REUSE_EARLY_WINDOW) {
                GLFW.glfwSetWindowSize(context, width, height);
                GLFW.glfwSetWindowTitle(context, title);
                return context;
            } else {
                final long newHandle = GLFW.glfwCreateWindow(width, height, title, monitor, share);
                GLFW.glfwMakeContextCurrent(newHandle);
                GL.createCapabilities();
                GLFW.glfwSwapBuffers(newHandle);
                LoadingScreenManager.reInitLoop();
                GLFW.glfwMakeContextCurrent(0L);
                SharedConstants.LOGGER.info("Destroying early window");
                GLFW.glfwDestroyWindow(context);
                return newHandle;
            }
        } else {
            return GLFW.glfwCreateWindow(width, height, title, monitor, share);
        }
    }

}
