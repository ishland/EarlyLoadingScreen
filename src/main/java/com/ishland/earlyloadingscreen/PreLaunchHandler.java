package com.ishland.earlyloadingscreen;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        LoadingScreenManager.windowEventLoop.setWindowTitle("Minecraft - Launching...");
    }
}
