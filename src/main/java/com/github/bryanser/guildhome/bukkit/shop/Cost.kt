package com.github.bryanser.guildhome.bukkit.shop

import com.github.bryanser.brapi.ItemMatcher
import com.github.bryanser.brapi.Utils
import com.github.bryanser.guildhome.service.impl.DonateService
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

val floreMatcher: (ItemMatcher, ItemMatcher) -> Boolean = d@{ i1, item ->
    if (i1.id == item.id && i1.durability == item.durability) {
        i1.run {
            if (Bukkit.getItemFactory().equals(item.meta, this.meta)) {
                return@d true
            }
            if (item.meta?.hasLore() ?: false && meta?.hasLore() ?: false) {
                val lore = item.meta!!.lore
                val olore = meta!!.lore
                val it = lore.iterator()
                val oit = olore.iterator()
                while (it.hasNext() && oit.hasNext()) {
                    val str = it.next()
                    val ostr = oit.next()
                    if (str != ostr) {
                        if (str.contains("灵魂绑定")) {
                            it.next()
                            continue
                        }
                        return@d false
                    }
                }
                return@d true
            }
        }
    }
    false
}

class Cost(config: ConfigurationSection) {
    val level = config.getInt("level", 1)
    val contribution = config.getInt("contribution", 0)
    val items = config.getStringList("item")?.map { Utils.readItemStack(it) } ?: listOf()
    val permission = config.getInt("permission", 3)
    val money = config.getInt("money", 0)

    fun checkCost(context: ShopViewContext): Boolean {
        context.run {
            if (level > guild.level) {
                player.sendMessage("§c你的公会等级不足")
                return false
            }
            if (guild.contribution < contribution) {
                player.sendMessage("§c你的公会贡献不足")
                return false
            }
            if (self.career.level < permission) {
                player.sendMessage("§c你在公会的级别不能购买这个东西")
                return false
            }
            if (money > 0) {
                val has = Utils.economy!!.getBalance(player)
                if (has < money) {
                    player.sendMessage("§c所需的节操不足")
                    return false
                }
            }
            if (items.isNotEmpty() && !Utils.hasEnoughItems(player, items, floreMatcher)) {
                player.sendMessage("§c所需的物品不足")
                return false
            }
        }
        return true
    }

    fun cost(context: ShopViewContext) {
        context.run {
            if (contribution > 0) {
                DonateService.donate(-contribution, player, false, true)
            }
            if (money > 0) {
                Utils.economy!!.withdrawPlayer(player, money.toDouble())
            }
            if (items.isNotEmpty()) {
                Utils.removeItem(player, items, floreMatcher)
            }
            this.guild.contribution -= contribution
        }
    }
}