package com.ishland.earlyloadingscreen;

import com.ishland.earlyloadingscreen.patch.FabricLoaderInvokePatch;
import com.ishland.earlyloadingscreen.platform_cl.Config;

public class Launch {

    static {
        final ClassLoader classLoader = Launch.class.getClassLoader();
        System.out.println(String.format("Loading EarlyLoadingScreen on ClassLoader %s", classLoader.getClass().getName()));

        Config.init();
        if (Config.ENABLE_ENTRYPOINT_INFORMATION) {
            FabricLoaderInvokePatch.init();
        }
        LoadingScreenManager.init();
    }

    public static void init() {
    }

}
