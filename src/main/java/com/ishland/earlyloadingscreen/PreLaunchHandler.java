package com.ishland.earlyloadingscreen;

import com.ishland.earlyloadingscreen.platform_cl.Config;
import com.ishland.earlyloadingscreen.platform_cl.LaunchPoint;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        System.clearProperty("earlyloadingscreen.duringEarlyLaunch");
        if (Config.WINDOW_CREATION_POINT.ordinal() <= LaunchPoint.preLaunch.ordinal()) {
            Launch.initAndCreateWindow(false);
            LoadingScreenManager.windowEventLoop.setWindowTitle("Minecraft - Launching...");
        }
    }
}
