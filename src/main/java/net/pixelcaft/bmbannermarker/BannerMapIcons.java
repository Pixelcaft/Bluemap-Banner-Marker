package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BannerMapIcons {

    private static final Logger LOGGER = LoggerFactory.getLogger("BannerMapIcons");

    public void loadMapIcons(BlueMapAPI blueMapAPI) {
       LOGGER.info("Executing loadMapIcons method");
        blueMapAPI.getMaps().forEach(blueMapMap -> {
            var assetStorage = blueMapMap.getAssetStorage();
            for (var dyeColor : DyeColor.values()) {
                var iconName = dyeColor.getName().toLowerCase() + ".png";
                var resourcePath = "/assets/bmbannermarker/icons/banners/" + iconName;
                LOGGER.info("Loading icon {} from resource path {}", iconName, resourcePath);
                try {
                    if (!assetStorage.assetExists(iconName)) {
                        try (var outStream = assetStorage.writeAsset(iconName);
                             var stream = BmBannerMarker.class.getResourceAsStream(resourcePath)) {
                            if (stream != null) {
                                LOGGER.warn("Writing icon {} to map {}", iconName, blueMapMap);
                                outStream.write(stream.readAllBytes());
                            } else {
                                LOGGER.warn("Icon resource not found: {}", resourcePath);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to create an icon for {}", iconName, e);
                }
            }
        });
    }
}