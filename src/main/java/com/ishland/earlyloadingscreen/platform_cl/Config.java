package com.ishland.earlyloadingscreen.platform_cl;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class Config {

    public static final boolean REUSE_EARLY_WINDOW;
    public static final boolean ENABLE_ENTRYPOINT_INFORMATION;
    public static final boolean ENABLE_MIXIN_PRETRANSFORM;
    public static final boolean HACK_VERY_EARLY_LOAD;

    static {
        final Properties properties = new Properties();
        final Properties newProperties = new Properties();
        final Path path = FabricLoader.getInstance().getConfigDir().resolve("early-loading-screen.properties");
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path, StandardOpenOption.CREATE)) {
                properties.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Early loading screen configuration file\n");

        REUSE_EARLY_WINDOW = getBoolean(properties, newProperties, "reuse_early_window", true, sb);
        ENABLE_ENTRYPOINT_INFORMATION = getBoolean(properties, newProperties, "enable_entrypoint_information", true, sb);
        ENABLE_MIXIN_PRETRANSFORM = getBoolean(properties, newProperties, "enable_mixin_pretransform", true, sb);
        HACK_VERY_EARLY_LOAD = getBoolean(properties, newProperties, "hack_very_early_load", true, sb);

        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            newProperties.store(out, sb.toString().trim().indent(1));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
    }

    private static int getInt(Properties properties, Properties newProperties, String key, int def, StringBuilder comment) {
        comment.append(key).append(" default: ").append(def).append("\n");
        try {
            final int i = Integer.parseInt(properties.getProperty(key));
            newProperties.setProperty(key, String.valueOf(i));
            return i;
        } catch (NumberFormatException e) {
            newProperties.setProperty(key, "default");
            return def;
        }
    }

    private static boolean getBoolean(Properties properties, Properties newProperties, String key, boolean def, StringBuilder comment) {
        comment.append(key).append(" default: ").append(def).append("\n");
        try {
            final boolean b = parseBoolean(properties.getProperty(key));
            newProperties.setProperty(key, String.valueOf(b));
            return b;
        } catch (NumberFormatException e) {
            newProperties.setProperty(key, "default");
            return def;
        }
    }

    private static boolean parseBoolean(String string) {
        if (string == null) throw new NumberFormatException("null");
        if (string.trim().equalsIgnoreCase("true")) return true;
        if (string.trim().equalsIgnoreCase("false")) return false;
        throw new NumberFormatException(string);
    }

}
