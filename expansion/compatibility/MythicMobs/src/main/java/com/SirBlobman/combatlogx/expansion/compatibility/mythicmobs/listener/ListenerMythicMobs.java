package com.SirBlobman.combatlogx.expansion.compatibility.mythicmobs.listener;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.SirBlobman.combatlogx.api.ICombatLogX;
import com.SirBlobman.combatlogx.api.event.PlayerPreTagEvent;
import com.SirBlobman.combatlogx.api.event.PlayerPreTagEvent.TagReason;
import com.SirBlobman.combatlogx.api.event.PlayerPreTagEvent.TagType;
import com.SirBlobman.combatlogx.api.utility.ICombatManager;
import com.SirBlobman.combatlogx.expansion.compatibility.mythicmobs.CompatibilityMythicMobs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import io.lumine.xikage.mythicmobs.mobs.MobManager;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;

public class ListenerMythicMobs implements Listener {
    private final ICombatLogX plugin;
    public ListenerMythicMobs(CompatibilityMythicMobs expansion) {
        this.plugin = expansion.getPlugin();
    }
    
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void beforeTag(PlayerPreTagEvent e) {
        LivingEntity enemy = e.getEnemy();
        if(enemy == null) return;
        
        String mobName = getMythicMobName(enemy);
        if(mobName == null) return;
        
        boolean preventTag = shouldPreventTag(mobName);
        if(preventTag) e.setCancelled(true);
    }
    
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity damaged = e.getEntity();
        
        if(damager instanceof Player) {
            Player player = (Player) damager;
            checkForceTag(player, damaged, TagReason.ATTACKER);
        }
        
        if(damaged instanceof Player) {
            Player player = (Player) damaged;
            checkForceTag(player, damager, TagReason.ATTACKED);
        }
    }
    
    private void checkForceTag(Player player, Entity entity, TagReason reason) {
        String mobName = getMythicMobName(entity);
        if(mobName == null || !shouldForceTag(mobName)) return;
        
        ICombatManager combatManager = this.plugin.getCombatManager();
        if(entity instanceof LivingEntity) {
            LivingEntity enemy = (LivingEntity) entity;
            combatManager.tag(player, enemy, TagType.MOB, reason);
            return;
        }
        
        combatManager.tag(player, null, TagType.UNKNOWN, reason);
    }
    
    private String getMythicMobName(Entity entity) {
        if(entity == null) return null;
    
        MythicMobs mythicMobs = MythicMobs.inst();
        MobManager mobManager = mythicMobs.getMobManager();
        
        UUID uuid = entity.getUniqueId();
        Optional<ActiveMob> activeMobOptional = mobManager.getActiveMob(uuid);
        if(activeMobOptional.isPresent()) {
            ActiveMob activeMob = activeMobOptional.get();
            MythicMob mobType = activeMob.getType();
            return mobType.getInternalName();
        }
        
        return null;
    }
    
    private boolean shouldPreventTag(String mobName) {
        FileConfiguration config = this.plugin.getConfig("mythicmobs-compatibility.yml");
        List<String> noTagMobNameList = config.getStringList("no-tag-mob-types");
        return noTagMobNameList.contains(mobName);
    }
    
    private boolean shouldForceTag(String mobName) {
        FileConfiguration config = this.plugin.getConfig("mythicmobs-compatibility.yml");
        List<String> forceTagMobNameList = config.getStringList("force-tag-mob-types");
        return forceTagMobNameList.contains(mobName);
    }
}