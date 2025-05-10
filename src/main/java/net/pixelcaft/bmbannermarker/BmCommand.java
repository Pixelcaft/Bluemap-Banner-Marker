package net.pixelcaft.bmbannermarker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class BmCommand {

    private final Config config;

    public BmCommand(Config config) {
        this.config = config;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bmbm")
                .requires(source -> source.hasPermission(0)) // Iedereen kan dit command uitvoeren
                .then(Commands.literal("markertypes")
                        .executes(this::showMarkerTypes)
                )
                .then(Commands.literal("info")
                        .executes(this::showInfo)
                )
        );
    }

    private int showInfo(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        source.sendSuccess(() -> Component.literal("To create a marker, place a banner and name it starting with '#' followed by the marker type."), false);
        source.sendSuccess(() -> Component.literal("Example: Name the banner '#markerType' to create a marker of type 'markerType'."), false);
        return 1; // Command executed successfully
    }

    private int showMarkerTypes(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var markerTypes = config.getMarkerTypes();

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