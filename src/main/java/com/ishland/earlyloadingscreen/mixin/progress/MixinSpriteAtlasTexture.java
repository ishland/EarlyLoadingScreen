package com.ishland.earlyloadingscreen.mixin.progress;

import com.ishland.earlyloadingscreen.util.ProgressUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureStitcher;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTexture {

    @Inject(method = "loadSprites(Lnet/minecraft/resource/ResourceManager;Ljava/util/Set;)Ljava/util/Collection;", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void captureLoadSprites(ResourceManager resourceManager, Set<Identifier> ids, CallbackInfoReturnable<Collection<Sprite.Info>> cir, List<CompletableFuture<?>> list, Queue<Sprite.Info> queue) {
        ProgressUtil.createProgress(list, CompletableFuture.allOf(list.toArray(CompletableFuture[]::new)), "Deserializing sprites");
    }

    @Inject(method = "loadSprites(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/client/texture/TextureStitcher;I)Ljava/util/List;", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void captureLoadSprites2(ResourceManager resourceManager, TextureStitcher textureStitcher, int maxLevel, CallbackInfoReturnable<List<Sprite>> cir, Queue<Sprite> queue, List<CompletableFuture<?>> list) {
        ProgressUtil.createProgress(list, CompletableFuture.allOf(list.toArray(CompletableFuture[]::new)), "Loading sprites");
    }


}
