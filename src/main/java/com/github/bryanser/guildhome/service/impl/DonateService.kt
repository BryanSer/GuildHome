package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.database.DatabaseHandler
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player

object DonateService : Service(
        "donate",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val value = data["Value"].asInt()
        val from = data["Player"] as String
        val p = BungeeMain.Plugin.proxy.getPlayer(from) ?: return
        val self = data["Self"] as Boolean
        val guild = data["Guild"] as Boolean
        async {
            val member = GuildManager.getMember(p.uniqueId) ?: return@async
            DatabaseHandler.sql {
                if (self) {
                    val ps = this.prepareStatement("UPDATE ${DatabaseHandler.TABLE_GUILD_MEMBER} SET CONTRIBUTION = CONTRIBUTION + ? WHERE NAME = ?")
                    ps.setInt(1, value)
                    ps.setString(2, p.uniqueId.toString())
                    ps.executeUpdate()
                }
                if (guild) {
                    val ps2 = this.prepareStatement("UPDATE ${DatabaseHandler.TABLE_GUILDHOME} SET CONTRIBUTION = CONTRIBUTION + ? WHERE ID = ?")
                    ps2.setInt(1, value)
                    ps2.setInt(2, member.gid)
                    ps2.executeUpdate()
                }
            }
        }
    }

    fun donate(value: Int, from: Player, editSelf: Boolean = true, editGuild: Boolean = true) {
        sendData(mutableMapOf(
                "Player" to from.name,
                "Value" to value,
                "Self" to editSelf,
                "Guild" to editGuild
        ), from)
    }
}