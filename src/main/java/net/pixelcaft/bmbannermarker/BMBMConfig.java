package net.pixelcaft.bmbannermarker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BMBMConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BMBMConfig.class);
    private static final String CONFIG_PATH = "config/bmbm.toml";
    private static final String ASSETS_DIR = "bluemap/web/maps/world/assets";
    private List<String> markerTypes = new ArrayList<>();

    public void loadConfig() {
        File configFile = new File(CONFIG_PATH);

        // Ensure the config file exists
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        // Load marker types from the config file
        markerTypes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            boolean inMarkerTypesSection = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[markerTypes]")) {
                    inMarkerTypesSection = true;
                } else if (line.startsWith("[") && inMarkerTypesSection) {
                    break; // Exit the section
                } else if (inMarkerTypesSection && !line.isEmpty() && !line.startsWith("#")) {
                    markerTypes.add(line);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load configuration file: {}", configFile.getAbsolutePath(), e);
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            // Create the configuration file
            if (configFile.createNewFile()) {
                try (var writer = new FileWriter(configFile)) {
                    writer.write("""
                # bmbm Configuration File
                # To create a custom icon for a marker type, place a PNG file in the assets directory with the name <markerType>-<color>.png
                # Example: For a marker type "marker" and color "red", place a file named "marker-red.png" in the assets directory.
                # The default icon for each color will be used if a specific icon is not found.
                # The icon foler can be found bluemap\\web\\maps\\world
                # Your need to place each copy in the correct world folder world, world_nether, world_the_end
                # The default icons are using the size 64x64 pixels


                # Add your marker types here
                [markerTypes]
                marker
                # Define marker types here
                # Colors will be handled dynamically
                """);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create configuration file: {}", configFile.getAbsolutePath(), e);
        }
    }

    public List<String> getMarkerTypes() {
        return markerTypes;
    }

    public File getIconFile(String markerType, String color) {
        if (markerType.startsWith("#")) {
            markerType = markerType.substring(1);
        }

        String specificIconName = markerType + "-" + color + ".png";
        File specificIconFile = new File(ASSETS_DIR, specificIconName);

        if (specificIconFile.exists()) {
            return specificIconFile;
        }

        LOGGER.warn("Specific icon {} not found, falling back to default icon {}", specificIconName, color + ".png");

        String defaultIconName = color + ".png";
        return new File(ASSETS_DIR, defaultIconName);
    }
}