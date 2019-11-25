package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player

object ApplyService : Service(
        "apply guild",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val name = data["Name"] as String
         val player = data["Player"] as String
        val p = BungeeMain.Plugin.proxy.getPlayer(player) ?: return
        async {
            val guild = GuildManager.getGuildByName(name)
            if(guild == null){
                p.sendSyncMsg("§c名为${name}的公会不存在,请确认公会名是否正确")
                return@async
            }
            if(GuildManager.getMember(p.uniqueId) != null){
                p.sendSyncMsg("§c你已经有公会了哦")
                return@async
            }
            val msg = GuildManager.addApply(guild, p.uniqueId)
            p.sendSyncMsg(msg)
        }
    }

    fun apply(name: String, from: Player) {
        val data = mutableMapOf<String, Any>()
        data["Name"] = name
        data["Player"] = from.name
        sendData(data, from)
    }
}