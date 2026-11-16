package com.example.donutsmp.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * يحفظ كل لاعب في ملف JSON منفصل باسم UUID داخل مجلد الـ world.
 * يعمل مع UUID الأوفلاين والأونلاين لأن المفتاح هو UUID فقط.
 */
public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path directory;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(Path directory) {
        this.directory = directory;
    }

    public PlayerData getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            PlayerData loaded = readFromDisk(id);
            return loaded != null ? loaded : new PlayerData(id, "Unknown");
        });
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        writeToDisk(data);
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            writeToDisk(data);
        }
    }

    public void loadAll() {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create player data directory", e);
        }
        try (var stream = Files.list(directory)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        UUID uuid = UUID.fromString(name.substring(0, name.length() - 5));
                        PlayerData data = readFromDisk(uuid);
                        if (data != null) cache.put(uuid, data);
                    });
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Could not load player data", e);
        }
    }

    /** لأمر /baltop: يرجع أغنى اللاعبين مرتبين تنازليًا */
    public List<PlayerData> getTopByMoney(int limit) {
        return cache.values().stream()
                .sorted(Comparator.comparingLong(PlayerData::getMoneyCents).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Collection<PlayerData> all() {
        return cache.values();
    }

    private Path fileFor(UUID uuid) {
        return directory.resolve(uuid + ".json");
    }

    private PlayerData readFromDisk(UUID uuid) {
        Path file = fileFor(uuid);
        if (!Files.exists(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, PlayerData.class);
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("[DonutSMP] Failed to read " + file + ": " + e.getMessage());
            return null;
        }
    }

    private void writeToDisk(PlayerData data) {
        try {
            Files.createDirectories(directory);
            Path file = fileFor(data.getUuid());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            // الكتابة إلى ملف مؤقت ثم إعادة التسمية تمنع تلف الملف إذا أُغلقت اللعبة أثناء الحفظ
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[DonutSMP] Failed to save player " + data.getUuid() + ": " + e.getMessage());
        }
    }
}
