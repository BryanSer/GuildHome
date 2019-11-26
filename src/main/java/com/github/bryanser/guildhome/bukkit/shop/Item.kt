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
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.inventory.ItemStack

@Suppress("LeakingThis")
abstract class Item(
        config: ConfigurationSection
) {
    var display: ItemStack? = null

    init {
        display = loadDisplay(config)
    }

    open fun loadDisplay(config: ConfigurationSection): ItemStack? {
        val dis = config.getString("Display") ?: return null
        return Utils.readItemStack(dis)
    }

    abstract fun build(view: KViewBuilder<ShopViewContext>): KIcon<ShopViewContext>

    companion object {
        val items = HashMap<String, (ConfigurationSection) -> Item>()

        init {
            items["ICON"] = ::Icon
            items["EXP"] = ::Exp
        }
    }
}

class Icon(config: ConfigurationSection) : Item(config) {
    override fun build(view: KViewBuilder<ShopViewContext>): KIcon<ShopViewContext> {
        return view.icon {
            initDisplay {
                super.display
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

    inner class UsingExp(
            val owner: String,
            val gid: Int
    ) {
        val endTime = System.currentTimeMillis() + (time * 60 * 1000L).toLong()

        fun getEffect(p: Player): Double? {
            if (System.currentTimeMillis() > endTime) {
                return -1.0
            }
            if (world.isNotEmpty() && !world.contains(p.world.name)) {
                return null
            }
            if (allGuild) {
                return effect
            }
            if (owner == p.name) {
                return effect
            }
            return null
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
                super.display
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
        val buff = mutableMapOf<Int, MutableList<UsingExp>>()

        init {
            Bukkit.getPluginManager().registerEvents(this, BukkitMain.Plugin)
        }

        inline fun sum(player: Player): Double {
            var max = 0.0
            val m = GuildConfig.cache[player.uniqueId] as? Member ?: return 0.0
            val it = buff[m.gid]?.iterator() ?: return 0.0
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
            if (evt.amount <= 0) {
                return
            }
            val s = sum(evt.player)
            if (s > 0) {
                val exp = evt.amount * (1 + s)
                evt.amount = exp.toInt()
            }
        }
    }

}

