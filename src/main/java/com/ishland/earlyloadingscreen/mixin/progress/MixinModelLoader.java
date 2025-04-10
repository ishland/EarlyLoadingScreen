package com.ishland.earlyloadingscreen.mixin.progress;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ModelBaker.class)
public abstract class MixinModelLoader {

    @Shadow @Final private Map<BlockState, BlockStateModel.UnbakedGrouped> blockModels;
    @Shadow @Final private Map<Identifier, ItemAsset> itemAssets;
    private LoadingProgressManager.ProgressHolder modelLoadProgressHolder;
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
        for(Block block : Registries.BLOCK) {
            modelLoadTotalEstimate += block.getStateManager().getStates().size();
        }
        modelLoadTotalEstimate += Registries.ITEM.getIds().size();
        modelLoadTotalEstimate += 6;
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

}
