package de.joancode.invSorter.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class InvSorterSettings {
    private static final File CONFIG_DIR = new File(System.getProperty("user.home"), ".minecraft/config/invsorter");
    private static final File SETTINGS_FILE = new File(CONFIG_DIR, "settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static SettingsData settings = new SettingsData();

    private InvSorterSettings() {
    }

    public static void load() {
        if (!SETTINGS_FILE.exists()) {
            settings = new SettingsData();
            return;
        }

        try (FileReader reader = new FileReader(SETTINGS_FILE)) {
            SettingsData loaded = GSON.fromJson(reader, SettingsData.class);
            settings = loaded != null ? loaded : new SettingsData();
        } catch (Exception e) {
            System.err.println("[HotbarPresets] Failed to load settings: " + e.getMessage());
            settings = new SettingsData();
        }
    }

    public static void save() {
        if (!CONFIG_DIR.exists() && !CONFIG_DIR.mkdirs()) {
            return;
        }

        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            GSON.toJson(settings, writer);
        } catch (IOException e) {
            System.err.println("[HotbarPresets] Failed to save settings: " + e.getMessage());
        }
    }

    public static boolean isFullInventorySortModeEnabled() {
        return settings.fullInventorySortMode;
    }

    public static void setFullInventorySortModeEnabled(boolean enabled) {
        settings.fullInventorySortMode = enabled;
        save();
    }

    private static class SettingsData {
        private boolean fullInventorySortMode = false;
    }
}
