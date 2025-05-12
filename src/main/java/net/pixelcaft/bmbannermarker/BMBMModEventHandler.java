package net.pixelcaft.bmbannermarker;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BMBMModEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("pixelcaftsbmbannermarker");

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("BmBannerMarker mod setup complete.");
    }
}
