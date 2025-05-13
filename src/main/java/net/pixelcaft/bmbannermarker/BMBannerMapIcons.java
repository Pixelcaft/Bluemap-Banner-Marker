package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class BMBannerMapIcons {

    private static final Logger LOGGER = LoggerFactory.getLogger("BannerMapIcons");
    private final BMBMConfig BMBMConfig;

    public BMBannerMapIcons(BMBMConfig BMBMConfig) {
        this.BMBMConfig = BMBMConfig;
    }

    public void loadMapIcons(BlueMapAPI blueMapAPI) {
        LOGGER.debug("Executing loadMapIcons method");
        blueMapAPI.getMaps().forEach(blueMapMap -> {
            var assetStorage = blueMapMap.getAssetStorage();
            for (var markerType : BMBMConfig.getMarkerTypes()) {
                for (var dyeColor : DyeColor.values()) {
                    File iconFile = BMBMConfig.getIconFile(markerType, dyeColor.getName().toLowerCase());
                    String iconName = iconFile.getName();
                    try {
                        if (!assetStorage.assetExists(iconName)) {
                            try (var outStream = assetStorage.writeAsset(iconName);
                                 var stream = iconFile.exists()
                                         ? new java.io.FileInputStream(iconFile)
                                         : BMBannerMarker.class.getResourceAsStream("/assets/bmbannermarker/icons/" + dyeColor.getName().toLowerCase() + ".png")) {
                                if (stream != null) {
                                    outStream.write(stream.readAllBytes());
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