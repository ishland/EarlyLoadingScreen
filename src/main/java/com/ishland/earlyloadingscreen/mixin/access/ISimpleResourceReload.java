package com.ishland.earlyloadingscreen.mixin.access;

import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.SimpleResourceReload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(SimpleResourceReload.class)
public interface ISimpleResourceReload {

    @Accessor
    Set<ResourceReloader> getWaitingReloaders();

}
