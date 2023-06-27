package com.ishland.earlyloadingscreen.mixin.progress;

import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(ModelLoader.class)
public abstract class MixinModelLoader {

    @Shadow protected abstract void addModel(ModelIdentifier modelId);

    @Shadow @Final public static ModelIdentifier MISSING_ID;
    private LoadingScreenManager.RenderLoop.ProgressHolder modelLoadProgressHolder;
    private LoadingScreenManager.RenderLoop.ProgressHolder modelAdditionalLoadProgressHolder;
    private List<ModelIdentifier> deferredLoad;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER))
    private void earlyInit(CallbackInfo ci) {
        modelLoadProgressHolder = LoadingScreenManager.tryCreateProgressHolder();
        modelAdditionalLoadProgressHolder = LoadingScreenManager.tryCreateProgressHolder();
        deferredLoad = new ArrayList<>();
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.update(() -> "Preparing models...");
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(CallbackInfo ci) {
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.close();
            modelLoadProgressHolder = null;
        }
        if (modelAdditionalLoadProgressHolder != null) {
            modelAdditionalLoadProgressHolder.close();
            modelAdditionalLoadProgressHolder = null;
        }
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Redirect(method = {"<init>", "method_4723", "method_4716"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/ModelLoader;addModel(Lnet/minecraft/client/util/ModelIdentifier;)V"))
    private void deferAddModel(ModelLoader instance, ModelIdentifier modelId) {
        assert instance == (Object) this;
        if (modelId == MISSING_ID) { // we don't want to defer loading of missing model
            this.addModel(modelId);
            return;
        }
        if (deferredLoad != null) {
            deferredLoad.add(modelId);
        } else {
            this.addModel(modelId);
        }
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/model/ModelLoader;modelsToBake:Ljava/util/Map;", opcode = Opcodes.GETFIELD))
    private void runDeferredLoad(CallbackInfo ci) {
        if (deferredLoad != null) {
            int index = 0;
            int size = deferredLoad.size();
            for (ModelIdentifier modelId : deferredLoad) {
                if (modelLoadProgressHolder != null) {
                    int finalIndex = index;
                    modelLoadProgressHolder.update(() -> String.format("Loading model (%d/%d): %s", finalIndex, size, modelId));
                }
                index ++;
                this.addModel(modelId);
            }
            deferredLoad = null;
            if (modelLoadProgressHolder != null) {
                modelLoadProgressHolder.update(() -> "Resolving models...");
            }
        }
    }

    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Ljava/util/Set;forEach(Ljava/util/function/Consumer;)V"))
    private void redirectIteration(Set<Identifier> instance, Consumer<Identifier> consumer) {
        try (LoadingScreenManager.RenderLoop.ProgressHolder progressHolder = LoadingScreenManager.tryCreateProgressHolder()) {
            int index = 0;
            int size = instance.size();
            for (Identifier identifier : instance) {
                if (progressHolder != null) {
                    int finalIndex = index;
                    progressHolder.update(() -> String.format("Baking model (%d/%d): %s", finalIndex, size, identifier));
                }
                index++;
                consumer.accept(identifier);
            }
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/SpriteAtlasTexture;stitch(Lnet/minecraft/resource/ResourceManager;Ljava/util/stream/Stream;Lnet/minecraft/util/profiler/Profiler;I)Lnet/minecraft/client/texture/SpriteAtlasTexture$Data;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void captureStitching(ResourceManager resourceManager, BlockColors blockColors, Profiler profiler, int mipmapLevel, CallbackInfo ci, Set<Pair<String, String>> set, Set<SpriteIdentifier> set2, Map<Identifier, List<SpriteIdentifier>> map, Iterator<Map.Entry<Identifier, List<SpriteIdentifier>>> var8, Map.Entry<Identifier, List<SpriteIdentifier>> entry, SpriteAtlasTexture spriteAtlasTexture) {
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.update(() -> "Stitching texture %s...".formatted(entry.getKey()));
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/SpriteAtlasTexture;stitch(Lnet/minecraft/resource/ResourceManager;Ljava/util/stream/Stream;Lnet/minecraft/util/profiler/Profiler;I)Lnet/minecraft/client/texture/SpriteAtlasTexture$Data;")))
    private void capturePostStitching(CallbackInfo ci) {
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.update(() -> "Finalizing model load...");
        }
    }

    @Inject(method = "loadModel", at = @At("HEAD"))
    private void captureAdditionalLoadModelsPre(Identifier id, CallbackInfo ci) {
        if (deferredLoad == null && modelAdditionalLoadProgressHolder != null) {
            modelAdditionalLoadProgressHolder.update(() -> "Loading additional model %s...".formatted(id));
        }
    }

    @Inject(method = "loadModel", at = @At("RETURN"))
    private void captureAdditionalLoadModelsPost(Identifier id, CallbackInfo ci) {
        if (deferredLoad == null && modelAdditionalLoadProgressHolder != null) {
            modelAdditionalLoadProgressHolder.update(() -> "Loaded additional model %s".formatted(id));
        }
    }

}
