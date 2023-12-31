package com.ishland.earlyloadingscreen.mixin.progress;

import com.ishland.earlyloadingscreen.LoadingProgressManager;
import com.ishland.earlyloadingscreen.mixin.access.ITextureStitcherHolder;
import net.minecraft.client.texture.TextureStitcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@Mixin(TextureStitcher.class)
public class MixinTextureStitcher<T extends TextureStitcher.Stitchable> {

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
    private void monitorStitch(CallbackInfo ci, List<TextureStitcher.Holder<T>> list, Iterator<TextureStitcher.Holder<T>> var2, TextureStitcher.Holder<T> holder) {
        if (progressHolder != null && var2 instanceof ListIterator<TextureStitcher.Holder<T>> iterator) {
            final int idx = iterator.previousIndex();
            final int size = list.size();
            progressHolder.update(() -> "Stitiching textures (%d/%d): %s".formatted(idx, size, ((ITextureStitcherHolder<T>) holder).getSprite().getId()));
            progressHolder.updateProgress(() -> idx / (float) size);
        }
    }

    @Inject(method = "stitch", at = @At("RETURN"))
    private void postStitch(CallbackInfo ci) {
        if (progressHolder != null) progressHolder.close();
    }

}
