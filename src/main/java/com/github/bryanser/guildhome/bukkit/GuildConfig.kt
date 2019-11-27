package com.github.bryanser.guildhome.bukkit

import com.avaje.ebean.annotation.EnumValue
import com.github.bryanser.brapi.Utils
import com.github.bryanser.guildhome.*
import com.github.bryanser.guildhome.database.UserName
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object GuildConfig : Listener {
    val costItem = mutableMapOf<Int, Cost>()

    lateinit var protectedWorlds: MutableList<String>

    var create_cost: Double = 1000.0
    val cache = ConcurrentHashMap<UUID, IMember>()
    val guilds = ConcurrentHashMap<Int, GuildInfo>()
    val players = CopyOnWriteArrayList<UUID>()


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDamage(evt: EntityDamageByEntityEvent) {
        if (protectedWorlds.isEmpty()) {
            return
        }
        val p = evt.entity as? Player ?: return
        val d = evt.damager as? Player ?: return
        if (!protectedWorlds.contains(p.world.name)) {
            return
        }
        val pm = cache[p.uniqueId] as? Member ?: return
        val dm = cache[d.uniqueId] as? Member ?: return
        if (pm.gid == dm.gid) {
            evt.isCancelled = true
            d.sendMessage("§e§l你不能攻击和你相同公会的人哦")
        }
    }

    @EventHandler
    fun onJoin(evt: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskAsynchronously(BukkitMain.Plugin) {
            UserName[evt.player.uniqueId] = evt.player.name
        }
    }

    fun init() {
        Bukkit.getScheduler().runTaskTimer(BukkitMain.Plugin, {
            val list = Bukkit.getOnlinePlayers().map(Player::getUniqueId).toList()
            players.clear()
            players.addAll(list)
        }, 300, 300)
        Bukkit.getScheduler().runTaskTimerAsynchronously(BukkitMain.Plugin, {
            for (name in players) {
                val m = GuildManager.getMember(name) ?: NullMember()
                cache[name] = m
            }
        }, 600, 600)
        Bukkit.getScheduler().runTaskTimerAsynchronously(BukkitMain.Plugin, {
            for (g in GuildManager.getAllGuild()) {
                guilds[g.gid] = g
            }
        }, 700, 700)
    }

    class Cost(config: ConfigurationSection) {
        val cost: Int = config.getInt("Cost", 0)
        val items: List<ItemStack> = config.getStringList("Items")?.map { Utils.readItemStack(it) }
                ?: listOf()
        val info = config.getStringList("Info").map { ChatColor.translateAlternateColorCodes('&', it)!! }
    }

    fun loadConfig(config: ConfigurationSection) {
        costItem.clear()
        create_cost = config.getDouble("Cost.Create", 1000.0)
        val level = config.getConfigurationSection("Level")
        for (key in level.getKeys(false)) {
            val lv = key.toInt()
            Guild.maxMember[lv] = level.getInt("$key.MaxMember")
            if (lv > 1) {
                costItem[lv] = Cost(level.getConfigurationSection(key))
            }
        }
        protectedWorlds = config.getStringList("ProtectedWorlds") ?: mutableListOf()
    }

}