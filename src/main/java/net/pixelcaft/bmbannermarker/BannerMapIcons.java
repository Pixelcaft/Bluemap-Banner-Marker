package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class BannerMapIcons {

    private static final Logger LOGGER = LoggerFactory.getLogger("BannerMapIcons");
    private final Config config;

    public BannerMapIcons(Config config) {
        this.config = config;
    }

    public void loadMapIcons(BlueMapAPI blueMapAPI) {
        LOGGER.info("Executing loadMapIcons method");
        blueMapAPI.getMaps().forEach(blueMapMap -> {
            var assetStorage = blueMapMap.getAssetStorage();
            for (var markerType : config.getMarkerTypes()) {
                for (var dyeColor : DyeColor.values()) {
                    File iconFile = config.getIconFile(markerType, dyeColor.getName().toLowerCase());
                    String iconName = iconFile.getName();
                    LOGGER.info("Loading icon {} for marker type {}", iconName, markerType);
                    try {
                        if (!assetStorage.assetExists(iconName)) {
                            try (var outStream = assetStorage.writeAsset(iconName);
                                 var stream = iconFile.exists()
                                         ? new java.io.FileInputStream(iconFile)
                                         : BmBannerMarker.class.getResourceAsStream("/assets/bmbannermarker/icons/" + dyeColor.getName().toLowerCase() + ".png")) {
                                if (stream != null) {
                                    outStream.write(stream.readAllBytes());
                                } else {
                                    LOGGER.warn("Default icon for color {} not found.", dyeColor.getName().toLowerCase());
                                }
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Failed to load icon for {}", iconName, e);
                    }
                }
            }
        });
    }
}