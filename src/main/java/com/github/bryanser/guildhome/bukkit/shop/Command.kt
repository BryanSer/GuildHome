package com.github.bryanser.guildhome.bukkit.shop

import com.github.bryanser.brapi.kview.KIcon
import com.github.bryanser.brapi.kview.builder.KViewBuilder
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

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


    override fun info(gid: Int): String? {
        return null
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
                    for (v in commands) {
                        v(player)
                    }
                }
            }
        }
    }

}