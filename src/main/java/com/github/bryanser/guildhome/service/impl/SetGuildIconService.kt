package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.bungee.GuildSetManager
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.Key
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player

object SetGuildIconService : Service(
        "set guild icon",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val player = data["Player"] as String
        val p = BungeeMain.Plugin.proxy.getPlayer(player) ?: return
        val gid = data["Gid"] as Int
        async {
            val guild = GuildManager.getGuild(gid) ?: return@async
            val ginfo = GuildManager.getMember(p.uniqueId) ?: return@async
            if(ginfo.gid != gid){
                return@async
            }
            if (ginfo.career < Career.MANAGER) {
                p.sendSyncMsg("§c你没有权限修改ICON")
                return@async
            }
            GuildSetManager.updateGuild(guild.id, Key.ICON, data["Icon"] as String)
            p.sendSyncMsg("§6修改成功")
        }
    }

    fun setGuildIcon(gid: Int, icon: String, from: Player) {
        val data = mutableMapOf<String, Any>()
        data["Player"] = from.name
        data["Gid"] = gid
        data["Icon"] = icon
        this.sendData(data, from)
    }
}