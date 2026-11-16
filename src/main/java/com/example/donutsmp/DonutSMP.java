package com.example.donutsmp;

import com.example.donutsmp.data.PlayerDataManager;
import com.example.donutsmp.economy.PriceManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class DonutSMP implements ModInitializer {
    public static final String MOD_ID = "donutsmp";

    private static PlayerDataManager playerData;
    private static PriceManager prices;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (playerData != null) playerData.saveAll();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                playerData.getOrCreate(handler.getPlayer().getUUID()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                playerData.save(handler.getPlayer().getUUID()));
    }

    private void onServerStarted(MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID);
        Path configDir = net_fabricmc_configDir().resolve(MOD_ID);

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

    private static Path net_fabricmc_configDir() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
    }
}
