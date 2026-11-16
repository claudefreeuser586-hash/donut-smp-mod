package com.example.donutsmp.gui;

import java.util.List;

public enum ShopCategory {
    BLOCKS("Blocks", "minecraft:bricks", List.of(
            "minecraft:stone", "minecraft:cobblestone", "minecraft:dirt", "minecraft:oak_log",
            "minecraft:oak_planks", "minecraft:glass", "minecraft:bricks", "minecraft:obsidian",
            "minecraft:deepslate", "minecraft:netherrack", "minecraft:end_stone", "minecraft:purpur_block",
            "minecraft:prismarine", "minecraft:sea_lantern", "minecraft:glowstone", "minecraft:moss_block",
            "minecraft:sponge", "minecraft:terracotta", "minecraft:copper_block", "minecraft:iron_block",
            "minecraft:gold_block", "minecraft:diamond_block", "minecraft:emerald_block",
            "minecraft:netherite_block")),

    ORES("Ores & Ingots", "minecraft:diamond", List.of(
            "minecraft:coal", "minecraft:raw_iron", "minecraft:raw_copper", "minecraft:raw_gold",
            "minecraft:iron_ingot", "minecraft:copper_ingot", "minecraft:gold_ingot", "minecraft:diamond",
            "minecraft:emerald", "minecraft:lapis_lazuli", "minecraft:redstone", "minecraft:quartz",
            "minecraft:netherite_scrap", "minecraft:netherite_ingot", "minecraft:amethyst_shard")),

    GEAR("Gear", "minecraft:diamond_sword", List.of(
            "minecraft:iron_sword", "minecraft:diamond_sword", "minecraft:netherite_sword",
            "minecraft:iron_pickaxe", "minecraft:diamond_pickaxe", "minecraft:netherite_pickaxe",
            "minecraft:iron_axe", "minecraft:diamond_axe", "minecraft:netherite_axe",
            "minecraft:iron_shovel", "minecraft:diamond_shovel", "minecraft:bow", "minecraft:arrow",
            "minecraft:shield", "minecraft:totem_of_undying", "minecraft:ender_pearl",
            "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings",
            "minecraft:iron_boots", "minecraft:diamond_helmet", "minecraft:diamond_chestplate",
            "minecraft:diamond_leggings", "minecraft:diamond_boots", "minecraft:netherite_helmet",
            "minecraft:netherite_chestplate", "minecraft:netherite_leggings", "minecraft:netherite_boots")),

    FOOD("Food", "minecraft:cooked_beef", List.of(
            "minecraft:apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple",
            "minecraft:bread", "minecraft:cooked_beef", "minecraft:cooked_porkchop",
            "minecraft:cooked_chicken", "minecraft:cooked_mutton", "minecraft:cooked_cod",
            "minecraft:cooked_salmon", "minecraft:baked_potato", "minecraft:carrot",
            "minecraft:golden_carrot", "minecraft:melon_slice", "minecraft:cookie",
            "minecraft:cake", "minecraft:pumpkin_pie")),

    MISC("Spawners & Misc", "minecraft:redstone", List.of(
            "minecraft:spawner", "minecraft:redstone_block", "minecraft:repeater", "minecraft:comparator",
            "minecraft:observer", "minecraft:piston", "minecraft:sticky_piston", "minecraft:hopper",
            "minecraft:dispenser", "minecraft:dropper", "minecraft:lever", "minecraft:redstone_torch",
            "minecraft:tnt", "minecraft:target", "minecraft:daylight_detector", "minecraft:string",
            "minecraft:bone", "minecraft:gunpowder", "minecraft:feather", "minecraft:leather",
            "minecraft:ghast_tear", "minecraft:slime_ball", "minecraft:blaze_rod",
            "minecraft:blaze_powder", "minecraft:ender_eye", "minecraft:dragon_breath"));

    public final String title;
    public final String iconId;
    public final List<String> itemIds;

    ShopCategory(String title, String iconId, List<String> itemIds) {
        this.title = title;
        this.iconId = iconId;
        this.itemIds = itemIds;
    }
}
