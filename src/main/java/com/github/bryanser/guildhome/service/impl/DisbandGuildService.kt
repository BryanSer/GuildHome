package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.DatabaseHandler
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player
import java.util.*

object DisbandGuildService : Service(
        "disband guild",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val uuid = UUID.fromString(data["UUID"] as String)
        val p = BungeeMain.Plugin.proxy.getPlayer(uuid)
        val member = GuildManager.getMember(uuid)
        if (member == null) {
            p.sendSyncMsg("§c你还有没有任何公会")
            return
        }
        if (member.career != Career.PRESIDENT) {
            p.sendSyncMsg("§c你不是会长")
            return
        }
        BroadcastMessageService.broadcastBungee(member.gid,
                arrayOf(
                        "§6========§c[公会公告]§6========",
                        "§c§l您所在的公会已经被会长解散"
                )
        )
        async {
            DatabaseHandler.sql {
                val ps1 = this.prepareStatement("DELETE FROM ${DatabaseHandler.TABLE_GUILD_MEMBER} WHERE GID = ?")
                ps1.setInt(1, member.gid)
                ps1.executeUpdate()
                val ps2 = this.prepareStatement("DELETE FROM ${DatabaseHandler.TABLE_GUILD_APPLY} WHERE GID = ?")
                ps2.setInt(1, member.gid)
                ps2.executeUpdate()
                val ps3 = this.prepareStatement("DELETE FROM ${DatabaseHandler.TABLE_GUILDHOME} WHERE ID = ?")
                ps3.setInt(1, member.gid)
                ps3.executeUpdate()
            }
        }
    }

    fun disband(from: Player) {
        val data = mutableMapOf<String, Any>()
        data["UUID"] = from.uniqueId.toString()
        sendData(data, from)
    }
}