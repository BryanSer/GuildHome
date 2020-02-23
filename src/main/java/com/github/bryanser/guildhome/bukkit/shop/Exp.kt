package com.github.bryanser.guildhome.bukkit.shop

import com.github.bryanser.brapi.Utils
import com.github.bryanser.brapi.kview.KIcon
import com.github.bryanser.brapi.kview.builder.KViewBuilder
import com.github.bryanser.guildhome.Member
import com.github.bryanser.guildhome.bukkit.BukkitMain
import com.github.bryanser.guildhome.bukkit.GuildConfig
import com.github.bryanser.guildhome.service.impl.BroadcastMessageService
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import java.io.File

class Exp(cs: ConfigurationSection) : Item(cs) {
    val effect: Double
    val time: Double
    val world: List<String>
    val allGuild: Boolean
    val cost: Cost
    val message: List<String>
    val broadcast: Array<String>

    fun createUsingExp(
            owner: String,
            gid: Int,
            leftTime: Long
    ) {
        val al = UsingExp(owner, gid, System.currentTimeMillis() + leftTime)
        val list = buff.getOrPut(gid) { mutableListOf() }
        list += al
        Bukkit.getLogger().info("公会加成: Loot 读取完毕 为公会$gid 增加加成效果 data: $owner, $leftTime")
    }

    inner class UsingExp(
            val owner: String,
            val gid: Int,
            val endTime: Long = System.currentTimeMillis() + (time * 60 * 1000L).toLong()
    ) {

        fun getEffect(p: Player): Double? {
            if (System.currentTimeMillis() > endTime) {
                this.end()
                return -1.0
            }
            if (world.isNotEmpty() && !world.contains(p.world.name)) {
                if (DEBUG) {
                    p.sendMessage("§6DEBUG-所在世界并非配置指定的加成世界")
                }
                return null
            }
            if (allGuild) {
                return effect
            }
            if (owner == p.name) {
                return effect
            }
            if (DEBUG) {
                p.sendMessage("§6DEBUG-个人加成 并非你购买的")
            }
            return null
        }

        val index = this@Exp.index
        fun end() {
            if (allGuild) {
                val all = Utils.getOnlinePlayers()
                if (all.isEmpty()) {
                    return
                }
                BroadcastMessageService.broadcast(gid, all.first(), "§6§l=====§a§l公会公告§6§l=====", "§c§l公会的加成效果已结束")
            } else {
                Bukkit.getPlayer(owner)?.sendMessage("§a§l你的公会个人加成效果已结束")
            }
        }
    }

    init {
        val config = cs.getConfigurationSection("Config")
        effect = config.getDouble("effect")
        time = config.getDouble("time")
        world = config.getStringList("world")
        allGuild = config.getBoolean("allGuild")
        cost = Cost(config.getConfigurationSection("cost"))
        message = config.getStringList("message")?.map { ChatColor.translateAlternateColorCodes('&', it) }
                ?: mutableListOf()
        broadcast = config.getStringList("broadcast")?.map { ChatColor.translateAlternateColorCodes('&', it) }?.toTypedArray()
                ?: arrayOf()
    }

    override fun build(view: KViewBuilder<ShopViewContext>): KIcon<ShopViewContext> {
        return view.icon {
            display {
                super.display(this)
            }
            click {
                if (!init) {
                    return@click
                }
                if (cost.checkCost(this)) {
                    cost.cost(this)
                    val list = buff.getOrPut(guild.id) { mutableListOf() }
                    list += UsingExp(player.name, guild.id)
                    if (message.isNotEmpty()) {
                        for (msg in message.map { it.replace("%player%", player.name) }) {
                            player.sendMessage(msg)
                        }
                    }
                    if (broadcast.isNotEmpty()) {
                        BroadcastMessageService.broadcast(guild.id, player, *(broadcast.map { it.replace("%player%", player.name) }.toTypedArray()))
                    }
                }
            }
        }
    }

    companion object : Listener {
        const val DEBUG = false

        val buff = mutableMapOf<Int, MutableList<UsingExp>>()

        fun save() {
            val f = File(BukkitMain.Plugin.dataFolder, "UsingExp.yml")
            val config = YamlConfiguration()
            val list = mutableListOf<Map<String, Any>>()
            for ((_, v) in buff) {
                for (al in v) {
                    if (System.currentTimeMillis() > al.endTime) {
                        continue
                    }
                    list += mapOf(
                            "gid" to al.gid,
                            "leftTime" to al.endTime - System.currentTimeMillis(),
                            "owner" to al.owner,
                            "index" to al.index
                    )
                }
            }
            config.set("Data", list)
            config.save(f)
        }

        fun load() {
            val f = File(BukkitMain.Plugin.dataFolder, "UsingExp.yml")
            if (!f.exists()) {
                return
            }
            val config = YamlConfiguration.loadConfiguration(f)
            val list = config.getMapList("Data") as List<Map<String, Any>>
            for (map in list) {
                val index = (map["index"] as Number).toInt()
                val gid = (map["gid"] as Number).toInt()
                val leftTime = (map["leftTime"] as Number).toLong()
                val owner = map["owner"] as String
                val item = ShopViewContext.items[index] as? Exp
                        ?: continue
                item.createUsingExp(owner, gid, leftTime)
            }
            f.delete()
        }

        init {
            Bukkit.getPluginManager().registerEvents(this, BukkitMain.Plugin)
            Bukkit.getScheduler().runTaskTimer(BukkitMain.Plugin, {
                for (v in buff.values) {
                    val it = v.iterator()
                    while (it.hasNext()) {
                        val al = it.next()
                        if (System.currentTimeMillis() > al.endTime) {
                            al.end()
                            it.remove()
                        }
                    }
                }
            }, 1200, 1200)
        }

        fun sum(player: Player): Double {
            var max = 0.0
            val m = GuildConfig.cache[player.uniqueId] as? Member
            if (m == null) {
                if (DEBUG) {
                    player.sendMessage("§cDEBUG-找不到公会信息")
                }
                return 0.0
            }
            val it = buff[m.gid]?.iterator()
            if (it == null) {
                if (DEBUG) {
                    player.sendMessage("§cDEBUG-公会未购买经验加成")
                }
                return 0.0
            }
            while (it.hasNext()) {
                val ue = it.next()
                val effect = ue.getEffect(player)
                if (effect != null) {
                    if (effect < 0) {
                        it.remove()
                    } else {
                        if (effect > max) {
                            max = effect
                        }
                    }
                }
            }
            return max
        }

        @EventHandler
        fun onExpChange(evt: PlayerExpChangeEvent) {
            if (DEBUG) {
                evt.player.sendMessage("§6DEBUG-触发经验事件")
            }
            if (evt.amount <= 0) {
                if (DEBUG) {
                    evt.player.sendMessage("§6DEBUG-经验增量小于0 中断执行")
                }
                return
            }
            val s = sum(evt.player)
            if (s > 0) {
                val exp = evt.amount * (1 + s)
                if (DEBUG) {
                    evt.player.sendMessage("§6DEBUG-你的经验加成: ${s}  额外经验量: ${exp.toInt() - evt.amount}")
                }
                evt.amount = exp.toInt()
            }
        }
    }

}