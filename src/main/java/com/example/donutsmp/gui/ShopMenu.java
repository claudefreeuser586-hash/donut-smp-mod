package com.example.donutsmp.gui;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;

public final class ShopMenu {
    private ShopMenu() {}

    /** Opens the category selector (/shop). */
    public static void openMenu(ServerPlayerEntity player) {
        open(player, null);
    }

    /** Opens a category page, or the category selector if category is null. */
    public static void open(ServerPlayerEntity player, ShopCategory category) {
        String title = category == null ? "Shop" : "Shop - " + category.title;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, p) -> new ShopScreenHandler(syncId, inventory, category),
                Text.literal(title)));
    }
}
