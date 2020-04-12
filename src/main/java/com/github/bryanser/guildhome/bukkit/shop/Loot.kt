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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Loot(cs: ConfigurationSection) : Item(cs) {

    val guildKey: String = cs.getString("Config.GuildKey", "null")
    val cost: Cost = Cost(cs.getConfigurationSection("Config.cost"))
    val allGuild: Boolean = cs.getBoolean("Config.allGuild", true)
    val message: List<String>
    val broadcast: Array<String>
    val time: Double


    init {
        val config = cs.getConfigurationSection("Config")
        time = config.getDouble("time")
        message = config.getStringList("message")?.map { ChatColor.translateAlternateColorCodes('&', it) }
                ?: mutableListOf()
        broadcast = config.getStringList("broadcast")?.map { ChatColor.translateAlternateColorCodes('&', it) }?.toTypedArray()
                ?: arrayOf()
    }


    val df = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss")
    override fun info(gid: Int): String? {
        for ((g, buf) in activing) {
            if (g == gid) {
                for (ue in buf) {
                    if (ue.index == index) {
                        return "到期时间: ${df.format(Date(ue.endTime))}"
                    }
                }
            }
        }
        return null
    }

    inner class ActiveLoot(
            val owner: String,
            val gid: Int,
            val endTime: Long = System.currentTimeMillis() + (time * 60 * 1000L).toLong()
    ) {

        fun isActive(p: Player, key: String): Boolean? {
            if (System.currentTimeMillis() > endTime) {
                this.end()
                return null
            }
            if (key != guildKey) {
                return false
            }
            if (allGuild) {
                return true
            }
            if (owner == p.name) {
                return true
            }
            return false
        }

        val index = this@Loot.index

        fun end() {
            if (allGuild) {
                val all = Utils.getOnlinePlayers()
                if (all.isEmpty()) {
                    return
                }
                BroadcastMessageService.broadcast(gid, all.first(), "§6§l=====§a§l公会公告§6§l=====", "§c§l公会的加成效果已结束")
            } else {
                Bukkit.getPlayer(owner)?.sendMessage("§6§l你的公会个人加成已结束")
            }
        }
    }

    fun createLoot(
            owner: String,
            gid: Int,
            leftTime: Long
    ) {
        val al = ActiveLoot(owner, gid, System.currentTimeMillis() + leftTime)
        val list = activing.getOrPut(gid) { mutableListOf() }
        list += al
        Bukkit.getLogger().info("公会加成: Loot 读取完毕 为公会$gid 增加加成效果 data: $owner, $leftTime")
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
                    val list = activing.getOrPut(guild.id) { mutableListOf() }
                    list += ActiveLoot(player.name, guild.id)
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

    companion object {
        val activing = mutableMapOf<Int, MutableList<ActiveLoot>>()

        fun save() {
            val f = File(BukkitMain.Plugin.dataFolder, "ActiveLoot.yml")
            val config = YamlConfiguration()
            val list = mutableListOf<Map<String, Any>>()
            for ((_, v) in activing) {
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
            val f = File(BukkitMain.Plugin.dataFolder, "ActiveLoot.yml")
            if (!f.exists()) {
                return
            }
            val config = YamlConfiguration.loadConfiguration(f)
            val list = config.get("Data") as List<Map<String, Any>>
            for (map in list) {
                val index = (map["index"] as Number).toInt()
                val gid = (map["gid"] as Number).toInt()
                val leftTime = (map["leftTime"] as Number).toLong()
                val owner = map["owner"] as String
                val item = ShopViewContext.items[index] as? Loot
                        ?: continue
                item.createLoot(owner, gid, leftTime)
            }
            f.delete()
        }

        init {
            Bukkit.getScheduler().runTaskTimer(BukkitMain.Plugin, {
                for (v in activing.values) {
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

        fun isActiving(key: String, p: Player): Boolean {
            val m = GuildConfig.cache[p.uniqueId] as? Member
            if (m == null) {
                if (Exp.DEBUG) {
                    p.sendMessage("§cDEBUG-找不到公会信息")
                }
                return false
            }
            val it = activing[m.gid]?.iterator()
            if (it == null) {
                if (Exp.DEBUG) {
                    p.sendMessage("§cDEBUG-公会未购买加成")
                }
                return false
            }
            while (it.hasNext()) {
                val al = it.next()
                val t = al.isActive(p, key)
                if (t == null) {
                    it.remove()
                    continue
                }
                if (t) {
                    return true
                }
            }

            return false
        }

    }

}