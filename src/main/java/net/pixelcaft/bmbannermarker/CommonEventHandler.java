package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("pixelcaftsbmbannermarker");
    private final BannerMarkerManager bannerMarkerManager;
    private final BannerMapIcons bannerMapIcons;

    public CommonEventHandler(BannerMarkerManager bannerMarkerManager, BannerMapIcons bannerMapIcons) {
        this.bannerMarkerManager = bannerMarkerManager;
        this.bannerMapIcons = bannerMapIcons;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Starting BmBannerMarker");

        BlueMapAPI.onEnable(blueMapAPI -> {
            event.getServer().getAllLevels().forEach(serverLevel -> {
                LOGGER.info("Loading markers for dimension: {}", serverLevel.dimension().location());
                bannerMarkerManager.loadMarkers(serverLevel);
            });

            bannerMapIcons.loadMapIcons(blueMapAPI);
        });

        // Registreer de BmCommand
        var dispatcher = event.getServer().getCommands().getDispatcher();
        new BmCommand(bannerMarkerManager.getConfig(), bannerMarkerManager).register(dispatcher);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        BlueMapAPI.onDisable(blueMapAPI -> {
            LOGGER.info("Stopping BmBannerMarker");
            bannerMarkerManager.saveMarkers();
        });
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // Alleen server-side
        if (event.getLevel().isClientSide()) return;

        // Controleer of de entiteit een speler is
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Alleen als het om een banner gaat
        if (!(event.getPlacedBlock().getBlock() instanceof BannerBlock)) return;

        BlockPos pos = event.getPos();
        BlockState blockState = event.getPlacedBlock();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);

        if (blockEntity instanceof BannerBlockEntity bannerBlockEntity) {
            if (bannerBlockEntity.getCustomName() != null && bannerBlockEntity.getCustomName().getString().startsWith("#")) {
                // Haal alleen het eerste woord na de #
                String markerType = bannerBlockEntity.getCustomName().getString().substring(1).split(" ")[0];
                if (bannerMarkerManager.getConfig().getMarkerTypes().contains(markerType)) {
                    // Marker type is geldig
                    LOGGER.trace("Player {} placed a valid marker at {}", player.getName().getString(), pos);
                    bannerMarkerManager.toggleMarker(blockState, blockEntity);
                } else {
                    // Ongeldig marker type
                    LOGGER.warn("Player {} placed an invalid marker type '{}' at {}", player.getName().getString(), markerType, pos);
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
                    bannerMarkerManager.removeMarker(blockEntity.getBlockPos());
                }
            }
        }
    }
}
