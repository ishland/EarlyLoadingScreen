package com.ishland.earlyloadingscreen.mixin.access;

import net.minecraft.client.texture.TextureStitcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureStitcher.Holder.class)
public interface ITextureStitcherHolder<T extends TextureStitcher.Stitchable> {

    @Accessor
    T getSprite();

}
