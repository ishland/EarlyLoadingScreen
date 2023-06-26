package com.ishland.earlyloadingscreen.platform_cl;

import java.util.Locale;

public class PlatformUtil {

    private static final String NORMALIZED_OS = normalizeOs(System.getProperty("os.name", ""));

    public static final boolean IS_WINDOWS = "windows".equals(NORMALIZED_OS);
    public static final boolean IS_OSX = "osx".equals(NORMALIZED_OS);


    private static String normalize(String value) {
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    private static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("aix")) {
            return "aix";
        } else if (value.startsWith("hpux")) {
            return "hpux";
        } else if (!value.startsWith("os400") || value.length() > 5 && Character.isDigit(value.charAt(5))) {
            if (value.startsWith("linux")) {
                return "linux";
            } else if (!value.startsWith("macosx") && !value.startsWith("osx") && !value.startsWith("darwin")) {
                if (value.startsWith("freebsd")) {
                    return "freebsd";
                } else if (value.startsWith("openbsd")) {
                    return "openbsd";
                } else if (value.startsWith("netbsd")) {
                    return "netbsd";
                } else if (!value.startsWith("solaris") && !value.startsWith("sunos")) {
                    return value.startsWith("windows") ? "windows" : "unknown";
                } else {
                    return "sunos";
                }
            } else {
                return "osx";
            }
        } else {
            return "os400";
        }
    }

}
