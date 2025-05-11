package net.pixelcaft.bmbannermarker;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
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

public class BannerMarkerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("BannerMarkerManager");

    private final String markerJsonFileName = "marker-file.json";
    private final String markerSetLabel = "Map Banners";
    private final String bannerMarkerSetId = "overworldmapbanners";
    private final Config config;

    public BannerMarkerManager(Config config) {
        this.config = config;
    }

    // Add this method to expose the Config instance
    public Config getConfig() {
        return config;
    }


    public void loadMarkers(ServerLevel serverLevel) {
        BlueMapAPI.getInstance()
                .flatMap(blueMapAPI -> blueMapAPI.getWorld(serverLevel))
                .ifPresent(blueMapWorld -> blueMapWorld.getMaps().forEach(blueMapMap -> {
                    String dimensionId = blueMapWorld.getId(); // Get dimension ID
                    File markerFile = new File(markerJsonFileName.replace(".json", "_" + dimensionId + ".json")); // Dimension-specific file
                    MarkerSet markerSet;

                    if (markerFile.exists()) {
                        try (FileReader reader = new FileReader(markerFile)) {
                            markerSet = MarkerGson.INSTANCE.fromJson(reader, MarkerSet.class);
                        } catch (IOException ex) {
                            LOGGER.error("Failed to read marker file for dimension {}", dimensionId, ex);
                            return;
                        }
                    } else {
                        markerSet = MarkerSet.builder()
                                .label(markerSetLabel + " (" + dimensionId + ")")
                                .defaultHidden(false)
                                .toggleable(true)
                                .build();
                    }

                    blueMapMap.getMarkerSets().put(bannerMarkerSetId + "_" + dimensionId, markerSet);
                }));
    }

    public void saveMarkers() {
        BlueMapAPI.getInstance().ifPresent(blueMapAPI -> blueMapAPI.getMaps().forEach(blueMapMap -> blueMapMap.getMarkerSets().forEach((id, markerSet) -> {
            if (id != null && id.startsWith(bannerMarkerSetId)) { // Controleer op prefix
                String dimensionId = id.replace(bannerMarkerSetId + "_", ""); // Haal dimensie-ID uit de MarkerSet-ID
                File markerFile = new File(markerJsonFileName.replace(".json", "_" + dimensionId + ".json")); // Unieke bestandsnaam per dimensie
                try (FileWriter writer = new FileWriter(markerFile)) {
                    MarkerGson.INSTANCE.toJson(markerSet, writer);
                } catch (IOException e) {
                    LOGGER.error("Failed to save marker file for {}", id, e);
                }
            }
        })));
    }

    public void removeMarker(BlockPos pos) {
        BlueMapAPI.getInstance().ifPresent(blueMapAPI -> {
            blueMapAPI.getWorlds().forEach(blueMapWorld -> {
                String dimensionId = blueMapWorld.getId();
                String dimensionMarkerSetId = bannerMarkerSetId + "_" + dimensionId;

                blueMapWorld.getMaps().forEach(blueMapMap -> {
                    var markerSet = blueMapMap.getMarkerSets().get(dimensionMarkerSetId);
                    if (markerSet != null) {
                        markerSet.remove(pos.toShortString());
                    }
                });
            });
        });

        // Sla de markers op na het verwijderen
        saveMarkers();
    }

    public void toggleMarker(BlockState blockState, BlockEntity blockEntity) {
        if (!(blockEntity instanceof BannerBlockEntity bannerBlockEntity)) {
            return;
        }
        BlueMapAPI.getInstance()
                .flatMap(blueMapAPI -> blueMapAPI.getWorld(blockEntity.getLevel()))
                .ifPresent(blueMapWorld -> {
                    String dimensionId = blueMapWorld.getId(); // Dynamically retrieve dimension ID
                    String dimensionMarkerSetId = bannerMarkerSetId + "_" + dimensionId; // Unique MarkerSet ID per dimension

                    blueMapWorld.getMaps().forEach(blueMapMap -> {
                        var existingBannerMarkerSet = blueMapMap.getMarkerSets().computeIfAbsent(dimensionMarkerSetId, id ->
                                MarkerSet.builder().label(markerSetLabel + " (" + dimensionId + ")").defaultHidden(false).toggleable(true).build()
                        );

                        var markerId = blockEntity.getBlockPos().toShortString();
                        var existingMarker = existingBannerMarkerSet.getMarkers().get(markerId);
                        if (existingMarker != null) {
                            existingBannerMarkerSet.remove(markerId);
                        } else if (blockState != null) {
                            String name = bannerBlockEntity.getCustomName() != null
                                    ? bannerBlockEntity.getCustomName().getString()
                                    : Component.translatable(blockState.getBlock().getDescriptionId()).getString();
                            addMarker(name, bannerBlockEntity, existingBannerMarkerSet, blueMapMap);
                        }
                    });
                });

        // Save markers immediately after toggling
        saveMarkers();
    }

    private void addMarker(String blockName, BannerBlockEntity bannerBlockEntity, MarkerSet existingBannerMarkerSet, BlueMapMap blueMapMap) {
        BlockPos blockPos = bannerBlockEntity.getBlockPos();
        var x = blockPos.getX() + 0.5;
        var y = blockPos.getY() + 0.5;
        var z = blockPos.getZ() + 0.5;

        // Verwijder de `#` en het eerste woord, gebruik de rest als label
        String markerLabel = blockName.startsWith("#") ? blockName.substring(1).trim() : blockName;
        int firstSpaceIndex = markerLabel.indexOf(" ");
        if (firstSpaceIndex != -1) {
            markerLabel = markerLabel.substring(firstSpaceIndex + 1).trim();
        } else {
            markerLabel = "default"; // Fallback als er geen extra tekst is
        }

        // Retrieve the correct icon file
        String color = bannerBlockEntity.getBaseColor().getName().toLowerCase();
        File iconFile = config.getIconFile(blockName.split(" ")[0].substring(1), color); // Gebruik het eerste woord voor het icoon
        var iconAddress = blueMapMap.getAssetStorage().getAssetUrl(iconFile.getName());

        if (iconAddress == null) {
            LOGGER.warn("Icon address is null for marker label: {} and color: {}", markerLabel, color);
            return;
        }

        LOGGER.info("Adding marker with icon {} at position ({}, {}, {})", iconAddress, x, y, z);

        POIMarker bannerMarker = POIMarker.builder()
                .label(markerLabel) // Gebruik de aangepaste markerLabel
                .position(x, y, z)
                .icon(iconAddress, 32, 32)
                .maxDistance(1000)
                .build();

        existingBannerMarkerSet.put(blockPos.toShortString(), bannerMarker);
        saveMarkers();
    }
}