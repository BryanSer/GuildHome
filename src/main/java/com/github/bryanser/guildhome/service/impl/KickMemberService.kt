package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.bungee.GuildSetManager
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player
import java.util.*

object KickMemberService : Service(
        "kick member",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val gid = data["Gid"].asInt()
        val target = UUID.fromString(data["Target"] as String)
        GuildSetManager.removeMember(target, gid)

    }

    fun kick(gid: Int, target: UUID, from: Player) {
        val data = mutableMapOf<String, Any>()
        data["Gid"] = gid
        data["Target"] = target.toString()
        sendData(data, from)
    }
}