package com.example.donutsmp.data;

import java.util.UUID;

/**
 * بيانات لاعب واحد. المال يُخزَّن بالسنتات (cents) لتجنب أخطاء الفاصلة العشرية.
 * مثال: 1500 = $15.00
 */
public class PlayerData {
    private final UUID uuid;
    private String lastKnownName;
    private long moneyCents;
    private long shards;
    private int kills;
    private int deaths;
    private long playtimeSeconds;

    public PlayerData(UUID uuid, String lastKnownName) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.moneyCents = 0L;
        this.shards = 0L;
    }

    public UUID getUuid() { return uuid; }

    public String getLastKnownName() { return lastKnownName; }
    public void setLastKnownName(String name) { this.lastKnownName = name; }

    public long getMoneyCents() { return moneyCents; }
    public void setMoneyCents(long cents) { this.moneyCents = Math.max(0, cents); }
    public void addMoneyCents(long cents) { setMoneyCents(this.moneyCents + cents); }

    public boolean removeMoneyCents(long cents) {
        if (cents < 0 || cents > moneyCents) return false;
        this.moneyCents -= cents;
        return true;
    }

    public long getShards() { return shards; }
    public void setShards(long shards) { this.shards = Math.max(0, shards); }
    public void addShards(long amount) { setShards(this.shards + amount); }

    public int getKills() { return kills; }
    public void addKill() { kills++; }

    public int getDeaths() { return deaths; }
    public void addDeath() { deaths++; }

    public double getKdr() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public long getPlaytimeSeconds() { return playtimeSeconds; }
    public void addPlaytimeSeconds(long seconds) { playtimeSeconds += seconds; }
}
