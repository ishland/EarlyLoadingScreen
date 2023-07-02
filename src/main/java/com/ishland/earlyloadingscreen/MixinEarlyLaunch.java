package com.ishland.earlyloadingscreen;

import com.ishland.earlyloadingscreen.EarlyLaunch;
import com.ishland.earlyloadingscreen.platform_cl.LaunchPoint;

public class MixinEarlyLaunch {

    public static final String SMALL_REMINDER = "The following \"Unable to register injection point\" can be safely ignored. ";

    static {
        EarlyLaunch.load0(LaunchPoint.mixinEarly);
        System.out.println(SMALL_REMINDER);
    }

}
