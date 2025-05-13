package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BMBannerMarkerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("BannerMarkerManager");

    private final String markerJsonFileName = "marker-file.json";
    private final String markerSetLabel = "Map Banners";
    private final String bannerMarkerSetId = "mapbannermarkerset";
    private final BMBMConfig BMBMConfig;

    public BMBannerMarkerManager(BMBMConfig BMBMConfig) {
        this.BMBMConfig = BMBMConfig;
    }

    // Add this method to expose the Config instance
    public BMBMConfig getConfig() {
        return BMBMConfig;
    }

    private void ensureFolderExists(File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            LOGGER.error("Failed to create folder: {}", folder.getAbsolutePath());
        }
    }

    private void ensureFileExists(File file) {
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    LOGGER.debug("Created new file: {}", file.getAbsolutePath());
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create file: {}", file.getAbsolutePath(), e);
            }
        }
    }


    public void loadMarkers(ServerLevel serverLevel) {
        BlueMapAPI.getInstance()
                .flatMap(blueMapAPI -> blueMapAPI.getWorld(serverLevel))
                .ifPresent(blueMapWorld -> blueMapWorld.getMaps().forEach(blueMapMap -> {
                    String dimensionId = blueMapWorld.getId(); // Get dimension ID
                    String safeDimensionId = dimensionId.replace(":", "_"); // Sanitize dimensionId for filenames
                    File markerFile = new File("config/bmbm/bmbm-" + safeDimensionId + ".json"); // Dimension-specific file
                    MarkerSet markerSet;

                    if (markerFile.exists()) {
                        try (FileReader reader = new FileReader(markerFile)) {
                            markerSet = MarkerGson.INSTANCE.fromJson(reader, MarkerSet.class);
                        } catch (IOException ex) {
                            LOGGER.error("Failed to read marker file for dimension {}", safeDimensionId, ex);
                            return;
                        }
                    } else {
                        markerSet = MarkerSet.builder()
                                .label(markerSetLabel + " (" + dimensionId + ")")
                                .defaultHidden(false)
                                .toggleable(true)
                                .build();
                    }

                    blueMapMap.getMarkerSets().put(bannerMarkerSetId + "_" + safeDimensionId, markerSet);
                }));
    }

    public void saveMarkers(String dimensionId) {
        File folder = new File("config/bmbm");
        ensureFolderExists(folder);

        BlueMapAPI.getInstance().ifPresent(blueMapAPI -> {
            blueMapAPI.getMaps().forEach(blueMapMap -> {
                String safeDimensionId = dimensionId.replace(":", "_"); // Sanitize dimensionId
                String markerSetId = bannerMarkerSetId + "_" + safeDimensionId;

                var markerSet = blueMapMap.getMarkerSets().get(markerSetId);
                if (markerSet != null) {
                    File markerFile = new File(folder, "bmbm-" + safeDimensionId + ".json");
                    ensureFileExists(markerFile);

                    try (FileWriter writer = new FileWriter(markerFile)) {
                        MarkerGson.INSTANCE.toJson(markerSet, writer);
                        LOGGER.debug("Saved markers for dimension {} to {}", safeDimensionId, markerFile.getAbsolutePath());
                    } catch (IOException e) {
                        LOGGER.error("Failed to save marker file for {}", safeDimensionId, e);
                    }
                }
            });
        });
    }

    public void removeMarker(BlockPos pos) {
        BlueMapAPI.getInstance().ifPresent(blueMapAPI -> {
            blueMapAPI.getWorlds().forEach(blueMapWorld -> {
                String dimensionId = blueMapWorld.getId(); // Retrieve the dimension ID
                String safeDimensionId = dimensionId.replace(":", "_"); // Sanitize dimensionId

                blueMapWorld.getMaps().forEach(blueMapMap -> {
                    var markerSet = blueMapMap.getMarkerSets().get(bannerMarkerSetId + "_" + safeDimensionId);
                    if (markerSet != null) {
                        markerSet.remove(pos.toShortString());
                    }
                });

                // Save markers for the specific dimension
                saveMarkers(dimensionId);
            });
        });
    }

    public void toggleMarker(BlockState blockState, BlockEntity blockEntity) {
        if (!(blockEntity instanceof BannerBlockEntity bannerBlockEntity)) {
            return;
        }

        BlueMapAPI.getInstance()
                .flatMap(blueMapAPI -> blueMapAPI.getWorld(blockEntity.getLevel()))
                .ifPresent(blueMapWorld -> {
                    String dimensionId = blueMapWorld.getId(); // Dynamically retrieve dimension ID
                    String safeDimensionId = dimensionId.replace(":", "_"); // Sanitize dimensionId

                    blueMapWorld.getMaps().forEach(blueMapMap -> {
                        var existingBannerMarkerSet = blueMapMap.getMarkerSets().computeIfAbsent(bannerMarkerSetId + "_" + safeDimensionId, id ->
                                MarkerSet.builder().label(markerSetLabel + " (" + dimensionId + ")").defaultHidden(false).toggleable(true).build()
                        );

                        var markerId = blockEntity.getBlockPos().toShortString();
                        var existingMarker = existingBannerMarkerSet.getMarkers().get(markerId);
                        if (existingMarker != null) {
                            existingBannerMarkerSet.remove(markerId);
                            LOGGER.debug("Removed existing marker at {}", blockEntity.getBlockPos());
                        } else if (blockState != null && (blockState.getBlock() instanceof BannerBlock || blockState.getBlock().getClass().getSimpleName().equals("WallBannerBlock"))) {
                            String name = bannerBlockEntity.getCustomName() != null
                                    ? bannerBlockEntity.getCustomName().getString()
                                    : Component.translatable(blockState.getBlock().getDescriptionId()).getString();
                            LOGGER.debug("Adding marker for block: {}", blockState.getBlock().getDescriptionId());
                            addMarker(name, bannerBlockEntity, existingBannerMarkerSet, blueMapMap, dimensionId);
                        } else {
                            LOGGER.warn("Block is not a valid BannerBlock or WallBannerBlock: {}", blockState.getBlock().getDescriptionId());
                        }
                    });

                });
    }

    private void addMarker(String blockName, BannerBlockEntity bannerBlockEntity, MarkerSet existingBannerMarkerSet, BlueMapMap blueMapMap, String dimensionId) {
        BlockPos blockPos = bannerBlockEntity.getBlockPos();
        var x = blockPos.getX() + 0.5;
        var y = blockPos.getY() + 0.5;
        var z = blockPos.getZ() + 0.5;

        // Remove the `#` and first word, use the rest as label
        String markerLabel = blockName.startsWith("#") ? blockName.substring(1).trim() : blockName;
        int firstSpaceIndex = markerLabel.indexOf(" ");
        if (firstSpaceIndex != -1) {
            markerLabel = markerLabel.substring(firstSpaceIndex + 1).trim();
        } else {
            markerLabel = "Marker"; // Fallback if there's no extra text
        }

        // Retrieve the correct icon file
        String color = bannerBlockEntity.getBaseColor().getName().toLowerCase();
        File iconFile = BMBMConfig.getIconFile(blockName.split(" ")[0].substring(1), color); // Use first word for icon
        if (!iconFile.exists()) {
            LOGGER.warn("Icon file does not exist for marker label: {} and color: {}", markerLabel, color);
            return;
        }

        var iconAddress = blueMapMap.getAssetStorage().getAssetUrl(iconFile.getName());
        if (iconAddress == null) {
            LOGGER.warn("Icon address is null for marker label: {} and color: {}", markerLabel, color);
            return;
        }

        LOGGER.debug("Adding marker '{}' with icon '{}' at position ({}, {}, {})", markerLabel, iconAddress, x, y, z);

        POIMarker bannerMarker = POIMarker.builder()
                .label(markerLabel) // Use the custom markerLabel
                .position(x, y, z)
                .icon(iconAddress, 32, 32)
                .maxDistance(1000)
                .build();

        existingBannerMarkerSet.put(blockPos.toShortString(), bannerMarker);

        // Save markers for the specific dimension
        saveMarkers(dimensionId);
    }
}