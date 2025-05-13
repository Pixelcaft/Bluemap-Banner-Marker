package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BMBMCommonEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("pixelcaftsbmbannermarker");
    private final BMBannerMarkerManager BMBannerMarkerManager;
    private final BMBannerMapIcons BMBannerMapIcons;

    public BMBMCommonEventHandler(BMBannerMarkerManager BMBannerMarkerManager, BMBannerMapIcons BMBannerMapIcons) {
        this.BMBannerMarkerManager = BMBannerMarkerManager;
        this.BMBannerMapIcons = BMBannerMapIcons;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Starting BmBannerMarker");

        BlueMapAPI.onEnable(blueMapAPI -> {
            event.getServer().getAllLevels().forEach(serverLevel -> {
                BMBannerMarkerManager.loadMarkers(serverLevel);
            });

            BMBannerMapIcons.loadMapIcons(blueMapAPI);
        });

        var dispatcher = event.getServer().getCommands().getDispatcher();
        new BMBMCommand(BMBannerMarkerManager.getConfig(), BMBannerMarkerManager).register(dispatcher);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        BlueMapAPI.onDisable(blueMapAPI -> {
            LOGGER.info("Stopping BmBannerMarker");
            blueMapAPI.getWorlds().forEach(blueMapWorld -> {
                String dimensionId = blueMapWorld.getId(); // Retrieve the dimension ID
                BMBannerMarkerManager.saveMarkers(dimensionId); // Save markers for each dimension
            });
        });
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BlockState blockState = event.getPlacedBlock();

        if (!blockState.is(BlockTags.BANNERS)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);

        if (blockEntity instanceof BannerBlockEntity bannerBlockEntity) {
            if (bannerBlockEntity.getCustomName() != null && bannerBlockEntity.getCustomName().getString().startsWith("#")) {
                String markerType = bannerBlockEntity.getCustomName().getString().substring(1).split(" ")[0];

                if (BMBannerMarkerManager.getConfig().getMarkerTypes().contains(markerType)) {
                    BMBannerMarkerManager.toggleMarker(blockState, blockEntity);
                } else {
                    LOGGER.warn("Invalid marker type: {}", markerType);
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level world)) return; // Ensure the level is of type Level

        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);

        if (state.is(BlockTags.BANNERS)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof BannerBlockEntity bannerBlockEntity) {
                if (bannerBlockEntity.getCustomName() != null && bannerBlockEntity.getCustomName().getString().startsWith("#")) {
                    // Remove the marker without validation
                    BMBannerMarkerManager.removeMarker(blockEntity.getBlockPos());
                }
            }
        }
    }
}
