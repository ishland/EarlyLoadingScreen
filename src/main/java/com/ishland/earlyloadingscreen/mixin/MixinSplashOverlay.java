package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.LoadingScreenManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.ishland.earlyloadingscreen.render.GLText.gltSetText;

@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void postRender(CallbackInfo ci) {
        final LoadingScreenManager.RenderLoop renderLoop = LoadingScreenManager.windowEventLoop.renderLoop;
        if (renderLoop != null) {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                gltSetText(renderLoop.fpsText, "%d fps".formatted(client.getCurrentFps()));
            } else {
                gltSetText(renderLoop.fpsText, "");
            }
            renderLoop.render();
        }
    }

}
