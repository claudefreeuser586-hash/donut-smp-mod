package com.example.donutsmp;

import com.example.donutsmp.command.CommandRegistry;
import com.example.donutsmp.data.PlayerData;
import com.example.donutsmp.data.PlayerDataManager;
import com.example.donutsmp.economy.PriceManager;
import com.example.donutsmp.teleport.TeleportManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class DonutSMP implements ModInitializer {
    public static final String MOD_ID = "donutsmp";

    private static PlayerDataManager playerData;
    private static PriceManager prices;

    @Override
    public void onInitialize() {
        CommandRegistry.register();
        TeleportManager.init();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (playerData != null) playerData.saveAll();
            TeleportManager.clear();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (playerData == null) return;
            PlayerData data = playerData.getOrCreate(player.getUuid());
            data.setLastKnownName(player.getName().getString());
            playerData.save(player.getUuid());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (playerData != null) playerData.save(handler.getPlayer().getUUID());
        });
    }

    private void onServerStarted(MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID);
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

        prices = new PriceManager(configDir.resolve("prices.json"));
        prices.loadOrGenerate();

        playerData = new PlayerDataManager(worldDir.resolve("players"));
        playerData.loadAll();
    }

    public static PlayerDataManager getPlayerData() {
        return playerData;
    }

    public static PriceManager getPrices() {
        return prices;
    }
}
