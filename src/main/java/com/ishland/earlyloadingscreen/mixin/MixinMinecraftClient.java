package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.Launch;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import com.ishland.earlyloadingscreen.mixin.access.IGlStateManager;
import com.ishland.earlyloadingscreen.platform_cl.Config;
import com.ishland.earlyloadingscreen.platform_cl.LaunchPoint;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.util.Window;
import net.minecraft.util.ModStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.ishland.earlyloadingscreen.render.GLText.gltSetText;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Shadow protected abstract String getWindowTitle();

    @Shadow
    public static ModStatus getModStatus() {
        throw new AbstractMethodError();
    }

    @Shadow public abstract int getCurrentFps();

    @Shadow @Nullable public abstract Overlay getOverlay();

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;instance:Lnet/minecraft/client/MinecraftClient;", opcode = Opcodes.PUTSTATIC, shift = At.Shift.AFTER))
    private void earlyInit(CallbackInfo ci) {
        String windowTitle;
        try {
            StringBuilder stringBuilder = new StringBuilder("Minecraft");
            final ModStatus modStatus = getModStatus();
            if (modStatus != null && modStatus.isModded()) {
                stringBuilder.append("*");
            }

            stringBuilder.append(" ");
            stringBuilder.append(net.minecraft.SharedConstants.getGameVersion().getName());
            windowTitle = stringBuilder.toString();
        } catch (Throwable t) {
            SharedConstants.LOGGER.error("Failed to get window title", t);
            windowTitle = "Minecraft";
        }
        if (Config.WINDOW_CREATION_POINT.ordinal() <= LaunchPoint.mcEarly.ordinal()) {
            Launch.initAndCreateWindow(false);
            LoadingScreenManager.windowEventLoop.setWindowTitle(windowTitle);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V", shift = At.Shift.AFTER))
    private void postBlit(boolean tick, CallbackInfo ci) {
        if (this.getOverlay() != null) {
            final LoadingScreenManager.RenderLoop renderLoop = LoadingScreenManager.windowEventLoop.renderLoop;
            gltSetText(renderLoop.fpsText, "%d fps".formatted(this.getCurrentFps()));
            final Window window = MinecraftClient.getInstance().getWindow();
            renderLoop.render(window.getFramebufferWidth(), window.getFramebufferHeight(), (float) window.getScaleFactor() / 2.0f, false);
            // restore state
            int activeTexture = GlStateManager._getActiveTexture();
            GL32.glActiveTexture(activeTexture);
            GL32.glBindTexture(GL32.GL_TEXTURE_2D, IGlStateManager.getTEXTURES()[activeTexture - GL32.GL_TEXTURE0].boundTexture);
        }
    }

}
