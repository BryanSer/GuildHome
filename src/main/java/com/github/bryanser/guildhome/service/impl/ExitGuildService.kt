package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.bungee.GuildSetManager
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player

object ExitGuildService : Service(
        "exit guild",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val player = data["Player"] as String
        val p = BungeeMain.Plugin.proxy.getPlayer(player) ?: return
        async {
            val member = GuildManager.getMember(p.uniqueId)
            if(member == null){
                p.sendSyncMsg("§c你本来就不属于任何公会")
                return@async
            }
            BroadcastMessageService.broadcastBungee(member.gid, arrayOf(
                    "§6========§c[公会公告]§6========",
                    "§a§l成员 ${p.name} 离开了公会"
            ))
            GuildSetManager.removeMember(p.uniqueId, member.gid)
        }
    }

    fun exit(from: Player) {
        val data = mutableMapOf<String, Any>()
        data["Name"] = from.name
        sendData(data, from)
    }
}