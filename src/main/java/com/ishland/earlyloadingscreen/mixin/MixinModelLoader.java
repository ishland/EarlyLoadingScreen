package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.LoadingScreenManager;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(ModelLoader.class)
public abstract class MixinModelLoader {

    @Shadow protected abstract void addModel(ModelIdentifier modelId);

    private LoadingScreenManager.RenderLoop.ProgressHolder modelLoadProgressHolder;
    private List<ModelIdentifier> deferredLoad;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER))
    private void earlyInit(CallbackInfo ci) {
        modelLoadProgressHolder = LoadingScreenManager.tryCreateProgressHolder();
        deferredLoad = new ArrayList<>();
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.update("Preparing models...");
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(CallbackInfo ci) {
        if (modelLoadProgressHolder != null) {
            modelLoadProgressHolder.close();
            modelLoadProgressHolder = null;
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
        StringBuilder sb = new StringBuilder();
        if (deferredLoad != null) {
            int index = 0;
            int size = deferredLoad.size();
            for (ModelIdentifier modelId : deferredLoad) {
                if (modelLoadProgressHolder != null) {
                    sb.setLength(0);
                    sb.append("Loading model").append(" (").append(index).append("/").append(size).append("): ").append(modelId);
                    modelLoadProgressHolder.update(sb.toString());
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
            StringBuilder sb = new StringBuilder();
            for (Identifier identifier : instance) {
                if (progressHolder != null) {
                    sb.setLength(0);
                    sb.append("Baking model").append(" (").append(index).append("/").append(size).append("): ").append(identifier);
                    progressHolder.update(sb.toString());
                }
                index++;
                consumer.accept(identifier);
            }
        }

    }

}
