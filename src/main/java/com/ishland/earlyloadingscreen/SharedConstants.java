package com.ishland.earlyloadingscreen;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedConstants {

    public static final boolean DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("els.debug");

    public static final Logger LOGGER = LoggerFactory.getLogger("EarlyLoadingScreen");
}
