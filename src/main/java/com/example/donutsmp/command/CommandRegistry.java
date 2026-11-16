package com.example.donutsmp.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class CommandRegistry {
    private CommandRegistry() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EconomyCommands.register(dispatcher);
            TeleportCommands.register(dispatcher);
        });
    }
}
