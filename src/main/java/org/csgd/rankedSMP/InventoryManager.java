package org.csgd.rankedSMP;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager implements Listener {

    private final RankedSMP plugin;
    private final RankManager rankManager;
    private final Map<UUID, Inventory> extraInventories;
    private File inventoryFile;
    private FileConfiguration inventoryConfig;

    public InventoryManager(RankedSMP plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.extraInventories = new HashMap<>();
        loadInventoryData();
    }

    private void loadInventoryData() {
        inventoryFile = new File(plugin.getDataFolder(), "inventories.yml");
        if (!inventoryFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                inventoryFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create inventories.yml!");
            }
        }
        inventoryConfig = YamlConfiguration.loadConfiguration(inventoryFile);
    }

    public void setupPlayerInventory(Player player) {
        UUID uuid = player.getUniqueId();
        int rank = rankManager.getRank(uuid);
        int extraSlots = rankManager.getExtraInventorySlots(rank);

        if (extraSlots <= 0) {
            extraInventories.remove(uuid);
            return;
        }

        int rows = (int) Math.ceil(extraSlots / 9.0);
        int inventorySize = rows * 9;

        Inventory extraInv = Bukkit.createInventory(null, inventorySize, "§6Extra Inventory §7(Rank " + rank + ")");

        loadPlayerExtraInventory(uuid, extraInv);

        extraInventories.put(uuid, extraInv);
    }

    private void loadPlayerExtraInventory(UUID uuid, Inventory inventory) {
        String path = "inventory." + uuid.toString();
        if (!inventoryConfig.contains(path)) return;

        for (int i = 0; i < inventory.getSize(); i++) {
            String itemPath = path + "." + i;
            if (inventoryConfig.contains(itemPath)) {
                ItemStack item = inventoryConfig.getItemStack(itemPath);
                if (item != null && i < inventory.getSize()) {
                    inventory.setItem(i, item);
                }
            }
        }
    }

    public void savePlayerExtraInventory(UUID uuid) {
        Inventory extraInv = extraInventories.get(uuid);
        if (extraInv == null) return;

        String path = "inventory." + uuid.toString();

        inventoryConfig.set(path, null);

        for (int i = 0; i < extraInv.getSize(); i++) {
            ItemStack item = extraInv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                inventoryConfig.set(path + "." + i, item);
            }
        }

        try {
            inventoryConfig.save(inventoryFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save inventory for " + uuid);
        }
    }

    public void saveAllInventories() {
        for (UUID uuid : extraInventories.keySet()) {
            savePlayerExtraInventory(uuid);
        }
    }

    public void openExtraInventory(Player player) {
        UUID uuid = player.getUniqueId();

        int rank = rankManager.getRank(uuid);
        int extraSlots = rankManager.getExtraInventorySlots(rank);

        if (extraSlots <= 0) {
            player.sendMessage("§cYou don't have access to extra inventory slots!");
            player.sendMessage("§7Extra inventory is available for Rank 10 and higher.");
            return;
        }

        Inventory extraInv = extraInventories.get(uuid);
        if (extraInv == null) {
            setupPlayerInventory(player);
            extraInv = extraInventories.get(uuid);
        }

        if (extraInv != null) {
            player.openInventory(extraInv);
        }
    }

    public boolean isExtraInventory(Inventory inventory) {
        return extraInventories.containsValue(inventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (isExtraInventory(event.getInventory())) {
            savePlayerExtraInventory(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        savePlayerExtraInventory(uuid);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        Player killer = player.getKiller();
        if (killer != null && killer != player) {
            handleRankSwap(killer, player);
        }

        Inventory extraInv = extraInventories.get(uuid);
        if (extraInv == null) return;

        for (int i = 0; i < extraInv.getSize(); i++) {
            ItemStack item = extraInv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                event.getDrops().add(item);
                extraInv.setItem(i, null);
            }
        }

        savePlayerExtraInventory(uuid);
    }

    private void handleRankSwap(Player killer, Player victim) {
        int killerRank = rankManager.getRank(killer.getUniqueId());
        int victimRank = rankManager.getRank(victim.getUniqueId());

        if (killerRank == 0 && victimRank == 0) {
            return;
        }

        if (killerRank > 0 && victimRank == 0) {
            return;
        }

        if (killerRank == 0 && victimRank > 0) {
            rankManager.setRank(killer.getUniqueId(), victimRank);
            rankManager.setRank(victim.getUniqueId(), 0);

            killer.sendMessage("§7You are now §fRank " + victimRank);
            victim.sendMessage("§7You lost your rank to " + killer.getName());

            plugin.getRankListener().refreshPlayerBonuses(killer);
            plugin.getRankListener().refreshPlayerBonuses(victim);

            plugin.getRankDisplayManager().updatePlayerDisplay(killer);
            plugin.getRankDisplayManager().updatePlayerDisplay(victim);

            onRankChange(killer, 0, victimRank);
            onRankChange(victim, victimRank, 0);
            return;
        }

        if (killerRank > victimRank) {
            int oldKillerRank = killerRank;
            int oldVictimRank = victimRank;

            rankManager.setRank(killer.getUniqueId(), oldVictimRank);
            rankManager.setRank(victim.getUniqueId(), oldKillerRank);

            killer.sendMessage("§7You are now §fRank " + oldVictimRank);
            victim.sendMessage("§7You are now §fRank " + oldKillerRank);

            plugin.getRankListener().refreshPlayerBonuses(killer);
            plugin.getRankListener().refreshPlayerBonuses(victim);

            plugin.getRankDisplayManager().updatePlayerDisplay(killer);
            plugin.getRankDisplayManager().updatePlayerDisplay(victim);

            onRankChange(killer, oldKillerRank, oldVictimRank);
            onRankChange(victim, oldVictimRank, oldKillerRank);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    public void onRankChange(Player player, int oldRank, int newRank) {
        UUID uuid = player.getUniqueId();
        int oldSlots = rankManager.getExtraInventorySlots(oldRank);
        int newSlots = rankManager.getExtraInventorySlots(newRank);

        if (oldSlots == newSlots) return;

        Inventory oldInv = extraInventories.get(uuid);

        if (newSlots <= 0) {
            if (oldInv != null) {
                for (ItemStack item : oldInv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                extraInventories.remove(uuid);
                inventoryConfig.set("inventory." + uuid, null);
                try {
                    inventoryConfig.save(inventoryFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save inventory changes");
                }
            }
            return;
        }

        int rows = (int) Math.ceil(newSlots / 9.0);
        int inventorySize = rows * 9;
        Inventory newInv = Bukkit.createInventory(null, inventorySize, "§6Extra Inventory §7(Rank " + newRank + ")");

        if (oldInv != null) {
            for (int i = 0; i < Math.min(oldInv.getSize(), newInv.getSize()); i++) {
                ItemStack item = oldInv.getItem(i);
                if (item != null) {
                    newInv.setItem(i, item);
                }
            }

            if (newSlots < oldSlots) {
                for (int i = newInv.getSize(); i < oldInv.getSize(); i++) {
                    ItemStack item = oldInv.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                player.sendMessage("§eSome items from your extra inventory were dropped due to size reduction.");
            }
        } else {
            loadPlayerExtraInventory(uuid, newInv);
        }

        extraInventories.put(uuid, newInv);
        savePlayerExtraInventory(uuid);

    }
}

