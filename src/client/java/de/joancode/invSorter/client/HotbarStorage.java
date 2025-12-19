package de.joancode.invSorter.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.item.ItemStack;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HotbarStorage {
    private static final File CONFIG_DIR = new File(System.getProperty("user.home"), ".minecraft/config/invsorter");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "hotbars.json");

    // Build a minimal Gson that only knows about ItemStack via our adapter
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .create();

    public static List<HotbarConfig> loadConfigs() {
        if (!CONFIG_FILE.exists()) return new ArrayList<>();

        // Prüfe, ob die Datei leer ist
        if (CONFIG_FILE.length() == 0) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            Type listType = new TypeToken<List<HotbarConfig>>(){}.getType();

            // Read whole content safely
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }

            String jsonContent = content.toString().trim();
            if (jsonContent.isEmpty()) return new ArrayList<>();

            List<HotbarConfig> configs;
            try {
                configs = GSON.fromJson(jsonContent, listType);
            } catch (JsonSyntaxException jse) {
                System.err.println("Malformed JSON in hotbars.json — backing up and starting fresh: " + jse.getMessage());
                backupCorruptedFile();
                return new ArrayList<>();
            }

            if (configs == null) return new ArrayList<>();

            // Normalize configs: ensure items lists are present and have 9 entries
            for (HotbarConfig cfg : configs) {
                if (cfg == null) continue;
                try {
                    // This will replace null lists and null entries with ItemStack.EMPTY
                    cfg.setItems(cfg.getItems());
                } catch (Exception e) {
                    System.err.println("Failed to normalize configuration '" + (cfg.getName() != null ? cfg.getName() : "<unknown>") + "': " + e.getMessage());
                }
            }

            // Validate and filter out any truly corrupted configurations
            List<HotbarConfig> validConfigs = new ArrayList<>();
            for (HotbarConfig config : configs) {
                if (isValidConfig(config)) {
                    validConfigs.add(config);
                } else {
                    System.err.println("Skipping corrupted configuration: " + (config != null && config.getName() != null ? config.getName() : "null"));
                }
            }

            return validConfigs;
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der Hotbar-Konfigurationen: " + e.getMessage());
            // Bei Fehler die Datei sichern und neu anfangen
            backupCorruptedFile();
            return new ArrayList<>();
        }
    }

    private static void backupCorruptedFile() {
        try {
            if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();
            File backup = new File(CONFIG_DIR, "hotbars.json.corrupted." + System.currentTimeMillis() + ".bak");
            java.nio.file.Files.copy(CONFIG_FILE.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // ignore backup failures
        }
    }

    // Helper method to validate configurations
    private static boolean isValidConfig(HotbarConfig config) {
        if (config == null) return false;
        if (config.getName() == null || config.getName().trim().isEmpty()) return false;
        if (config.getItems() == null) return false;

        // Check that all items in the configuration are not null (they may be ItemStack.EMPTY)
        for (net.minecraft.item.ItemStack item : config.getItems()) {
            if (item == null) return false;
        }

        return true;
    }

    public static void saveConfigs(List<HotbarConfig> configs) {
        if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();

        // Erstelle eine Backup-Datei falls vorhanden
        File backupFile = new File(CONFIG_DIR, "hotbars.json.bak");
        if (CONFIG_FILE.exists()) {
            try {
                java.nio.file.Files.copy(CONFIG_FILE.toPath(), backupFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // Backup fehlgeschlagen, trotzdem fortfahren
            }
        }

        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(configs, writer);
            writer.flush(); // Stelle sicher, dass alles geschrieben wurde
        } catch (IOException e) {
            e.printStackTrace();
            // Falls das Speichern fehlschlägt und ein Backup existiert, stelle es wieder her
            if (backupFile.exists()) {
                try {
                    java.nio.file.Files.copy(backupFile.toPath(), CONFIG_FILE.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    // Wiederherstellung fehlgeschlagen
                }
            }
        }
    }
}
