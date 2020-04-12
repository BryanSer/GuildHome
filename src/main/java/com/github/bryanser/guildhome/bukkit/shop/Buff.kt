package com.github.bryanser.guildhome.bukkit.shop

import Br.API.Utils
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
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Buff(cs: ConfigurationSection) : Item(cs) {
    val effect: List<PotionEffect>
    val time: Double
    val world: List<String>
    val allGuild: Boolean
    val cost: Cost
    val message: List<String>
    val broadcast: Array<String>
    val power: Int

    fun createUsingEffect(
            owner: String,
            gid: Int,
            leftTime: Long
    ) {
        val al = UsingEffect(owner, gid, System.currentTimeMillis() + leftTime)
        val list = buff.getOrPut(gid) { mutableListOf() }
        list += al
        Bukkit.getLogger().info("公会加成: WeLore 读取完毕 为公会$gid 增加加成效果 data: $owner, $leftTime")
    }

    val df = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss")
    override fun info(gid: Int): String? {
        for((g,buf) in buff){
            if(g == gid){
                for(ue in buf){
                    if(ue.index == index){
                        return "到期时间: ${df.format(Date(ue.endTime))}"
                    }
                }
            }
        }
        return null
    }

    inner class UsingEffect(
            val owner: String,
            val gid: Int,
            val endTime: Long = System.currentTimeMillis() + (time * 60 * 1000L).toLong()
    ) {
        val icon: Buff = this@Buff

        val index = this@Buff.index
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
        val buff = config.getStringList("buff")
        effect = buff.map { s ->
            val str = s.split("|")
            PotionEffect(PotionEffectType.getById(str[0].toInt()), 300, str[1].toInt() - 1)
        }
        time = config.getDouble("time")
        world = config.getStringList("world")
        allGuild = config.getBoolean("allGuild")
        cost = Cost(config.getConfigurationSection("cost"))
        message = config.getStringList("message")?.map { ChatColor.translateAlternateColorCodes('&', it) }
                ?: mutableListOf()
        broadcast = config.getStringList("broadcast")?.map { ChatColor.translateAlternateColorCodes('&', it) }?.toTypedArray()
                ?: arrayOf()
        power = config.getInt("power", 0)
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
                    list += UsingEffect(player.name, guild.id)
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

        fun sum(player: Player): List<PotionEffect>? {
            val m = GuildConfig.cache[player.uniqueId] as? Member
            if (m == null) {
                if (DEBUG) {
                    player.sendMessage("§cDEBUG-找不到公会信息")
                }
                return null
            }
            val it = buff[m.gid]?.iterator()
            if (it == null) {
                if (DEBUG) {
                    player.sendMessage("§cDEBUG-公会未购买经验加成")
                }
                return null
            }
            var mp = -10000
            var data: List<PotionEffect>? = null
            while (it.hasNext()) {
                val ue = it.next()
                if (ue.icon.power > mp) {
                    mp = ue.icon.power
                    data = ue.icon.effect
                }
            }
            return data
        }

        val buff = mutableMapOf<Int, MutableList<UsingEffect>>()

        fun save() {
            val f = File(BukkitMain.Plugin.dataFolder, "UsingEffect.yml")
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
            val f = File(BukkitMain.Plugin.dataFolder, "UsingEffect.yml")
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
                val item = ShopViewContext.items[index] as? Buff
                        ?: continue
                item.createUsingEffect(owner, gid, leftTime)
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

            Bukkit.getScheduler().runTaskTimer(BukkitMain.Plugin, {
                for (p in Utils.getOnlinePlayers()) {
                    val eff = sum(p) ?: continue
                    for (e in eff) {
                        p.removePotionEffect(e.type)
                        p.addPotionEffect(PotionEffect(e.type, e.duration, e.amplifier))
                    }
                }
            }, 100, 20)
        }

    }

}