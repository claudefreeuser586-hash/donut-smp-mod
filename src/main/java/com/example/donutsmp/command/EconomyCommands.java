package com.example.donutsmp.command;

import com.example.donutsmp.DonutSMP;
import com.example.donutsmp.data.PlayerData;
import com.example.donutsmp.economy.EconomyService;
import com.example.donutsmp.economy.MoneyFormatter;
import com.example.donutsmp.economy.PriceManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class EconomyCommands {
    private enum EcoOp { GIVE, TAKE, SET }

    private EconomyCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        for (String name : List.of("bal", "balance")) {
            d.register(literal(name)
                    .executes(ctx -> showBalance(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
                    .then(argument("player", EntityArgumentType.player())
                            .executes(ctx -> showBalance(ctx.getSource(),
                                    EntityArgumentType.getPlayer(ctx, "player")))));
        }

        d.register(literal("pay")
                .then(argument("player", EntityArgumentType.player())
                        .then(argument("amount", DoubleArgumentType.doubleArg(0.01, 1_000_000_000.0))
                                .executes(ctx -> pay(ctx.getSource(),
                                        ctx.getSource().getPlayerOrThrow(),
                                        EntityArgumentType.getPlayer(ctx, "player"),
                                        DoubleArgumentType.getDouble(ctx, "amount"))))));

        d.register(literal("sell")
                .executes(ctx -> sell(ctx.getSource(), ctx.getSource().getPlayerOrThrow())));

        d.register(literal("sellall")
                .executes(ctx -> sellAll(ctx.getSource(), ctx.getSource().getPlayerOrThrow())));

        d.register(literal("worth")
                .executes(ctx -> worth(ctx.getSource(), ctx.getSource().getPlayerOrThrow())));

        d.register(literal("baltop")
                .executes(ctx -> baltop(ctx.getSource())));

        d.register(literal("eco")
                .requires(src -> src.hasPermissionLevel(2))
                .then(ecoBranch("give", EcoOp.GIVE))
                .then(ecoBranch("take", EcoOp.TAKE))
                .then(ecoBranch("set", EcoOp.SET)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> ecoBranch(String sub, EcoOp op) {
        return literal(sub)
                .then(argument("player", EntityArgumentType.player())
                        .then(argument("amount", DoubleArgumentType.doubleArg(0.0, 1_000_000_000.0))
                                .executes(ctx -> ecoChange(ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "player"),
                                        DoubleArgumentType.getDouble(ctx, "amount"),
                                        op))));
    }

    private static int showBalance(ServerCommandSource src, ServerPlayerEntity target) {
        PlayerData data = EconomyService.data(target);
        reply(src, "=== " + target.getName().getString() + " ===");
        reply(src, "Money: " + MoneyFormatter.format(data.getMoneyCents()));
        reply(src, "Shards: " + data.getShards());
        return 1;
    }

    private static int pay(ServerCommandSource src, ServerPlayerEntity from,
                           ServerPlayerEntity to, double amount) {
        if (from.getUuid().equals(to.getUuid())) {
            reply(src, "You cannot pay yourself.");
            return 0;
        }
        long cents = Math.round(amount * 100.0);
        EconomyService.Result r = EconomyService.pay(from, to, cents);
        reply(src, r.message());
        if (r.success()) {
            to.sendMessage(Text.literal("Received " + MoneyFormatter.format(cents)
                    + " from " + from.getName().getString() + "."), false);
        }
        return r.success() ? 1 : 0;
    }

    private static int sell(ServerCommandSource src, ServerPlayerEntity p) {
        EconomyService.Result r = EconomyService.sellHeld(p);
        reply(src, r.message());
        return r.success() ? 1 : 0;
    }

    private static int sellAll(ServerCommandSource src, ServerPlayerEntity p) {
        List<String> lines = EconomyService.sellAll(p);
        for (String line : lines) reply(src, line);
        return lines.size() > 1 ? 1 : 0;
    }

    private static int worth(ServerCommandSource src, ServerPlayerEntity p) {
        ItemStack held = p.getMainHandStack();
        if (held.isEmpty()) {
            reply(src, "You are not holding anything.");
            return 0;
        }
        PriceManager prices = DonutSMP.getPrices();
        String id = EconomyService.itemId(held);
        long sell = prices.getSellPriceCents(id);
        long buy = prices.getBuyPriceCents(id);

        reply(src, "=== Worth: " + held.getName().getString() + " ===");
        reply(src, sell < 0 ? "Sell: not sellable"
                : "Sell: " + MoneyFormatter.format(sell) + " each (" + MoneyFormatter.format(sell * held.getCount()) + " total)");
        reply(src, buy < 0 ? "Buy: not for sale" : "Buy: " + MoneyFormatter.format(buy) + " each");
        return 1;
    }

    private static int baltop(ServerCommandSource src) {
        List<PlayerData> top = DonutSMP.getPlayerData().getTopByMoney(10);
        reply(src, "=== Top Balances ===");
        if (top.isEmpty()) {
            reply(src, "No players yet.");
            return 0;
        }
        int rank = 1;
        for (PlayerData d : top) {
            reply(src, rank++ + ". " + d.getLastKnownName() + " - " + MoneyFormatter.format(d.getMoneyCents()));
        }
        return 1;
    }

    private static int ecoChange(ServerCommandSource src, ServerPlayerEntity target,
                                 double amount, EcoOp op) {
        long cents = Math.round(amount * 100.0);
        PlayerData data = EconomyService.data(target);

        switch (op) {
            case GIVE -> data.addMoneyCents(cents);
            case TAKE -> data.setMoneyCents(data.getMoneyCents() - cents);
            case SET -> data.setMoneyCents(cents);
        }
        EconomyService.save(target);

        reply(src, "Balance of " + target.getName().getString() + " is now "
                + MoneyFormatter.format(data.getMoneyCents()) + ".");
        return 1;
    }

    private static void reply(ServerCommandSource src, String text) {
        src.sendFeedback(() -> Text.literal(text), false);
    }
}
