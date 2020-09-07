package com.SirBlobman.combatlogx.api.expansion.region;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import com.SirBlobman.api.utility.Validate;
import com.SirBlobman.combatlogx.api.ICombatLogX;
import com.SirBlobman.combatlogx.api.ICombatManager;
import com.SirBlobman.combatlogx.api.object.NoEntryMode;
import com.SirBlobman.combatlogx.api.object.TagType;

public abstract class RegionHandler {
    private final RegionExpansion expansion;
    private final List<UUID> cooldownList;
    public RegionHandler(RegionExpansion expansion) {
        this.expansion = Validate.notNull(expansion, "expansion must not be null!");
        this.cooldownList = new ArrayList<>();
    }

    public final void sendEntryDeniedMessage(Player player, LivingEntity enemy) {
        if(player == null || enemy == null) return;
        UUID uuid = player.getUniqueId();
        if(this.cooldownList.contains(uuid)) return;

        TagType tagType = (enemy instanceof Player ? TagType.PLAYER : TagType.MOB);
        String messagePath = getEntryDeniedMessagePath(tagType);
        if(messagePath == null) return;

        ICombatLogX plugin = this.expansion.getPlugin();
        JavaPlugin javaPlugin = plugin.getPlugin();
        String message = plugin.getMessageColoredWithPrefix(player, messagePath);
        plugin.sendMessage(player, message);

        this.cooldownList.add(uuid);
        long cooldown = getEntryDeniedMessageCooldown();
        long delay = (cooldown * 20L);

        Runnable task = () -> this.cooldownList.remove(uuid);
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLaterAsynchronously(javaPlugin, task, delay);
    }

    public final void preventEntry(Cancellable e, Player player, Location fromLocation, Location toLocation) {
        if(player == null) return;
        UUID uuid = player.getUniqueId();

        ICombatLogX plugin = this.expansion.getPlugin();
        JavaPlugin javaPlugin = plugin.getPlugin();
        ICombatManager combatManager = plugin.getCombatManager();
        if(!combatManager.isInCombat(player)) return;

        LivingEntity enemy = combatManager.getEnemy(player);
        sendEntryDeniedMessage(player, enemy);

        NoEntryMode noEntryMode = getNoEntryMode();
        switch(noEntryMode) {
            case KILL_PLAYER:
                player.setHealth(0.0D);
                break;

            case TELEPORT_TO_ENEMY:
                if(enemy != null) {
                    player.teleport(enemy);
                    break;
                }

            case CANCEL_EVENT:
                e.setCancelled(true);
                break;

            case KNOCKBACK_PLAYER:
                Runnable task = () -> knockbackPlayer(player, fromLocation, toLocation);
                BukkitScheduler scheduler = Bukkit.getScheduler();
                scheduler.runTaskLater(javaPlugin, task, 1L);

            case DISABLED:
            case VULNERABLE:
            default:
                break;
        }
    }

    private void knockbackPlayer(Player player, Location fromLocation, Location toLocation) {
        if(player == null || fromLocation == null || toLocation == null) return;
        Vector velocity = getKnockback(fromLocation, toLocation);
        player.setVelocity(velocity);
    }

    private Vector getKnockback(Location fromLocation, Location toLocation) {
        Vector fromVector = fromLocation.toVector();
        Vector toVector = toLocation.toVector();

        Vector subtract = fromVector.subtract(toVector);
        Vector normal = subtract.normalize();

        double strength = getKnockbackStrength();
        Vector multiply = normal.multiply(strength);
        return makeFinite(multiply);
    }

    private Vector makeFinite(Vector original) {
        double originalX = original.getX();
        double originalY = original.getY();
        double originalZ = original.getZ();

        double newX = makeFinite(originalX);
        double newY = makeFinite(originalY);
        double newZ = makeFinite(originalZ);
        return new Vector(newX, newY, newZ);
    }

    private double makeFinite(double original) {
        if(Double.isNaN(original)) return 0.0D;
        if(Double.isInfinite(original)) return (original < 0 ? -1.0D : 1.0D);
        return original;
    }

    public abstract String getEntryDeniedMessagePath(TagType tagType);
    public abstract long getEntryDeniedMessageCooldown();

    public abstract NoEntryMode getNoEntryMode();
    public abstract double getKnockbackStrength();

    public abstract boolean isSafeZone(Location location, TagType tagType);
}