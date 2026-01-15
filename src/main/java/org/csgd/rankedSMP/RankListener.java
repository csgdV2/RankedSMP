package org.csgd.rankedSMP;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RankListener implements Listener {

    private final RankedSMP plugin;
    private final RankManager rankManager;
    private final Set<UUID> processingEffects = new HashSet<>();

    public RankListener(RankedSMP plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyRankBonuses(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> applyRankBonuses(player), 1L);
    }

    public void applyRankBonuses(Player player) {
        int rank = rankManager.getRank(player.getUniqueId());
        applyHealthBonus(player, rank);
        plugin.getInventoryManager().setupPlayerInventory(player);
    }

    private void applyHealthBonus(Player player, int rank) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute == null) return;

        double baseHealth = 20.0;
        double extraHearts = rankManager.getExtraHearts(rank);
        double newMaxHealth = baseHealth + (extraHearts * 2);

        healthAttribute.setBaseValue(newMaxHealth);

        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onXpGain(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int rank = rankManager.getRank(player.getUniqueId());

        if (rank == 0) return;

        double multiplier = rankManager.getXpMultiplier(rank);
        int originalXp = event.getAmount();
        int newXp = (int) Math.round(originalXp * multiplier);

        event.setAmount(newXp);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) return;

        UUID playerId = player.getUniqueId();
        if (processingEffects.contains(playerId)) return;

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) return;

        int rank = rankManager.getRank(playerId);
        if (rank == 0) return;

        double multiplier = rankManager.getEffectMultiplier(rank);

        event.setCancelled(true);

        int newDuration = (int) Math.round(newEffect.getDuration() * multiplier);
        PotionEffect modifiedEffect = new PotionEffect(
                newEffect.getType(),
                newDuration,
                newEffect.getAmplifier(),
                newEffect.isAmbient(),
                newEffect.hasParticles(),
                newEffect.hasIcon()
        );

        processingEffects.add(playerId);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.addPotionEffect(modifiedEffect);
            processingEffects.remove(playerId);
        });
    }

    public void refreshPlayerBonuses(Player player) {
        applyRankBonuses(player);
    }
}

