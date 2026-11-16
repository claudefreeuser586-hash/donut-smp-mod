package com.example.donutsmp.teleport;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles teleport warmup (player must stay still), cooldown, and /tpa requests.
 * All times are in server ticks (20 per second).
 */
public final class TeleportManager {
    public static final long WARMUP_TICKS = 60L;        // 3 seconds
    public static final long COOLDOWN_TICKS = 100L;     // 5 seconds
    public static final long REQUEST_TIMEOUT_TICKS = 1200L; // 60 seconds
    private static final double MAX_MOVE_SQ = 0.09;      // about 0.3 blocks

    public record Destination(ServerWorld world, double x, double y, double z, float yaw, float pitch) {}

    private record Pending(UUID player, Destination dest, long dueTick, double sx, double sy, double sz) {}
    private record TpaRequest(UUID requester, long expiresTick) {}

    private static final Map<UUID, Pending> pending = new HashMap<>();
    private static final Map<UUID, TpaRequest> requests = new HashMap<>(); // key: target player
    private static final Map<UUID, Long> lastTeleport = new HashMap<>();

    private TeleportManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(TeleportManager::tick);
    }

    public static void clear() {
        pending.clear();
        requests.clear();
        lastTeleport.clear();
    }

    /**
     * Starts a warmup teleport. Returns an error message, or null if it started.
     */
    public static String schedule(ServerPlayerEntity p, Destination dest, long now) {
        Long last = lastTeleport.get(p.getUuid());
        if (last != null && now - last < COOLDOWN_TICKS) {
            long secs = (COOLDOWN_TICKS - (now - last) + 19) / 20;
            return "Teleport on cooldown. Wait " + secs + "s.";
        }
        pending.put(p.getUuid(), new Pending(
                p.getUuid(), dest, now + WARMUP_TICKS, p.getX(), p.getY(), p.getZ()));
        p.sendMessage(Text.literal("Teleporting in 3 seconds. Don't move."), false);
        return null;
    }

    public static void putRequest(UUID target, UUID requester, long now) {
        requests.put(target, new TpaRequest(requester, now + REQUEST_TIMEOUT_TICKS));
    }

    /** Returns the requester's UUID if a valid request exists, and removes it. */
    public static UUID takeRequest(UUID target, long now) {
        TpaRequest req = requests.remove(target);
        if (req == null || req.expiresTick() < now) return null;
        return req.requester();
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTicks();

        Iterator<Map.Entry<UUID, Pending>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Pending t = it.next().getValue();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(t.player());
            if (p == null) {
                it.remove();
                continue;
            }

            double dx = p.getX() - t.sx();
            double dy = p.getY() - t.sy();
            double dz = p.getZ() - t.sz();
            if (dx * dx + dy * dy + dz * dz > MAX_MOVE_SQ) {
                it.remove();
                p.sendMessage(Text.literal("Teleport cancelled because you moved."), false);
                continue;
            }

            if (now >= t.dueTick()) {
                it.remove();
                lastTeleport.put(t.player(), now);
                Destination d = t.dest();
                p.teleport(d.world(), d.x(), d.y(), d.z(), Set.of(), d.yaw(), d.pitch(), false);
                p.sendMessage(Text.literal("Teleported!"), false);
            }
        }

        requests.values().removeIf(r -> r.expiresTick() < now);
    }
}
