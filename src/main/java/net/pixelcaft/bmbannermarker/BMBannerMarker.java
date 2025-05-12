package net.pixelcaft.bmbannermarker;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(BMBannerMarker.MODID)
public class BMBannerMarker {

    public static final String MODID = "pixelcaftsbmbannermarker";

    public BMBannerMarker(IEventBus modEventBus) {
        BMBMConfig BMBMConfig = new BMBMConfig();
        BMBMConfig.loadConfig();
        BMBannerMarkerManager BMBannerMarkerManager = new BMBannerMarkerManager(BMBMConfig);
        BMBannerMapIcons BMBannerMapIcons = new BMBannerMapIcons(BMBMConfig);

        // Mod lifecycle events
        modEventBus.register(new BMBMModEventHandler());

        // Spel / server events
        NeoForge.EVENT_BUS.register(new BMBMCommonEventHandler(BMBannerMarkerManager, BMBannerMapIcons));
    }
}