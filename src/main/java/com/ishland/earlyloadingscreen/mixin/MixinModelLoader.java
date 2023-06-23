package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.LoadingScreenManager;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(ModelLoader.class)
public abstract class MixinModelLoader {

    @Shadow protected abstract void addModel(ModelIdentifier modelId);

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
        if (deferredLoad != null) {
            deferredLoad.add(modelId);
        } else {
            assert instance == (Object) this;
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
