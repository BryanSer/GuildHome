package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.database.DatabaseHandler
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player

object LevelUpService : Service(
        "level up",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val gid = data["Gid"].asInt()
        val player = data["Player"] as String
        val cost = data["Cost"].asInt()
        val p = BungeeMain.Plugin.proxy.getPlayer(player) ?: return
        async {
            DatabaseHandler.sql {
                val ps = this.prepareStatement("UPDATE ${DatabaseHandler.TABLE_GUILDHOME} SET CONTRIBUTION = CONTRIBUTION - ? WHERE ID = ?")
                ps.setInt(1, cost)
                ps.setInt(2, gid)
                ps.executeUpdate()
                val ps2 = this.prepareStatement("UPDATE ${DatabaseHandler.TABLE_GUILDHOME} SET LEVEL = LEVEL + 1 WHERE ID = ?")
                ps2.setInt(1,gid)
                ps2.executeUpdate()
            }
        }
    }

    fun levelUp(gid: Int, cost: Int, from: Player) {

        sendData(mutableMapOf(
                "Gid" to gid,
                "Player" to from.name,
                "Cost" to cost
        ), from)
    }
}