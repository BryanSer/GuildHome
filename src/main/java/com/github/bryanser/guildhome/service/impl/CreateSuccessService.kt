package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.brapi.Utils
import com.github.bryanser.guildhome.bukkit.BukkitMain
import com.github.bryanser.guildhome.bukkit.GuildConfig
import com.github.bryanser.guildhome.service.Service
import net.md_5.bungee.api.connection.ProxiedPlayer
import org.bukkit.Bukkit
import java.util.*

object CreateSuccessService : Service(
        "create success",
        false
) {
    override fun onReceive(data: Map<String, Any>) {
        val uuid = UUID.fromString(data["UUID"] as String)
        val success = data["Success"] as Boolean
        if (!success) {
            val p = Bukkit.getOfflinePlayer(uuid) ?: return
            if (p.isOnline) {
                p.player.sendMessage("§c由于公会创建失败 创建费用返还给你")
                Utils.economy!!.depositPlayer(p, GuildConfig.create_cost)
            }
        }
    }

    fun success(success: Boolean, from: ProxiedPlayer) {
        val data = mutableMapOf<String, Any>()
        data["UUID"] = from.uniqueId.toString()
        data["Success"] = success
        sendData(data, from)
    }
}