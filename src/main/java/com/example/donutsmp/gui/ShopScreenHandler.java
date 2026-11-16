package com.example.donutsmp.gui;

import com.example.donutsmp.DonutSMP;
import com.example.donutsmp.economy.EconomyService;
import com.example.donutsmp.economy.MoneyFormatter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Chest GUI for the shop. Shop slots cannot be picked up or filled.
 * Left click buys 1, shift click buys one full stack.
 */
public class ShopScreenHandler extends ScreenHandler {
    private static final int MENU_CATEGORY_START = 11; // middle row of the 9x3 menu
    private static final int MAX_ITEMS = 45;           // rows 0 to 4 of the category page
    private static final int BACK_SLOT = 49;           // bottom middle of the 9x6 page

    private final ShopCategory category; // null means the category selector
    private final List<String> items;
    private final int shopSlotCount;
    private final SimpleInventory shopInv;

    public ShopScreenHandler(int syncId, PlayerInventory playerInv, ShopCategory category) {
        super(category == null ? ScreenHandlerType.GENERIC_9X3 : ScreenHandlerType.GENERIC_9X6, syncId);
        this.category = category;
        this.items = category == null ? List.of() : filterPriced(category);
        this.shopSlotCount = category == null ? 27 : 54;
        this.shopInv = new SimpleInventory(shopSlotCount);

        fillDisplay();

        int rows = shopSlotCount / 9;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new LockedSlot(shopInv, r * 9 + c, 8 + c * 18, 18 + r * 18));
            }
        }

        int playerY = rows * 18 + 30; // 84 for 3 rows, matching vanilla layout
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, playerY + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(playerInv, c, 8 + c * 18, playerY + 58));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < shopSlotCount) {
            if (player instanceof ServerPlayerEntity sp) {
                handleShopClick(sp, slotIndex, button, actionType);
            }
            return;
        }
        if (actionType == SlotActionType.QUICK_MOVE) return;
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    private void handleShopClick(ServerPlayerEntity p, int slot, int button, SlotActionType type) {
        boolean shift = type == SlotActionType.QUICK_MOVE;
        boolean left = type == SlotActionType.PICKUP && button == 0;
        if (!shift && !left) return;

        if (category == null) {
            int idx = slot - MENU_CATEGORY_START;
            ShopCategory[] all = ShopCategory.values();
            if (idx >= 0 && idx < all.length) {
                ShopMenu.open(p, all[idx]);
            }
            return;
        }

        if (slot == BACK_SLOT) {
            ShopMenu.openMenu(p);
            return;
        }
        if (slot >= items.size()) return;

        String id = items.get(slot);
        int count = shift ? maxStack(id) : 1;
        EconomyService.Result result = EconomyService.buy(p, id, count);
        p.sendMessage(Text.literal(result.message()), false);
    }

    private void fillDisplay() {
        if (category == null) {
            ShopCategory[] all = ShopCategory.values();
            for (int i = 0; i < all.length; i++) {
                shopInv.setStack(MENU_CATEGORY_START + i, displayStack(
                        all[i].iconId, all[i].title, List.of(Text.literal("Click to open"))));
            }
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            String id = items.get(i);
            long buy = DonutSMP.getPrices().getBuyPriceCents(id);
            List<Text> lore = List.of(
                    Text.literal("Buy: " + MoneyFormatter.format(buy) + " each"),
                    Text.literal("Left click: x1"),
                    Text.literal("Shift click: x" + maxStack(id)));
            shopInv.setStack(i, displayStack(id, itemName(id), lore));
        }

        shopInv.setStack(BACK_SLOT, displayStack("minecraft:arrow", "Back",
                List.of(Text.literal("Return to categories"))));
    }

    private static List<String> filterPriced(ShopCategory c) {
        return c.itemIds.stream()
                .filter(id -> DonutSMP.getPrices().getBuyPriceCents(id) >= 0)
                .filter(id -> Registries.ITEM.containsId(Identifier.of(id)))
                .limit(MAX_ITEMS)
                .toList();
    }

    private static ItemStack displayStack(String id, String title, List<Text> lore) {
        Item item = Registries.ITEM.get(Identifier.of(id));
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(title));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    private static String itemName(String id) {
        return Registries.ITEM.get(Identifier.of(id)).getName().getString();
    }

    private static int maxStack(String id) {
        return Registries.ITEM.get(Identifier.of(id)).getMaxCount();
    }

    /** Slot that cannot receive or release items. Used for the shop display. */
    private static class LockedSlot extends Slot {
        LockedSlot(SimpleInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return false;
        }
    }
}
