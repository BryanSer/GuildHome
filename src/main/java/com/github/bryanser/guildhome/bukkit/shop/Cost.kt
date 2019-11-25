package com.github.bryanser.guildhome.bukkit.shop

import com.github.bryanser.brapi.Utils
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class Cost(config: ConfigurationSection) {
    val level = config.getInt("level", 1)
    val contribution = config.getInt("contribution", 0)
    val items = config.getStringList("item")?.map { Utils.readItemStack(it) } ?: listOf()
    val permission = config.getInt("permission", 3)
    val money = config.getInt("money", 0)

    fun checkCost(context: ShopViewContext): Boolean {
        TODO()
    }

    fun cost(context: ShopViewContext) {
        TODO()
    }
}