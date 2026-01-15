package org.csgd.rankedSMP;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final RankedSMP plugin;
    private final RankManager rankManager;

    public RankCommand(RankedSMP plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rank")) {
            return handleRankCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("extrainv")) {
            return handleExtraInvCommand(sender);
        }
        return false;
    }

    private boolean handleRankCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage: /rank <set|remove|info|list> ...");
                return true;
            }
            showPlayerRank(player, player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set" -> {
                if (!sender.hasPermission("rankedsmp.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /rank set <player> <rank 1-20>");
                    return true;
                }
                handleSetRank(sender, args[1], args[2]);
            }
            case "remove" -> {
                if (!sender.hasPermission("rankedsmp.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /rank remove <player>");
                    return true;
                }
                handleRemoveRank(sender, args[1]);
            }
            case "info" -> {
                if (args.length < 2) {
                    if (sender instanceof Player player) {
                        showPlayerRank(player, player);
                    } else {
                        sender.sendMessage("§cUsage: /rank info <player>");
                    }
                } else {
                    handleRankInfo(sender, args[1]);
                }
            }
            case "list" -> handleRankList(sender);
            case "perks" -> handlePerks(sender, args);
            case "startsmp" -> {
                if (!sender.hasPermission("rankedsmp.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                handleStartSmp(sender);
            }
            default -> sender.sendMessage("§cUnknown subcommand. Use: set, remove, info, list, perks, startsmp");
        }

        return true;
    }

    private void handleSetRank(CommandSender sender, String playerName, String rankStr) {
        int rank;

        if (rankStr.equalsIgnoreCase("unranked") || rankStr.equals("0")) {
            Player onlinePlayer = Bukkit.getPlayer(playerName);
            OfflinePlayer offlinePlayer = onlinePlayer != null ? onlinePlayer : Bukkit.getOfflinePlayer(playerName);

            if (!offlinePlayer.hasPlayedBefore() && onlinePlayer == null) {
                sender.sendMessage("§cPlayer not found!");
                return;
            }

            UUID targetUUID = offlinePlayer.getUniqueId();
            int oldRank = rankManager.getRank(targetUUID);

            if (oldRank == 0) {
                sender.sendMessage("§c" + offlinePlayer.getName() + " is already unranked");
                return;
            }

            rankManager.setRank(targetUUID, 0);

            if (onlinePlayer != null) {
                plugin.getRankListener().refreshPlayerBonuses(onlinePlayer);
                plugin.getInventoryManager().onRankChange(onlinePlayer, oldRank, 0);
                plugin.getRankDisplayManager().updatePlayerDisplay(onlinePlayer);
                onlinePlayer.sendMessage("§7Your rank has been set to §e[§8Unranked§e] §7by an admin");
            }

            sender.sendMessage("§aSet §f" + offlinePlayer.getName() + "§a to §fUnranked");
            return;
        }

        try {
            rank = Integer.parseInt(rankStr);
            if (rank < 1 || rank > 20) {
                sender.sendMessage("§cRank must be between 1 and 20!");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid rank number!");
            return;
        }

        Player onlinePlayer = Bukkit.getPlayer(playerName);
        OfflinePlayer offlinePlayer = onlinePlayer != null ? onlinePlayer : Bukkit.getOfflinePlayer(playerName);

        if (!offlinePlayer.hasPlayedBefore() && onlinePlayer == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        UUID targetUUID = offlinePlayer.getUniqueId();
        int oldRank = rankManager.getRank(targetUUID);

        if (rankManager.isRankTaken(rank)) {
            UUID currentHolder = rankManager.getPlayerWithRank(rank);
            if (!currentHolder.equals(targetUUID)) {
                OfflinePlayer holder = Bukkit.getOfflinePlayer(currentHolder);
                sender.sendMessage("§cRank §f" + rank + " §cis already held by §f" + holder.getName() + "§c!");
                sender.sendMessage("§7Use §f/rank remove " + holder.getName() + " §7first.");
                return;
            }
        }

        rankManager.setRank(targetUUID, rank);

        if (onlinePlayer != null) {
            plugin.getRankListener().refreshPlayerBonuses(onlinePlayer);
            plugin.getInventoryManager().onRankChange(onlinePlayer, oldRank, rank);
            plugin.getRankDisplayManager().updatePlayerDisplay(onlinePlayer);
            onlinePlayer.sendMessage("§7Your rank has been set to §e[#" + rank + "] §7by an admin");
        }

        sender.sendMessage("§aSet §f" + offlinePlayer.getName() + "§a's rank to §e[#" + rank + "]");
    }

    private void handleRemoveRank(CommandSender sender, String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        OfflinePlayer offlinePlayer = onlinePlayer != null ? onlinePlayer : Bukkit.getOfflinePlayer(playerName);

        if (!offlinePlayer.hasPlayedBefore() && onlinePlayer == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        UUID targetUUID = offlinePlayer.getUniqueId();
        int oldRank = rankManager.getRank(targetUUID);

        if (oldRank == 0) {
            sender.sendMessage("§c" + offlinePlayer.getName() + " is not ranked");
            return;
        }

        rankManager.setRank(targetUUID, 0);

        if (onlinePlayer != null) {
            plugin.getRankListener().refreshPlayerBonuses(onlinePlayer);
            plugin.getInventoryManager().onRankChange(onlinePlayer, oldRank, 0);
            plugin.getRankDisplayManager().updatePlayerDisplay(onlinePlayer);
            onlinePlayer.sendMessage("§7Your rank has been set to §e[§8Unranked§e] §7by an admin");
        }

        sender.sendMessage("§aRemoved §f" + offlinePlayer.getName() + "§a's rank §7(was §e[#" + oldRank + "]§7)");
    }

    private void handleRankInfo(CommandSender sender, String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        OfflinePlayer offlinePlayer = onlinePlayer != null ? onlinePlayer : Bukkit.getOfflinePlayer(playerName);

        if (!offlinePlayer.hasPlayedBefore() && onlinePlayer == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        showPlayerRank(sender, offlinePlayer);
    }

    private void showPlayerRank(CommandSender sender, OfflinePlayer target) {
        int rank = rankManager.getRank(target.getUniqueId());

        sender.sendMessage("§e" + target.getName() + "'s Rank Info");

        if (rank == 0) {
            sender.sendMessage("§7Status: §fUnranked");
            sender.sendMessage("§7No special perks");
        } else {
            sender.sendMessage("§7Status: §fRank " + rank);
            showRankPerks(sender, rank);
        }
    }

    private void showRankPerks(CommandSender sender, int rank) {
        double extraHearts = rankManager.getExtraHearts(rank);
        double effectMultiplier = rankManager.getEffectMultiplier(rank);
        double xpMultiplier = rankManager.getXpMultiplier(rank);
        int extraSlots = rankManager.getExtraInventorySlots(rank);

        sender.sendMessage("§7Extra Hearts: §f+" + String.format("%.1f", extraHearts));
        sender.sendMessage("§7Effect Duration: §f" + String.format("%.0f", (effectMultiplier * 100)) + "%");
        sender.sendMessage("§7XP Multiplier: §f" + String.format("%.1f", xpMultiplier) + "x");

        if (extraSlots > 0) {
            sender.sendMessage("§7Extra Inventory: §f" + extraSlots + " slots");
        } else {
            sender.sendMessage("§7Extra Inventory: §fNone");
        }
    }

    private void handleRankList(CommandSender sender) {
        sender.sendMessage("§eRanked Players");

        Map<UUID, Integer> allRanks = rankManager.getAllRankedPlayers();

        if (allRanks.isEmpty()) {
            sender.sendMessage("§7No players are currently ranked.");
            return;
        }

        List<Map.Entry<UUID, Integer>> sortedRanks = new ArrayList<>(allRanks.entrySet());
        sortedRanks.sort(Map.Entry.comparingByValue());

        for (Map.Entry<UUID, Integer> entry : sortedRanks) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String status = player.isOnline() ? "§a●" : "§c●";
            sender.sendMessage(status + " §e[§e#" + entry.getValue() + "§e] §f" + player.getName());
        }

        sender.sendMessage("§7Total: §f" + allRanks.size() + "/20");
    }

    private void handlePerks(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§eRank Perks Overview");
            sender.sendMessage("§7Use /rank perks <1-20> for specific rank info");
            sender.sendMessage("§7Hearts: §fRank 20 +0.5 to Rank 1 +10");
            sender.sendMessage("§7Effects: §fRank 20 1.05x to Rank 1 2.0x");
            sender.sendMessage("§7XP: §fRank 20 1.1x to Rank 1 5.0x");
            sender.sendMessage("§7Inventory: §fRank 10+ only (18-54 slots)");
            return;
        }

        try {
            int rank = Integer.parseInt(args[1]);
            if (rank < 1 || rank > 20) {
                sender.sendMessage("§cRank must be between 1 and 20");
                return;
            }
            sender.sendMessage("§eRank " + rank + " Perks");
            showRankPerks(sender, rank);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid rank number");
        }
    }

    private void handleStartSmp(CommandSender sender) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (onlinePlayers.isEmpty()) {
            sender.sendMessage("§cNo players online to assign ranks!");
            return;
        }

        if (onlinePlayers.size() > 20) {
            sender.sendMessage("§cToo many players online! Maximum 20 players can be ranked.");
            return;
        }

        for (UUID uuid : rankManager.getAllRankedPlayers().keySet()) {
            rankManager.setRank(uuid, 0);
        }

        Collections.shuffle(onlinePlayers);

        List<Integer> ranks = new ArrayList<>();
        for (int i = 1; i <= onlinePlayers.size(); i++) {
            ranks.add(i);
        }
        Collections.shuffle(ranks);

        for (int i = 0; i < onlinePlayers.size(); i++) {
            Player player = onlinePlayers.get(i);
            int rank = ranks.get(i);

            rankManager.setRank(player.getUniqueId(), rank);
            plugin.getRankListener().refreshPlayerBonuses(player);
            plugin.getInventoryManager().onRankChange(player, 0, rank);
            plugin.getRankDisplayManager().updatePlayerDisplay(player);

            player.sendMessage("§7You have been assigned §e[§e#" + rank + "§e]");
        }

        Bukkit.broadcastMessage("§eRanked SMP has begun! §7Ranks have been assigned.");

        sender.sendMessage("§aAssigned random ranks to §f" + onlinePlayers.size() + " §aplayers");
    }

    private boolean handleExtraInvCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        plugin.getInventoryManager().openExtraInventory(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("rank")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("info", "list", "perks"));
                if (sender.hasPermission("rankedsmp.admin")) {
                    completions.addAll(Arrays.asList("set", "remove", "startsmp"));
                }
            } else if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("set") || sub.equals("remove") || sub.equals("info")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                } else if (sub.equals("perks")) {
                    for (int i = 1; i <= 20; i++) {
                        completions.add(String.valueOf(i));
                    }
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                completions.add("unranked");
                for (int i = 1; i <= 20; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .toList();
    }
}

