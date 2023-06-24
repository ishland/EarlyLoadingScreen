package com.ishland.earlyloadingscreen.mixin.access;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface IMinecraftClient {

    @Accessor
    static int getCurrentFps() {
        throw new AbstractMethodError();
    }

}