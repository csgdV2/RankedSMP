package org.csgd.rankedSMP;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class RankDisplayManager implements Listener {

    private final RankedSMP plugin;
    private final RankManager rankManager;
    private Scoreboard scoreboard;

    public RankDisplayManager(RankedSMP plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        setupScoreboard();
    }

    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        for (int i = 1; i <= 20; i++) {
            String teamName = "rank" + String.format("%02d", i);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.prefix(getRankPrefix(i));
            team.color(NamedTextColor.WHITE);
        }

        Team unrankedTeam = scoreboard.getTeam("rank99");
        if (unrankedTeam == null) {
            unrankedTeam = scoreboard.registerNewTeam("rank99");
        }
        unrankedTeam.prefix(getUnrankedPrefix());
        unrankedTeam.color(NamedTextColor.WHITE);
    }

    private Component getRankPrefix(int rank) {
        return Component.text("[", NamedTextColor.YELLOW)
                .append(Component.text("#" + rank, NamedTextColor.YELLOW))
                .append(Component.text("] ", NamedTextColor.YELLOW));
    }

    private Component getUnrankedPrefix() {
        return Component.text("[", NamedTextColor.YELLOW)
                .append(Component.text("Unranked", NamedTextColor.DARK_GRAY))
                .append(Component.text("] ", NamedTextColor.YELLOW));
    }

    public void updatePlayerDisplay(Player player) {
        int rank = rankManager.getRank(player.getUniqueId());

        for (Team team : scoreboard.getTeams()) {
            team.removeEntry(player.getName());
        }

        String teamName;
        if (rank == 0) {
            teamName = "rank99";
        } else {
            teamName = "rank" + String.format("%02d", rank);
        }

        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.addEntry(player.getName());
        }

        if (rank == 0) {
            player.playerListName(Component.text("[", NamedTextColor.YELLOW)
                    .append(Component.text("Unranked", NamedTextColor.DARK_GRAY))
                    .append(Component.text("] ", NamedTextColor.YELLOW))
                    .append(Component.text(player.getName(), NamedTextColor.WHITE)));
        } else {
            player.playerListName(Component.text("[", NamedTextColor.YELLOW)
                    .append(Component.text("#" + rank, NamedTextColor.YELLOW))
                    .append(Component.text("] ", NamedTextColor.YELLOW))
                    .append(Component.text(player.getName(), NamedTextColor.WHITE)));
        }
    }

    public void updateAllPlayerDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayerDisplay(player), 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Team team : scoreboard.getTeams()) {
            team.removeEntry(player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        int rank = rankManager.getRank(player.getUniqueId());

        Component prefix;

        if (rank == 0) {
            prefix = Component.text("[", NamedTextColor.YELLOW)
                    .append(Component.text("Unranked", NamedTextColor.DARK_GRAY))
                    .append(Component.text("] ", NamedTextColor.YELLOW));
        } else {
            prefix = Component.text("[", NamedTextColor.YELLOW)
                    .append(Component.text("#" + rank, NamedTextColor.YELLOW))
                    .append(Component.text("] ", NamedTextColor.YELLOW));
        }

        event.renderer((source, sourceDisplayName, message, viewer) -> prefix
                .append(Component.text(source.getName(), NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(message.color(NamedTextColor.WHITE)));
    }
}

