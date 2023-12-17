package com.ishland.earlyloadingscreen.mixin.progress;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import net.minecraft.client.texture.TextureStitcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

@Mixin(TextureStitcher.class)
public class MixinTextureStitcher {

    @Shadow @Final private Set<TextureStitcher.Holder> holders;
    private LoadingProgressManager.ProgressHolder progressHolder;

    @Inject(method = "stitch", at = @At("HEAD"))
    private void preStitch(CallbackInfo ci) {
        progressHolder = LoadingProgressManager.tryCreateProgressHolder();
        if (progressHolder != null) {
            progressHolder.update(() -> "Stitiching textures...");
        }
    }

    @Redirect(method = "stitch", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private <E> Iterator<E> redirectStitchIterator(List<E> instance) {
        return instance.listIterator();
    }

    @Inject(method = "stitch", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;", shift = At.Shift.BY, by = 3), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void monitorStitch(CallbackInfo ci, List<TextureStitcher.Holder> list, Iterator<TextureStitcher.Holder> var2, TextureStitcher.Holder holder) {
        if (progressHolder != null && var2 instanceof ListIterator<TextureStitcher.Holder> iterator) {
            final int previousIndex = iterator.previousIndex();
            final int size = list.size();
            progressHolder.update(() -> "Stitiching textures (%d/%d): %s".formatted(previousIndex, size, holder.sprite.getId()));
            progressHolder.updateProgress(() -> (float) previousIndex / size);
        }
    }

    @Inject(method = "stitch", at = @At("RETURN"))
    private void postStitch(CallbackInfo ci) {
        if (progressHolder != null) progressHolder.close();
    }

}
