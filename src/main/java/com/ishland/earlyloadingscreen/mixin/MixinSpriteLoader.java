package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.util.ProgressUtil;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(SpriteLoader.class)
public class MixinSpriteLoader {

    @Inject(method = "loadAll", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void captureSpriteLoad(List<Supplier<SpriteContents>> sources, Executor executor, CallbackInfoReturnable<CompletableFuture<List<SpriteContents>>> cir, List<CompletableFuture<SpriteContents>> list) {
        ProgressUtil.createProgress(list, cir.getReturnValue(), "sprites");
    }

}
