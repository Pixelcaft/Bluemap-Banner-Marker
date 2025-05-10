package net.pixelcaft.bmbannermarker;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(BmBannerMarker.MODID)
public class BmBannerMarker {

    public static final String MODID = "pixelcaftsbmbannermarker";

    public BmBannerMarker(IEventBus modEventBus) {
        Config config = new Config();
        config.loadConfig();
        BannerMarkerManager bannerMarkerManager = new BannerMarkerManager(config);
        BannerMapIcons bannerMapIcons = new BannerMapIcons(config);

        // Mod lifecycle events
        modEventBus.register(new ModEventHandler());

        // Spel / server events
        NeoForge.EVENT_BUS.register(new CommonEventHandler(bannerMarkerManager, bannerMapIcons));
    }
}