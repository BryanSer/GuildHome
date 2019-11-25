package com.github.bryanser.guildhome

import com.github.bryanser.brapi.ItemBuilder
import com.github.bryanser.guildhome.bukkit.GuildView
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.util.*

data class Guild(
        val id: Int,
        val name: String,
        var level: Int,
        var contribution: Int,
        var motd: String,
        var icon: String?
) {
    companion object {
        fun getMaxMemberSize(lv: Int): Int {
            return when (lv) {
                1 -> 30
                2 -> 50
                3 -> 80
                else -> 80
            }
        }
    }
}

data class GuildInfo(
        val gid: Int,
        val name: String,
        val president: String,
        val level: Int,
        val contribution: Int,
        val memberSize: Int,
        val icon: String?,
        val motd:String,
        val score: Int
) {
    var displayIcon: Any? = null

    fun getDisplay(button: Boolean): ItemStack {
        if (displayIcon == null) {
            val iconItem = if (icon == null) {
                GuildView.defaultIcon
            } else {
                GuildView.loadIcon(icon)
            }
            val item = ItemBuilder.createItem(iconItem.type, iconItem.amount, iconItem.durability.toInt()) {
                name("§d公会: §6${this@GuildInfo.name}")
                lore {
                    +"§a会长: ${Bukkit.getOfflinePlayer(UUID.fromString(president))?.name ?: "找不到名字"}"
                    +"§a等级: $level"
                    +"§e公会总贡献值: $contribution"
                    +"§e公会人数: ${memberSize}/${Guild.getMaxMemberSize(level)}"
                    +"§6公会积分: $score"
                    +"§6§l公会信息: "
                    for (motd in motd.split("\n")) {
                        +motd
                    }
                    if (button) {
                        +"§6§l左键点击申请加入公会"
                    }
                }
            }
            displayIcon = item
        }
        return displayIcon as ItemStack
    }
}