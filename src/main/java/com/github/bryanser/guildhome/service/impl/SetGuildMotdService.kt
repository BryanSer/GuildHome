package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.bungee.GuildSetManager
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.Key
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player

object SetGuildMotdService : Service(
        "set guild motd",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val player = data["Player"] as String
        val p = BungeeMain.Plugin.proxy.getPlayer(player) ?: return
        val gid = data["Gid"] as Int
        async {
            val guild = GuildManager.getGuild(gid) ?: return@async
            if(guild.id != gid){
                return@async
            }
            val ginfo = GuildManager.getMember(p.uniqueId) ?: return@async
            if (ginfo.career < Career.MANAGER) {
                p.sendSyncMsg("§c你没有权限修改MOTD")
                return@async
            }
            val motd = "${data["Motd_0"]}\n${data["Motd_1"]}\n${data["Motd_2"]}\n${data["Motd_3"]}"
            GuildSetManager.updateGuild(guild.id, Key.MOTD, motd)
            p.sendSyncMsg("§6修改成功")
        }
    }

    fun setMotd(gid: Int, motd: Array<String>, from: Player) {
        val data = mutableMapOf<String, Any>()
        data["Player"] = from.name
        data["Gid"] = gid
        data["Motd_0"] = motd[0]
        data["Motd_1"] = motd[1]
        data["Motd_2"] = motd[2]
        data["Motd_3"] = motd[3]
        this.sendData(data, from)
    }

}