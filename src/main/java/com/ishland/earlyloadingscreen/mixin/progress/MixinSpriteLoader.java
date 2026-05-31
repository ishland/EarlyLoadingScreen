package com.ishland.earlyloadingscreen.mixin.progress;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.SpriteLoader.Preparations;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(SpriteLoader.class)
public class MixinSpriteLoader {

    @Inject(method = "loadAndStitch", at = @At("HEAD"))
    private void preLoadAndStitch(ResourceManager manager, Identifier atlasId, int maxSize, Executor executor, Set<?> metadataTypes, CallbackInfoReturnable<CompletableFuture<Preparations>> cir) {
        LoadingProgressManager.ProgressHolder holder = LoadingProgressManager.tryCreateProgressHolder();
        if (holder != null) {
            holder.update(() -> "Loading sprites: " + atlasId);
        }
    }
}
