package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.service.Service
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player

object BroadcastMessageService : Service(
        "broadcast message",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val gid = data["Gid"].asInt()
        val msg = data["Message"] as Array<String>
        broadcastBungee(gid, msg)
    }

    fun broadcast(gid: Int, from: Any, vararg msg: String) {
        if (!bukkit) {
            broadcastBungee(gid, msg)
            return
        }
        if (from !is Player) {
            return
        }
        val data = mutableMapOf<String, Any>()
        data["Gid"] = gid
        data["Message"] = msg
        this.sendData(data, from)
    }

    fun broadcastBungee(gid: Int, msg: Array<out String>) {
        for (member in GuildManager.getMembers(gid)) {
            val p = BungeeMain.Plugin.proxy.getPlayer(member.uuid) ?: continue
            for (s in msg) {
                p.sendMessage(*TextComponent.fromLegacyText(s))
            }
        }
    }
}