package com.ishland.earlyloadingscreen.util;

import io.netty.util.internal.PlatformDependent;

public class OSDetectionUtil {

    public static OperatingSystem getOperatingSystem() {
        final String normalizedOs = PlatformDependent.normalizedOs();
        return switch (normalizedOs) {
            case "linux" -> OperatingSystem.LINUX;
            case "osx" -> OperatingSystem.OSX;
            case "sunos" -> OperatingSystem.SOLARIS;
            case "windows" -> OperatingSystem.WINDOWS;
            default -> OperatingSystem.UNKNOWN;
        };
    }

    public enum OperatingSystem {
        LINUX("linux"),
        SOLARIS("solaris"),
        WINDOWS("windows"),
        OSX("osx"),
        UNKNOWN("unknown");

        public final String name;

        OperatingSystem(String name) {
            this.name = name;
        }
    }

}
