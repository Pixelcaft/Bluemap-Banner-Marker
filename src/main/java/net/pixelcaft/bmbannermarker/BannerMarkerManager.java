package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BannerMarkerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("BannerMarkerManager");

    private final String markerJsonFileName = "marker-file.json";
    private final String markerSetLabel = "Map Banners";
    private final String bannerMarkerSetId = "overworldmapbanners";

    public void loadMarkers(ServerLevel overworld) {
        MarkerSet bannerMarkerSet = getMarkerSet();
        var optionalApi = BlueMapAPI.getInstance();
        if (bannerMarkerSet == null || optionalApi.isEmpty()) {
            return;
        }
        var api = optionalApi.get();

        api.getWorld(overworld).ifPresent(blueMapWorld -> blueMapWorld.getMaps().forEach(blueMapMap -> blueMapMap.getMarkerSets().put(bannerMarkerSetId, bannerMarkerSet)));
    }

    private MarkerSet getMarkerSet() {
        File markerFile = new File(markerJsonFileName);
        if (markerFile.exists()) {
            try (FileReader reader = new FileReader(markerJsonFileName)) {
                return MarkerGson.INSTANCE.fromJson(reader, MarkerSet.class);
            } catch (IOException ex) {
                LOGGER.error("Failed to read marker file", ex);
            }
        } else {
            return MarkerSet.builder().label(markerSetLabel).defaultHidden(false).toggleable(true).build();
        }
        return null;
    }

    public void saveMarkers() {
        BlueMapAPI.getInstance().ifPresent(blueMapAPI -> blueMapAPI.getMaps().forEach(blueMapMap -> blueMapMap.getMarkerSets().forEach((id, markerSet) -> {
            if (id != null && id.equals(bannerMarkerSetId)) {
                try (FileWriter writer = new FileWriter(markerJsonFileName)) {
                    MarkerGson.INSTANCE.toJson(markerSet, writer);
                } catch (IOException e) {
                    LOGGER.error("Failed to save marker file", e);
                }
            }
        })));
    }

    public void removeMarker(BlockEntity blockEntity) {
        toggleMarker(null, blockEntity);
    }

    public void toggleMarker(BlockState blockState, BlockEntity blockEntity) {
        if (!(blockEntity instanceof BannerBlockEntity bannerBlockEntity)) {
            return;
        }
        BlueMapAPI.getInstance().flatMap(blueMapAPI -> blueMapAPI.getWorld((ServerLevel) blockEntity.getLevel())).ifPresent(blueMapWorld -> {
            blueMapWorld.getMaps().forEach(blueMapMap -> {
                var existingBannerMarkerSet = blueMapMap.getMarkerSets().get(bannerMarkerSetId);
                if (existingBannerMarkerSet == null) {
                    return;
                }
                var markerId = blockEntity.getBlockPos().toShortString();
                var existingMarker = existingBannerMarkerSet.getMarkers().get(markerId);
                if (existingMarker != null) {
                    existingBannerMarkerSet.remove(markerId);
                } else if (blockState != null) {
                    String name;
                    if (bannerBlockEntity.getCustomName() != null) {
                        name = bannerBlockEntity.getCustomName().getString();
                    } else {
                        var blockTranslationKey = blockState.getBlock().getDescriptionId();
                        name = Component.translatable(blockTranslationKey).getString();
                    }
                    addMarker(name, bannerBlockEntity, existingBannerMarkerSet, blueMapMap);
                }
            });
        });
    }

    private void addMarker(String blockName, BannerBlockEntity bannerBlockEntity, MarkerSet existingBannerMarkerSet, BlueMapMap blueMapMap) {
        BlockPos blockPos = bannerBlockEntity.getBlockPos();
        var x = blockPos.getX() + 0.5;
        var y = blockPos.getY() + 0.5;
        var z = blockPos.getZ() + 0.5;
        String iconAddress = blueMapMap.getAssetStorage().getAssetUrl(bannerBlockEntity.getBaseColor().getName() + ".png");
        POIMarker bannerMarker = POIMarker.builder().label(blockName).position(x, y, z).icon(iconAddress, 0, 0).build();
        existingBannerMarkerSet.put(blockPos.toShortString(), bannerMarker);
    }
}