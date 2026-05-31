package com.ishland.earlyloadingscreen.mixin.access;

import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Stitcher.Holder.class)
public interface ITextureStitcherHolder<T extends Stitcher.Entry> {

    @Accessor("entry")
    T getEntry();

}
