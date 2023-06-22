package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.ishland.earlyloadingscreen.mixin.access.ISimpleResourceReload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.resource.SimpleResourceReload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.function.Consumer;

import static com.ishland.earlyloadingscreen.render.GLText.gltSetText;

@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Shadow @Final private ResourceReload reload;

    private LoadingScreenManager.RenderLoop.ProgressHolder progressHolder;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (this.reload instanceof SimpleResourceReload<?>) {
            LoadingScreenManager.RenderLoop.ProgressHolder progressHolder = LoadingScreenManager.tryCreateProgressHolder();
            if (progressHolder != null) {
                this.progressHolder = progressHolder;
                this.reload.whenComplete().thenRun(progressHolder::close);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void postRender(CallbackInfo ci) {
        final LoadingScreenManager.RenderLoop renderLoop = LoadingScreenManager.windowEventLoop.renderLoop;
        if (renderLoop != null) {
            if (this.progressHolder != null && this.reload instanceof SimpleResourceReload<?> simpleResourceReload) {
                this.progressHolder.update("Pending reloads: " + Arrays.toString(((ISimpleResourceReload) simpleResourceReload).getWaitingReloaders().toArray()));
            }
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
