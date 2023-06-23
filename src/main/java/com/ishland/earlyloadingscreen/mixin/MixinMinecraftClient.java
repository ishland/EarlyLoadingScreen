package com.ishland.earlyloadingscreen.mixin;

import com.ishland.earlyloadingscreen.LoadingScreenManager;
import com.ishland.earlyloadingscreen.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ModStatus;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Shadow protected abstract String getWindowTitle();

    @Shadow
    public static ModStatus getModStatus() {
        throw new AbstractMethodError();
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;instance:Lnet/minecraft/client/MinecraftClient;", opcode = Opcodes.PUTSTATIC, shift = At.Shift.AFTER))
    private void earlyInit(CallbackInfo ci) {
        String windowTitle;
        try {
            StringBuilder stringBuilder = new StringBuilder("Minecraft");
            final ModStatus modStatus = getModStatus();
            if (modStatus != null && modStatus.isModded()) {
                stringBuilder.append("*");
            }

            stringBuilder.append(" ");
            stringBuilder.append(net.minecraft.SharedConstants.getGameVersion().getName());
            windowTitle = stringBuilder.toString();
        } catch (Throwable t) {
            SharedConstants.LOGGER.error("Failed to get window title", t);
            windowTitle = "Minecraft";
        }
        LoadingScreenManager.windowEventLoop.setWindowTitle(windowTitle + " - Loading...");
    }

}
