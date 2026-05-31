package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.ishland.earlyloadingscreen.render.GLText.gltSetText;

@Mixin(value = LoadingOverlay.class, priority = 1010)
public class MixinSplashOverlay {

    @Shadow @Final private ReloadInstance reload;

    private LoadingProgressManager.ProgressHolder progressHolder;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (this.reload instanceof SimpleReloadInstance<?>) {
            LoadingProgressManager.ProgressHolder progressHolder = LoadingProgressManager.tryCreateProgressHolder();
            if (progressHolder != null) {
                this.progressHolder = progressHolder;
                this.reload.done().thenRun(progressHolder::close);
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At(value = "RETURN"))
    private void postRender(CallbackInfo ci) {
        final LoadingScreenManager.RenderLoop renderLoop = LoadingScreenManager.windowEventLoop.renderLoop;
        if (renderLoop != null) {
            final Minecraft client = Minecraft.getInstance();
            if (client != null) {
                gltSetText(renderLoop.fpsText, "%d fps".formatted(client.getFps()));
            } else {
                gltSetText(renderLoop.fpsText, "");
            }
            final Window window = Minecraft.getInstance().getWindow();
            renderLoop.render(window.getWidth(), window.getHeight(), (float) window.getGuiScale() / 2.0f);
        }
    }

}
