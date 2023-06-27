package com.ishland.earlyloadingscreen;

import com.ishland.earlyloadingscreen.platform_cl.Config;
import com.ishland.earlyloadingscreen.platform_cl.LaunchPoint;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class TheMixinConfig implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
//        MixinExtrasBootstrap.init();
        Config.init();
        if (Config.WINDOW_CREATION_POINT.ordinal() <= LaunchPoint.mixinLoad.ordinal()) {
            Launch.initAndCreateWindow(false);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        final String packageName = "com.ishland.earlyloadingscreen.mixin.";
        if (mixinClassName.startsWith(packageName)) {
            final String name = mixinClassName.substring(packageName.length());
            for (String disabledMixin : Config.DISABLED_MIXINS) {
                if (name.startsWith(disabledMixin + ".") || name.equals(disabledMixin)) {
                    SharedConstants.LOGGER.info("Disabling mixin {} due to config", mixinClassName);
                    return false;
                }
            }

        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
