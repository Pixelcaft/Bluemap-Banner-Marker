package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
        ServerLevel overworld = event.getServer().overworld();
        if (overworld != null) {
            BlueMapAPI.onEnable(blueMapAPI -> {
                LOGGER.info("Starting BmBannerMarker");
                bannerMarkerManager.loadMarkers(overworld);
                bannerMapIcons.loadMapIcons(blueMapAPI);
            });
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        BlueMapAPI.onDisable(blueMapAPI -> {
            LOGGER.info("Stopping BmBannerMarker");
            bannerMarkerManager.saveMarkers();
        });
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Level world = player.getCommandSenderWorld();
            BlockPos pos = event.getPos();
            InteractionHand hand = event.getHand();

            if (!world.dimension().equals(Level.OVERWORLD) || player.isSpectator()) return;

            if (player.getItemInHand(hand).is(Items.FILLED_MAP)) {
                BlockState blockState = world.getBlockState(pos);
                BlockEntity blockEntity = world.getBlockEntity(pos);

                if (blockState.is(BlockTags.BANNERS)) {
                    LOGGER.trace("Toggling marker at {}", pos);
                    bannerMarkerManager.toggleMarker(blockState, blockEntity);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Level world = player.getCommandSenderWorld();
            BlockPos pos = event.getPos();

            if (world.dimension().equals(Level.OVERWORLD)) {
                BlockState state = world.getBlockState(pos);
                if (state.is(BlockTags.BANNERS)) {
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    bannerMarkerManager.removeMarker(blockEntity);
                }
            }
        }
    }
}
