package com.ishland.earlyloadingscreen.mixin.progress;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(ModelLoader.class)
public abstract class MixinModelLoader {

    @Shadow protected abstract void addModel(ModelIdentifier modelId);

    @Shadow @Final public static ModelIdentifier MISSING_ID;
    @Shadow @Final private static Map<Identifier, StateManager<Block, BlockState>> STATIC_DEFINITIONS;
    @Shadow @Final private Map<Identifier, UnbakedModel> modelsToBake;
    @Nullable
    private LoadingProgressManager.ProgressHolder modelLoadProgressHolder;
    @Nullable
    private LoadingProgressManager.ProgressHolder modelAdditionalLoadProgressHolder;
    private int modelLoadProgress = 0;
    private int modelLoadTotalEstimate;
    private int modelDependencyResolveProgress = 0;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER))
    private void earlyInit(CallbackInfo ci) {
        modelLoadProgressHolder = LoadingProgressManager.tryCreateProgressHolder();
        modelAdditionalLoadProgressHolder = LoadingProgressManager.tryCreateProgressHolder();
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.update(() -> "Preparing models...");
        }
        for (Map.Entry<Identifier, StateManager<Block, BlockState>> entry : STATIC_DEFINITIONS.entrySet()) {
            modelLoadTotalEstimate += entry.getValue().getStates().size();
        }
        for(Block block : Registry.BLOCK) {
            modelLoadTotalEstimate += block.getStateManager().getStates().size();
        }
        modelLoadTotalEstimate += Registry.ITEM.getIds().size();
        modelLoadTotalEstimate += 4;
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

    @Inject(method = "addModel", at = @At("HEAD"))
    private void progressAddModel(ModelIdentifier modelId, CallbackInfo ci) {
        this.modelLoadProgress ++;
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.update(() -> String.format("Loading model (%d/~%d): %s", this.modelLoadProgress, this.modelLoadTotalEstimate, modelId));
            modelLoadProgressHolder.updateProgress(() -> (float) this.modelLoadProgress / this.modelLoadTotalEstimate);
        }
    }

    @Inject(method = "method_4732", at = @At("HEAD"))
    private void progressModelResolution(Set<Pair<String, String>> set, UnbakedModel model, CallbackInfoReturnable<Stream<SpriteIdentifier>> cir) {
        this.modelDependencyResolveProgress ++;
        if (modelLoadProgressHolder != null) {
            final int size = this.modelsToBake.size();
            modelLoadProgressHolder.update(() -> String.format("Resolving model dependencies (%d/%d): %s", this.modelDependencyResolveProgress, size, model));
            modelLoadProgressHolder.updateProgress(() -> (float) this.modelDependencyResolveProgress / size);
        }
    }

    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Ljava/util/Set;forEach(Ljava/util/function/Consumer;)V"))
    private void redirectIteration(Set<Identifier> instance, Consumer<Identifier> consumer) {
        try (LoadingProgressManager.ProgressHolder progressHolder = LoadingProgressManager.tryCreateProgressHolder()) {
            int index = 0;
            int size = instance.size();
            for (Identifier identifier : instance) {
                if (progressHolder != null) {
                    int finalIndex = index;
                    progressHolder.update(() -> String.format("Baking model (%d/%d): %s", finalIndex, size, identifier));
                    progressHolder.updateProgress(() -> (float) finalIndex / size);
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
            modelLoadProgressHolder.updateProgress(null);
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
        if (this.modelLoadProgress > this.modelLoadTotalEstimate && modelAdditionalLoadProgressHolder != null) {
            modelAdditionalLoadProgressHolder.update(() -> "Loading additional model %s...".formatted(id));
        }
    }

    @Inject(method = "loadModel", at = @At("RETURN"))
    private void captureAdditionalLoadModelsPost(Identifier id, CallbackInfo ci) {
        if (this.modelLoadProgress > this.modelLoadTotalEstimate && modelAdditionalLoadProgressHolder != null) {
            modelAdditionalLoadProgressHolder.update(() -> "Loaded additional model %s".formatted(id));
        }
    }

}
