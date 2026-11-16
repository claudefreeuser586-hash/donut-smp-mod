package com.example.donutsmp.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * يقرأ أسعار الأغراض من config/donutsmp/prices.json.
 * إذا لم يوجد الملف، يُنشَأ تلقائيًا بقائمة افتراضية شاملة.
 * المفاتيح: معرّف الغرض (مثل minecraft:diamond)، والقيمة: السعر بالسنتات (مثل 1000 = $10).
 */
public class PriceManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Long>>() {}.getType();

    private final Path file;
    private Map<String, Long> prices = new LinkedHashMap<>();

    public PriceManager(Path file) {
        this.file = file;
    }

    public void loadOrGenerate() {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                prices = defaultPrices();
                write();
                return;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Long> loaded = GSON.fromJson(reader, MAP_TYPE);
                prices = loaded != null ? loaded : defaultPrices();
            }
        } catch (IOException e) {
            System.err.println("[DonutSMP] Could not load prices.json, using defaults: " + e.getMessage());
            prices = defaultPrices();
        }
    }

    /** يرجع سعر البيع بالسنتات، أو -1 إذا كان الغرض غير قابل للبيع */
    public long getSellPriceCents(String itemId) {
        return prices.getOrDefault(itemId, -1L);
    }

    /** سعر الشراء يساوي ضعف سعر البيع تقريبًا، وهذا قابل للتعديل لاحقًا */
    public long getBuyPriceCents(String itemId) {
        long sell = getSellPriceCents(itemId);
        return sell < 0 ? -1L : sell * 2;
    }

    public boolean isSellable(String itemId) {
        return prices.containsKey(itemId);
    }

    public Map<String, Long> all() {
        return prices;
    }

    private void write() {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(prices, writer);
        } catch (IOException e) {
            System.err.println("[DonutSMP] Could not write default prices.json: " + e.getMessage());
        }
    }

    private static Map<String, Long> defaultPrices() {
        Map<String, Long> p = new LinkedHashMap<>();

        // الخامات والمعادن (القيمة بالسنتات)
        p.put("minecraft:coal", 200L);
        p.put("minecraft:raw_iron", 500L);
        p.put("minecraft:raw_copper", 150L);
        p.put("minecraft:raw_gold", 800L);
        p.put("minecraft:iron_ingot", 600L);
        p.put("minecraft:copper_ingot", 200L);
        p.put("minecraft:gold_ingot", 1000L);
        p.put("minecraft:diamond", 5000L);
        p.put("minecraft:emerald", 4000L);
        p.put("minecraft:lapis_lazuli", 300L);
        p.put("minecraft:redstone", 250L);
        p.put("minecraft:quartz", 400L);
        p.put("minecraft:netherite_scrap", 20000L);
        p.put("minecraft:netherite_ingot", 80000L);
        p.put("minecraft:amethyst_shard", 350L);
        p.put("minecraft:stick", 20L);

        // الكتل
        p.put("minecraft:stone", 10L);
        p.put("minecraft:cobblestone", 8L);
        p.put("minecraft:dirt", 5L);
        p.put("minecraft:grass_block", 5L);
        p.put("minecraft:sand", 8L);
        p.put("minecraft:gravel", 8L);
        p.put("minecraft:oak_log", 40L);
        p.put("minecraft:spruce_log", 40L);
        p.put("minecraft:birch_log", 40L);
        p.put("minecraft:jungle_log", 40L);
        p.put("minecraft:acacia_log", 40L);
        p.put("minecraft:dark_oak_log", 40L);
        p.put("minecraft:cherry_log", 40L);
        p.put("minecraft:mangrove_log", 40L);
        p.put("minecraft:oak_planks", 10L);
        p.put("minecraft:glass", 30L);
        p.put("minecraft:bricks", 50L);
        p.put("minecraft:obsidian", 2500L);
        p.put("minecraft:deepslate", 15L);
        p.put("minecraft:cobbled_deepslate", 12L);
        p.put("minecraft:tuff", 15L);
        p.put("minecraft:calcite", 15L);
        p.put("minecraft:netherrack", 6L);
        p.put("minecraft:end_stone", 12L);
        p.put("minecraft:purpur_block", 60L);
        p.put("minecraft:prismarine", 80L);
        p.put("minecraft:sea_lantern", 150L);
        p.put("minecraft:glowstone", 120L);
        p.put("minecraft:moss_block", 25L);
        p.put("minecraft:sponge", 200L);
        p.put("minecraft:wool_white", 40L);
        p.put("minecraft:terracotta", 30L);
        p.put("minecraft:concrete_white", 35L);
        p.put("minecraft:copper_block", 1800L);
        p.put("minecraft:iron_block", 5400L);
        p.put("minecraft:gold_block", 9000L);
        p.put("minecraft:diamond_block", 45000L);
        p.put("minecraft:emerald_block", 36000L);
        p.put("minecraft:netherite_block", 720000L);

        // الطعام
        p.put("minecraft:apple", 50L);
        p.put("minecraft:golden_apple", 2000L);
        p.put("minecraft:enchanted_golden_apple", 500000L);
        p.put("minecraft:bread", 40L);
        p.put("minecraft:cooked_beef", 120L);
        p.put("minecraft:cooked_porkchop", 120L);
        p.put("minecraft:cooked_chicken", 100L);
        p.put("minecraft:cooked_mutton", 100L);
        p.put("minecraft:cooked_cod", 80L);
        p.put("minecraft:cooked_salmon", 90L);
        p.put("minecraft:baked_potato", 60L);
        p.put("minecraft:carrot", 30L);
        p.put("minecraft:golden_carrot", 800L);
        p.put("minecraft:melon_slice", 15L);
        p.put("minecraft:cookie", 20L);
        p.put("minecraft:cake", 500L);
        p.put("minecraft:pumpkin_pie", 150L);

        // الأدوات والسلاح
        p.put("minecraft:wooden_sword", 30L);
        p.put("minecraft:stone_sword", 80L);
        p.put("minecraft:iron_sword", 800L);
        p.put("minecraft:golden_sword", 1200L);
        p.put("minecraft:diamond_sword", 6000L);
        p.put("minecraft:netherite_sword", 90000L);
        p.put("minecraft:iron_pickaxe", 900L);
        p.put("minecraft:diamond_pickaxe", 6500L);
        p.put("minecraft:netherite_pickaxe", 95000L);
        p.put("minecraft:iron_axe", 800L);
        p.put("minecraft:diamond_axe", 6000L);
        p.put("minecraft:netherite_axe", 92000L);
        p.put("minecraft:iron_shovel", 200L);
        p.put("minecraft:diamond_shovel", 1500L);
        p.put("minecraft:bow", 500L);
        p.put("minecraft:arrow", 15L);
        p.put("minecraft:shield", 600L);
        p.put("minecraft:totem_of_undying", 150000L);
        p.put("minecraft:ender_pearl", 2500L);
        p.put("minecraft:blaze_rod", 1200L);
        p.put("minecraft:ender_eye", 3000L);

        // الدروع
        p.put("minecraft:iron_helmet", 1200L);
        p.put("minecraft:iron_chestplate", 2000L);
        p.put("minecraft:iron_leggings", 1800L);
        p.put("minecraft:iron_boots", 1000L);
        p.put("minecraft:diamond_helmet", 8000L);
        p.put("minecraft:diamond_chestplate", 12000L);
        p.put("minecraft:diamond_leggings", 10000L);
        p.put("minecraft:diamond_boots", 7000L);
        p.put("minecraft:netherite_helmet", 60000L);
        p.put("minecraft:netherite_chestplate", 90000L);
        p.put("minecraft:netherite_leggings", 80000L);
        p.put("minecraft:netherite_boots", 50000L);

        // أغراض الوحوش
        p.put("minecraft:rotten_flesh", 10L);
        p.put("minecraft:bone", 15L);
        p.put("minecraft:string", 20L);
        p.put("minecraft:spider_eye", 25L);
        p.put("minecraft:gunpowder", 40L);
        p.put("minecraft:feather", 10L);
        p.put("minecraft:leather", 30L);
        p.put("minecraft:ghast_tear", 2000L);
        p.put("minecraft:slime_ball", 60L);
        p.put("minecraft:blaze_powder", 300L);
        p.put("minecraft:phantom_membrane", 500L);
        p.put("minecraft:shulker_shell", 8000L);
        p.put("minecraft:nether_star", 150000L);
        p.put("minecraft:dragon_breath", 4000L);

        // الريدستون
        p.put("minecraft:redstone_block", 2250L);
        p.put("minecraft:repeater", 600L);
        p.put("minecraft:comparator", 900L);
        p.put("minecraft:observer", 1100L);
        p.put("minecraft:piston", 900L);
        p.put("minecraft:sticky_piston", 1200L);
        p.put("minecraft:hopper", 1500L);
        p.put("minecraft:dispenser", 800L);
        p.put("minecraft:dropper", 600L);
        p.put("minecraft:lever", 80L);
        p.put("minecraft:redstone_torch", 260L);
        p.put("minecraft:tnt", 400L);
        p.put("minecraft:target", 500L);
        p.put("minecraft:daylight_detector", 1500L);

        return p;
    }
}
