package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BannerMapIcons {

    private static final Logger LOGGER = LoggerFactory.getLogger("BannerMapIcons");

    public void loadMapIcons(BlueMapAPI blueMapAPI) {
        blueMapAPI.getMaps().forEach(blueMapMap -> {
            var assetStorage = blueMapMap.getAssetStorage();
            for (var dyeColor : DyeColor.values()) {
                var iconName = dyeColor.getName().toLowerCase() + ".png"; // Use `getName()` for NeoForge
                try {
                    if (!assetStorage.assetExists(iconName)) {
                        try (var outStream = assetStorage.writeAsset(iconName);
                             var stream = BmBannerMarker.class.getResourceAsStream("/assets/bmbannermarker/icons/banners/" + iconName)) {
                            if (stream != null) {
                                LOGGER.trace("Writing icon {} to map {}", iconName, blueMapMap);
                                outStream.write(stream.readAllBytes());
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