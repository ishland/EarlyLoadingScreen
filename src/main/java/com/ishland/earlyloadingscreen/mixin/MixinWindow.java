package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.LoadingScreenManager;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.locks.LockSupport;

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
            GLFW.glfwSetWindowSize(context, width, height);
            GLFW.glfwSetWindowTitle(context, title);
            return context;
        } else {
            return GLFW.glfwCreateWindow(width, height, title, monitor, share);
        }
    }

}
