package com.github.bryanser.guildhome.bukkit.shop

import br.kt.welore.attribute.AttributeInfo
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WeLore(cs: ConfigurationSection) : Item(cs) {
    val attribute: MutableMap<String, Double> = hashMapOf()
    val time: Double
    val world: List<String>
    val allGuild: Boolean
    val cost: Cost
    val message: List<String>
    val broadcast: Array<String>
    val power: Int

    fun createUsingWeLore(
            owner: String,
            gid: Int,
            leftTime: Long
    ) {
        val al = UsingWeLore(owner, gid, System.currentTimeMillis() + leftTime)
        val list = buff.getOrPut(gid) { mutableListOf() }
        list += al
        Bukkit.getLogger().info("公会加成: WeLore 读取完毕 为公会$gid 增加加成效果 data: $owner, $leftTime")
    }

    inner class UsingWeLore(
            val owner: String,
            val gid: Int,
            val endTime: Long = System.currentTimeMillis() + (time * 60 * 1000L).toLong()
    ) {
        val icon: WeLore = this@WeLore

        val index = this@WeLore.index
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
        val cts = config.getConfigurationSection("attribute")
        for (key in cts.getKeys(false)) {
            attribute[key] = cts.getDouble(key)
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
                    list += UsingWeLore(player.name, guild.id)
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



    val df = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss")
    override fun info(gid: Int): String? {
        for((g,buf) in buff){
            if(g == gid){
                for(ue in buf){
                    if(ue.index == index){
                        return "到期时间:_${df.format(Date(ue.endTime))}"
                    }
                }
            }
        }
        return null
    }

    companion object : Listener {
        const val DEBUG = false

        fun sum(player: Player): Map<String, Double>? {
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
            var data : Map<String, Double>? = null
            while (it.hasNext()) {
                val ue = it.next()
                if(ue.icon.power > mp){
                    mp = ue.icon.power
                    data = ue.icon.attribute
                }
            }
            return data
        }

        val buff = mutableMapOf<Int, MutableList<UsingWeLore>>()

        fun save() {
            val f = File(BukkitMain.Plugin.dataFolder, "UsingWeLore.yml")
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

        @EventHandler
        fun onAttribute(evt: br.kt.welore.attribute.AttributeLoadEvent) {
            val p = evt.entity as? Player ?: return
            val t = sum(p) ?: return
            for((k,v) in t){
//                p.sendMessage("§6追加属性$k: $v")
                val i = evt.data.data[k] ?: continue
                i.add(AttributeInfo(i.attribute, v))
            }
        }

        fun load() {
            val f = File(BukkitMain.Plugin.dataFolder, "UsingWeLore.yml")
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
                val item = ShopViewContext.items[index] as? WeLore
                        ?: continue
                item.createUsingWeLore(owner, gid, leftTime)
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

    }

}