package com.github.bryanser.guildhome.bukkit.shop

import com.github.bryanser.brapi.Utils
import com.github.bryanser.guildhome.service.impl.DonateService
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class Cost(config: ConfigurationSection) {
    val level = config.getInt("level", 1)
    val contribution = config.getInt("contribution", 0)
    val items = config.getStringList("item")?.map { Utils.readItemStack(it) } ?: listOf()
    val permission = config.getInt("permission", 3)
    val money = config.getInt("money", 0)

    fun checkCost(context: ShopViewContext): Boolean {
        context.run {
            if (level > guild.level) {
                player.sendMessage("§c公会等级不足")
                return false
            }
            if (guild.contribution < contribution) {
                player.sendMessage("§c公会贡献不足")
                return false
            }
            if (self.career.level < permission) {
                player.sendMessage("§c你的公会权限不能购买这个东西")
                return false
            }
            if (money > 0) {
                val has = Utils.economy!!.getBalance(player)
                if (has < money) {
                    player.sendMessage("§c你的金钱不足")
                    return false
                }
            }
            if (items.isNotEmpty() && !Br.API.Utils.hasEnoughItems(player, items)) {
                player.sendMessage("§c你的物品不足")
                return false
            }
        }
        return true
    }

    fun cost(context: ShopViewContext) {
        context.run {
            if (contribution > 0) {
                DonateService.donate(contribution, player, false, true)
            }
            if (money > 0) {
                Utils.economy!!.withdrawPlayer(player, money.toDouble())
            }
            if (items.isNotEmpty()) {
                Br.API.Utils.removeItem(player, items)
            }
        }
    }
}