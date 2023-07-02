package com.ishland.earlyloadingscreen;

import com.ishland.earlyloadingscreen.platform_cl.LaunchPoint;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;

public class LanguageAdapterLaunch implements LanguageAdapter {

    static {
        EarlyLaunch.load0(LaunchPoint.postModLoading);
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        throw new RuntimeException("This should not be called");
    }

}
