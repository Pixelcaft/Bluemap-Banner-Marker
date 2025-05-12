package net.pixelcaft.bmbannermarker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class BMBMCommand {

    private final BMBMConfig BMBMConfig;
    private final BMBannerMarkerManager BMBannerMarkerManager;

    public BMBMCommand(BMBMConfig BMBMConfig, BMBannerMarkerManager BMBannerMarkerManager) {
        this.BMBMConfig = BMBMConfig;
        this.BMBannerMarkerManager = BMBannerMarkerManager;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bmbm")
                .requires(source -> source.hasPermission(0)) // Everyone can execute this command
                .then(Commands.literal("markertypes")
                        .executes(this::showMarkerTypes)
                )
                .then(Commands.literal("info")
                        .executes(this::showInfo)
                )
        );

        dispatcher.register(Commands.literal("bmbm")
                .requires(source -> source.hasPermission(2)) // Only admins can execute this command
                .then(Commands.literal("removemarker")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(this::removeMarker)
                                        )
                                )
                        )
                )
                .then(Commands.literal("reload")
                        .executes(this::reloadConfigAndIcons)
                )
        );
    }

    private int removeMarker(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");

        BlockPos pos = new BlockPos(x, y, z);

        // Remove the marker directly without checking for a banner
        BMBannerMarkerManager.removeMarker(pos);
        source.sendSuccess(() -> Component.literal("Marker at " + pos + " removed successfully."), false);

        return 1; // Command executed
    }

    private int reloadConfigAndIcons(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        try {
            BMBMConfig.loadConfig(); // Reload the configuration
            source.sendSuccess(() -> Component.literal("Configuration reloaded successfully."), false);

            // Reload the icons
            BlueMapAPI.getInstance().ifPresent(api -> {
                BMBannerMapIcons BMBannerMapIcons = new BMBannerMapIcons(BMBMConfig);
                BMBannerMapIcons.loadMapIcons(api);
            });
            source.sendSuccess(() -> Component.literal("Icons reloaded successfully."), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to reload configuration or icons: " + e.getMessage()));
        }
        return 1; // Command executed
    }

    private int showInfo(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        source.sendSuccess(() -> Component.literal("To create a marker, place a banner and name it starting with '#' followed by the marker type."), false);
        source.sendSuccess(() -> Component.literal("Example: Name the banner '#markerType' to create a marker of type 'markerType'."), false);
        return 1; // Command executed successfully
    }

    private int showMarkerTypes(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var markerTypes = BMBMConfig.getMarkerTypes();

        if (markerTypes.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No marker types found in the config."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Marker Types:"), false);
            for (String markerType : markerTypes) {
                source.sendSuccess(() -> Component.literal("- " + markerType), false);
            }
        }

        return 1; // Command executed successfully
    }
}