package org.csgd.rankedSMP;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RankedSMP extends JavaPlugin {

    private RankManager rankManager;
    private RankListener rankListener;
    private InventoryManager inventoryManager;
    private RankDisplayManager rankDisplayManager;

    @Override
    public void onEnable() {
        rankManager = new RankManager(this);
        inventoryManager = new InventoryManager(this, rankManager);
        rankListener = new RankListener(this, rankManager);
        rankDisplayManager = new RankDisplayManager(this, rankManager);

        getServer().getPluginManager().registerEvents(rankListener, this);
        getServer().getPluginManager().registerEvents(inventoryManager, this);
        getServer().getPluginManager().registerEvents(rankDisplayManager, this);

        RankCommand rankCommand = new RankCommand(this, rankManager);
        getCommand("rank").setExecutor(rankCommand);
        getCommand("rank").setTabCompleter(rankCommand);
        getCommand("extrainv").setExecutor(rankCommand);

        for (Player player : getServer().getOnlinePlayers()) {
            rankListener.applyRankBonuses(player);
            rankDisplayManager.updatePlayerDisplay(player);
        }

        getLogger().info("RankedSMP has been enabled!");
        getLogger().info("Use /rank to manage player ranks (1-20)");
    }

    @Override
    public void onDisable() {
        if (rankManager != null) {
            rankManager.saveRanks();
        }
        if (inventoryManager != null) {
            inventoryManager.saveAllInventories();
        }
        getLogger().info("RankedSMP has been disabled!");
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public RankListener getRankListener() {
        return rankListener;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public RankDisplayManager getRankDisplayManager() {
        return rankDisplayManager;
    }
}

