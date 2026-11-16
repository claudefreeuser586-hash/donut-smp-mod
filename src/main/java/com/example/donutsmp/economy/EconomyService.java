package com.example.donutsmp.economy;

import com.example.donutsmp.DonutSMP;
import com.example.donutsmp.data.PlayerData;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared buy / sell / pay logic used by commands and the shop GUI. */
public final class EconomyService {
    private EconomyService() {}

    public record Result(boolean success, String message) {}

    public static PlayerData data(ServerPlayerEntity p) {
        return DonutSMP.getPlayerData().getOrCreate(p.getUuid());
    }

    public static void save(ServerPlayerEntity p) {
        DonutSMP.getPlayerData().save(p.getUuid());
    }

    public static String itemId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    public static String format(long cents) {
        return MoneyFormatter.format(cents);
    }

    /** Transfers money from one player to another. */
    public static Result pay(ServerPlayerEntity from, ServerPlayerEntity to, long cents) {
        if (cents <= 0) return fail("Amount must be greater than zero.");
        PlayerData sender = data(from);
        if (!sender.removeMoneyCents(cents)) {
            return fail("You don't have enough money. Balance: " + format(sender.getMoneyCents()));
        }
        data(to).addMoneyCents(cents);
        save(from);
        save(to);
        return ok("Sent " + format(cents) + " to " + to.getName().getString() + ".");
    }

    /** Sells the item in the main hand at its configured price. */
    public static Result sellHeld(ServerPlayerEntity p) {
        ItemStack held = p.getMainHandStack();
        if (held.isEmpty()) return fail("You are not holding anything.");

        long unit = DonutSMP.getPrices().getSellPriceCents(itemId(held));
        if (unit < 0) return fail("This item cannot be sold.");

        int count = held.getCount();
        long total = unit * count;
        String name = held.getName().getString();

        p.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        data(p).addMoneyCents(total);
        save(p);
        return ok("Sold " + count + "x " + name + " for " + format(total) + ".");
    }

    /** Sells every sellable item in the main inventory. Returns receipt lines for chat. */
    public static List<String> sellAll(ServerPlayerEntity p) {
        PlayerInventory inv = p.getInventory();
        Map<String, long[]> sold = new LinkedHashMap<>();
        long total = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            String id = itemId(stack);
            long unit = DonutSMP.getPrices().getSellPriceCents(id);
            if (unit < 0) continue;

            long value = unit * stack.getCount();
            long[] entry = sold.computeIfAbsent(id, k -> new long[2]);
            entry[0] += stack.getCount();
            entry[1] += value;
            total += value;
            inv.setStack(i, ItemStack.EMPTY);
        }

        List<String> lines = new ArrayList<>();
        if (total == 0) {
            lines.add("Nothing sellable in your inventory.");
            return lines;
        }

        lines.add("=== Sell Receipt ===");
        for (Map.Entry<String, long[]> e : sold.entrySet()) {
            lines.add(shortName(e.getKey()) + " x" + e.getValue()[0] + " = " + format(e.getValue()[1]));
        }
        lines.add("Total: " + format(total));

        data(p).addMoneyCents(total);
        save(p);
        return lines;
    }

    /** Buys {@code count} of an item if the player has money and inventory space. */
    public static Result buy(ServerPlayerEntity p, String itemId, int count) {
        if (count <= 0) return fail("Invalid amount.");

        long unit = DonutSMP.getPrices().getBuyPriceCents(itemId);
        Identifier id = Identifier.of(itemId);
        if (unit < 0 || !Registries.ITEM.containsId(id)) return fail("This item is not for sale.");

        Item item = Registries.ITEM.get(id);
        long total = unit * count;
        PlayerData d = data(p);

        if (d.getMoneyCents() < total) {
            return fail("You need " + format(total) + " for that. Balance: " + format(d.getMoneyCents()));
        }
        if (!canFit(p.getInventory(), item, count)) {
            return fail("Not enough inventory space.");
        }

        d.removeMoneyCents(total);
        give(p, item, count);
        save(p);
        return ok("Bought " + count + "x " + item.getName().getString() + " for " + format(total) + ".");
    }

    private static boolean canFit(PlayerInventory inv, Item item, int count) {
        int remaining = count;
        int max = item.getMaxCount();
        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) {
                remaining -= max;
            } else if (s.isOf(item)) {
                remaining -= (max - s.getCount());
            }
        }
        return remaining <= 0;
    }

    private static void give(ServerPlayerEntity p, Item item, int count) {
        int remaining = count;
        int max = item.getMaxCount();
        while (remaining > 0) {
            int amount = Math.min(remaining, max);
            ItemStack stack = new ItemStack(item, amount);
            if (!p.getInventory().insertStack(stack)) {
                p.dropItem(stack, false);
            }
            remaining -= amount;
        }
    }

    private static String shortName(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static Result ok(String msg) { return new Result(true, msg); }
    private static Result fail(String msg) { return new Result(false, msg); }
}
