package com.ishland.earlyloadingscreen.mixin.progress;

import com.ishland.earlyloadingscreen.LoadingScreenManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(ModelLoader.class)
public abstract class MixinModelLoader {

    @Shadow protected abstract void addModel(ModelIdentifier modelId);

    @Shadow @Final public static ModelIdentifier MISSING_ID;
    @Shadow @Final private static Map<Identifier, StateManager<Block, BlockState>> STATIC_DEFINITIONS;
    @Shadow @Final private Map<Identifier, UnbakedModel> modelsToBake;
    private LoadingScreenManager.RenderLoop.ProgressHolder modelLoadProgressHolder;
    private LoadingScreenManager.RenderLoop.ProgressHolder modelAdditionalLoadProgressHolder;
    private int modelLoadProgress = 0;
    private int modelLoadTotalEstimate;
    private int modelDependencyResolveProgress = 0;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER))
    private void earlyInit(CallbackInfo ci) {
        modelLoadProgressHolder = LoadingScreenManager.tryCreateProgressHolder();
        modelAdditionalLoadProgressHolder = LoadingScreenManager.tryCreateProgressHolder();
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.update(() -> "Preparing models...");
        }
        for (Map.Entry<Identifier, StateManager<Block, BlockState>> entry : STATIC_DEFINITIONS.entrySet()) {
            modelLoadTotalEstimate += entry.getValue().getStates().size();
        }
        for(Block block : Registries.BLOCK) {
            modelLoadTotalEstimate += block.getStateManager().getStates().size();
        }
        modelLoadTotalEstimate += Registries.ITEM.getIds().size();
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
        }
    }

    @Inject(method = "method_45875", at = @At("HEAD"))
    private void progressModelResolution(UnbakedModel model, CallbackInfo ci) {
        this.modelDependencyResolveProgress ++;
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.update(() -> String.format("Resolving model dependencies (%d/%d): %s", this.modelDependencyResolveProgress, this.modelsToBake.size(), model));
        }
    }

    @Redirect(method = "bake", at = @At(value = "INVOKE", target = "Ljava/util/Set;forEach(Ljava/util/function/Consumer;)V"))
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
