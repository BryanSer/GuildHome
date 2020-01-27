package com.github.bryanser.guildhome.bukkit.shop

import com.github.bryanser.brapi.Utils
import com.github.bryanser.brapi.kview.KIcon
import com.github.bryanser.brapi.kview.builder.KViewBuilder
import com.github.bryanser.guildhome.Member
import com.github.bryanser.guildhome.bukkit.BukkitMain
import com.github.bryanser.guildhome.bukkit.GuildConfig
import com.github.bryanser.guildhome.service.impl.BroadcastMessageService
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.material.Dye
import org.bukkit.material.Wool
import java.io.File
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

@Suppress("LeakingThis")
abstract class Item(
        config: ConfigurationSection
) {
    val index: Int = config.name.toInt()
    var display: (ShopViewContext) -> ItemStack?

    init {
        display = loadDisplay(config)
    }

    open fun loadDisplay(config: ConfigurationSection): (ShopViewContext) -> ItemStack? {
        val dis = config.getString("Display")!!
        return d@{
            val str = papi(dis, it.player).replace("%%", "%")
//            Bukkit.getLogger().info("替换后信息: $str")
            readItemStack(str)
        }
    }

    abstract fun build(view: KViewBuilder<ShopViewContext>): KIcon<ShopViewContext>

    companion object {
        val PAPIRegEx = Pattern.compile("%[^%]*%")

        fun papi(t: String, p: Player): String {
            val replace = mutableSetOf<String>()
            val matcher = PAPIRegEx.matcher(t)
            var format = t
            while (matcher.find()) {
                val g = matcher.group(0)
                if (!replace.contains(g)) {
                    replace.add(g)
                }
            }
            for (s in replace) {
                try {
                    val str = PlaceholderAPI.setPlaceholders(p, s)
                    if (str != null) {
                        format = format.replace(s, str)
                    } else {
                        format = format.replace(s, "")
                    }
                } catch (e: Throwable) {
                    format = format.replace(s, "")
                }
            }
            return format
        }

        val items = HashMap<String, (ConfigurationSection) -> Item>()
        @Deprecated("")
        var itemReplacer: (ItemStack, Player) -> ItemStack = { i, p -> i }

        init {
            items["ICON"] = ::Icon
            items["EXP"] = ::Exp
            items["COMMAND"] = ::Command
            items["LOOT"] = ::Loot
        }

        fun readItemStack(s: String): ItemStack? {
            val item: ItemStack
            item = try {
                ItemStack(Material.getMaterial(s.split(" ").toTypedArray()[0].toInt()))
            } catch (e: NumberFormatException) {
                ItemStack(Material.getMaterial(s.split(" ").toTypedArray()[0]))
            }
            var i = 0
            for (data in s.split(" ").toTypedArray()) {
                var data = data
                if (i == 0) {
                    i++
                    continue
                }
                if (i == 1) {
                    try {
                        item.amount = data.toInt()
                    } catch (e: NumberFormatException) {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l在读取物品: $s 时出现错误"))
                    }
                    i++
                    continue
                }
                if (i == 2) {
                    try {
                        item.durability = data.toShort()
                    } catch (e: NumberFormatException) {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l在读取物品: $s 时出现错误"))
                    }
                    i++
                    continue
                }
                if (data.toLowerCase().contains("name:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    data = ChatColor.translateAlternateColorCodes('&', data)
                    data = data.replace("_".toRegex(), " ")
                    val im = item.itemMeta
                    im.displayName = data
                    item.itemMeta = im
                    continue
                }
                if (data.toLowerCase().contains("lore:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    val lores = data.split("|").toTypedArray()
                    val LoreList: MutableList<String> = ArrayList()
                    for (o in lores.indices) {
                        lores[o] = lores[o].replace("_".toRegex(), " ")
                        lores[o] = ChatColor.translateAlternateColorCodes('&', lores[o])
                    }
                    LoreList.addAll(Arrays.asList(*lores))
                    val im = item.itemMeta
                    im.lore = LoreList
                    item.itemMeta = im
                    continue
                }
                if (data.toLowerCase().contains("hide:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    val im = item.itemMeta
                    for (str in data.split(",").toTypedArray()) {
                        im.addItemFlags(ItemFlag.valueOf(str))
                    }
                    item.itemMeta = im
                    continue
                }
                if (data.toLowerCase().contains("ench:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    val str = data.split("-").toTypedArray()
                    var e: Enchantment? = null
                    e = try {
                        Enchantment.getById(str[0].toInt())
                    } catch (ee: NumberFormatException) {
                        Enchantment.getByName(str[0])
                    }
                    val lv = str[1].toInt()
                    item.addUnsafeEnchantment(e, lv)
                }
                if (data.toLowerCase().contains("color:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    if (item.data is Dye) {
                        val d = item.data as Dye
                        d.color = DyeColor.valueOf(data)
                        item.data = d
                        continue
                    }
                    if (item.data is Wool) {
                        val w = item.data as Wool
                        w.color = DyeColor.valueOf(data)
                        item.data = w
                        continue
                    }
                    if (item.itemMeta is LeatherArmorMeta) {
                        val lam = item.itemMeta as LeatherArmorMeta
                        lam.color = Color.fromRGB(data.toInt())
                        item.itemMeta = lam
                        continue
                    }
                }
            }
            return item
        }
    }
}

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
            val list = config.get("Data") as List<Map<String,Any>>
            for (map in list) {
                val index = (map["index"] as Number).toInt()
                val gid = (map["gid"] as Number).toInt()
                val leftTime = (map["leftTime"] as Number).toLong()
                val owner = map["owner"] as String
                val item = ShopViewContext.items[index] as? Loot ?: continue
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

class Icon(config: ConfigurationSection) : Item(config) {
    override fun build(view: KViewBuilder<ShopViewContext>): KIcon<ShopViewContext> {
        return view.icon {
            initDisplay {
                super.display(this)
            }
        }
    }
}

class Command(cs: ConfigurationSection) : Item(cs) {

    val commands = cs.getStringList("Config.commands").map {
        val s = it.split(":".toRegex(), 2)
        val value = s[1]
        val t: (Player) -> Unit = when (s[0]) {
            "p" -> { p: Player ->
                Bukkit.dispatchCommand(p, value.replace("%player%", p.name))
            }
            "c" -> { p: Player ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.replace("%player%", p.name))
            }
            "op" -> { p: Player ->
                val op = p.isOp
                try {
                    p.isOp = true
                    Bukkit.dispatchCommand(p, value.replace("%player%", p.name))
                } finally {
                    p.isOp = op
                }
            }
            "message" -> {
                val msg = ChatColor.translateAlternateColorCodes('&', value)
                val t: (Player) -> Unit = {
                    it.sendMessage(msg)
                }
                t
            }
            else -> { p: Player -> Unit }
        }
        t
    }
    val cost: Cost = Cost(cs.getConfigurationSection("Config.cost"))


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
                    for (v in commands) {
                        v(player)
                    }
                }
            }
        }
    }

}

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
            val list = config.getMapList("Data") as List<Map<String,Any>>
            for (map in list) {
                val index = (map["index"] as Number).toInt()
                val gid = (map["gid"] as Number).toInt()
                val leftTime = (map["leftTime"] as Number).toLong()
                val owner = map["owner"] as String
                val item = ShopViewContext.items[index] as? Exp ?: continue
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

