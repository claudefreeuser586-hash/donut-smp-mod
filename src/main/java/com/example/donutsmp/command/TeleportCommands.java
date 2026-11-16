package com.example.donutsmp.command;

import com.example.donutsmp.DonutSMP;
import com.example.donutsmp.data.Home;
import com.example.donutsmp.economy.EconomyService;
import com.example.donutsmp.teleport.TeleportManager;
import com.example.donutsmp.teleport.TeleportManager.Destination;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class TeleportCommands {
    private static final int MAX_HOMES = 5;

    private TeleportCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("sethome")
                .executes(ctx -> setHome(ctx.getSource(), ctx.getSource().getPlayerOrThrow(), "home"))
                .then(argument("name", StringArgumentType.word())
                        .executes(ctx -> setHome(ctx.getSource(), ctx.getSource().getPlayerOrThrow(),
                                StringArgumentType.getString(ctx, "name")))));

        d.register(literal("home")
                .executes(ctx -> home(ctx.getSource(), ctx.getSource().getPlayerOrThrow(), "home"))
                .then(argument("name", StringArgumentType.word())
                        .executes(ctx -> home(ctx.getSource(), ctx.getSource().getPlayerOrThrow(),
                                StringArgumentType.getString(ctx, "name")))));

        d.register(literal("delhome")
                .executes(ctx -> delHome(ctx.getSource(), ctx.getSource().getPlayerOrThrow(), "home"))
                .then(argument("name", StringArgumentType.word())
                        .executes(ctx -> delHome(ctx.getSource(), ctx.getSource().getPlayerOrThrow(),
                                StringArgumentType.getString(ctx, "name")))));

        d.register(literal("tpa")
                .then(argument("player", EntityArgumentType.player())
                        .executes(ctx -> tpa(ctx.getSource(), ctx.getSource().getPlayerOrThrow(),
                                EntityArgumentType.getPlayer(ctx, "player")))));

        d.register(literal("tpaccept")
                .executes(ctx -> tpAccept(ctx.getSource(), ctx.getSource().getPlayerOrThrow())));

        d.register(literal("spawn")
                .executes(ctx -> spawn(ctx.getSource(), ctx.getSource().getPlayerOrThrow())));
    }

    private static int setHome(ServerCommandSource src, ServerPlayerEntity p, String name) {
        Map<String, Home> homes = EconomyService.data(p).getHomes();
        if (!homes.containsKey(name) && homes.size() >= MAX_HOMES) {
            return fail(src, "You reached the limit of " + MAX_HOMES + " homes. Use /delhome first.");
        }
        String dim = p.getServerWorld().getRegistryKey().getValue().toString();
        homes.put(name, new Home(dim, p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch()));
        EconomyService.save(p);
        return ok(src, "Home '" + name + "' set.");
    }

    private static int home(ServerCommandSource src, ServerPlayerEntity p, String name) {
        Home h = EconomyService.data(p).getHomes().get(name);
        if (h == null) return fail(src, "No home named '" + name + "'. Use /sethome " + name + ".");

        MinecraftServer server = src.getServer();
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(h.getDimension()));
        ServerWorld world = server.getWorld(key);
        if (world == null) return fail(src, "The world of this home no longer exists.");

        String err = TeleportManager.schedule(p,
                new Destination(world, h.getX(), h.getY(), h.getZ(), h.getYaw(), h.getPitch()),
                server.getTicks());
        return err != null ? fail(src, err) : ok(src, "Going to home '" + name + "'.");
    }

    private static int delHome(ServerCommandSource src, ServerPlayerEntity p, String name) {
        if (EconomyService.data(p).getHomes().remove(name) == null) {
            return fail(src, "No home named '" + name + "'.");
        }
        EconomyService.save(p);
        return ok(src, "Home '" + name + "' deleted.");
    }

    private static int tpa(ServerCommandSource src, ServerPlayerEntity requester, ServerPlayerEntity target) {
        if (requester.getUuid().equals(target.getUuid())) {
            return fail(src, "You cannot send a request to yourself.");
        }
        TeleportManager.putRequest(target.getUuid(), requester.getUuid(), src.getServer().getTicks());
        target.sendMessage(Text.literal(requester.getName().getString()
                + " wants to teleport to you. Type /tpaccept within 60 seconds."), false);
        return ok(src, "Teleport request sent to " + target.getName().getString() + ".");
    }

    private static int tpAccept(ServerCommandSource src, ServerPlayerEntity target) {
        MinecraftServer server = src.getServer();
        UUID requesterId = TeleportManager.takeRequest(target.getUuid(), server.getTicks());
        if (requesterId == null) return fail(src, "You have no pending teleport request.");

        ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterId);
        if (requester == null) return fail(src, "That player is no longer online.");

        Destination dest = new Destination(target.getServerWorld(),
                target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
        String err = TeleportManager.schedule(requester, dest, server.getTicks());
        if (err != null) {
            requester.sendMessage(Text.literal(err), false);
            return fail(src, err);
        }
        return ok(src, "Accepted. " + requester.getName().getString() + " is teleporting to you.");
    }

    private static int spawn(ServerCommandSource src, ServerPlayerEntity p) {
        MinecraftServer server = src.getServer();
        ServerWorld overworld = server.getOverworld();
        var spawnPos = overworld.getSpawnPos();

        Destination dest = new Destination(overworld,
                spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0f, 0f);
        String err = TeleportManager.schedule(p, dest, server.getTicks());
        return err != null ? fail(src, err) : ok(src, "Going to spawn...");
    }

    private static int ok(ServerCommandSource src, String text) {
        src.sendFeedback(() -> Text.literal(text), false);
        return 1;
    }

    private static int fail(ServerCommandSource src, String text) {
        src.sendFeedback(() -> Text.literal(text), false);
        return 0;
    }
}
