package org.csgd.rankedSMP;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankManager {

    private final RankedSMP plugin;
    private final Map<UUID, Integer> playerRanks;
    private File ranksFile;
    private FileConfiguration ranksConfig;

    public RankManager(RankedSMP plugin) {
        this.plugin = plugin;
        this.playerRanks = new HashMap<>();
        loadRanks();
    }

    private void loadRanks() {
        ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                ranksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create ranks.yml!");
            }
        }
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);

        if (ranksConfig.contains("ranks")) {
            for (String key : ranksConfig.getConfigurationSection("ranks").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                int rank = ranksConfig.getInt("ranks." + key);
                playerRanks.put(uuid, rank);
            }
        }
    }

    public void saveRanks() {
        for (Map.Entry<UUID, Integer> entry : playerRanks.entrySet()) {
            ranksConfig.set("ranks." + entry.getKey().toString(), entry.getValue());
        }
        try {
            ranksConfig.save(ranksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save ranks.yml!");
        }
    }

    public void setRank(UUID uuid, int rank) {
        if (rank < 0 || rank > 20) {
            throw new IllegalArgumentException("Rank must be between 0 and 20");
        }
        if (rank == 0) {
            playerRanks.remove(uuid);
            ranksConfig.set("ranks." + uuid.toString(), null);
        } else {
            playerRanks.put(uuid, rank);
        }
        saveRanks();
    }

    public int getRank(UUID uuid) {
        return playerRanks.getOrDefault(uuid, 0);
    }

    public boolean isRanked(UUID uuid) {
        return playerRanks.containsKey(uuid);
    }

    public Map<UUID, Integer> getAllRankedPlayers() {
        return new HashMap<>(playerRanks);
    }

    public double getExtraHearts(int rank) {
        if (rank == 0) return 0;
        return (21 - rank) * 0.5;
    }

    public double getEffectMultiplier(int rank) {
        if (rank == 0) return 1.0;
        return 1.05 + (20 - rank) * (0.95 / 19.0);
    }

    public int getExtraInventorySlots(int rank) {
        if (rank == 0 || rank > 10) return 0;
        if (rank <= 2) return 54;
        if (rank <= 4) return 45;
        if (rank <= 6) return 36;
        if (rank <= 8) return 27;
        return 18;
    }

    public double getXpMultiplier(int rank) {
        if (rank == 0) return 1.0;
        return 1.1 + (20 - rank) * (3.9 / 19.0);
    }

    public boolean isRankTaken(int rank) {
        return playerRanks.containsValue(rank);
    }

    public UUID getPlayerWithRank(int rank) {
        for (Map.Entry<UUID, Integer> entry : playerRanks.entrySet()) {
            if (entry.getValue() == rank) {
                return entry.getKey();
            }
        }
        return null;
    }
}

