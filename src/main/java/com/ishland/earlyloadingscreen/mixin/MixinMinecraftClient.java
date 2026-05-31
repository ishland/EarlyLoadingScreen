package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.Launch;
import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import com.ishland.earlyloadingscreen.platform_cl.Config;
import com.ishland.earlyloadingscreen.platform_cl.LaunchPoint;
import net.minecraft.client.Minecraft;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;instance:Lnet/minecraft/client/Minecraft;", opcode = Opcodes.PUTSTATIC, shift = At.Shift.AFTER))
    private void earlyInit(CallbackInfo ci) {
        String windowTitle;
        try {
            StringBuilder stringBuilder = new StringBuilder("Minecraft ");
            stringBuilder.append(net.minecraft.SharedConstants.getCurrentVersion().name());
            windowTitle = stringBuilder.toString();
        } catch (Throwable t) {
            SharedConstants.LOGGER.error("Failed to get window title", t);
            windowTitle = "Minecraft";
        }
        if (Config.WINDOW_CREATION_POINT.ordinal() <= LaunchPoint.mcEarly.ordinal()) {
            Launch.initAndCreateWindow(false);
            LoadingScreenManager.windowEventLoop.setWindowTitle(windowTitle);
        }
    }

}
