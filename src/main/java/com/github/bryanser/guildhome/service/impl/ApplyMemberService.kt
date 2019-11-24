package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player
import java.util.*

object ApplyMemberService : Service(
        "accept apply member",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val player = data["Player"] as String
        val p = BungeeMain.Plugin.proxy.getPlayer(player) ?: return
        val gid = data["Gid"].asInt()
        val accept = data["Accept"] as Boolean
        val target = UUID.fromString(data["Target"] as String)
        val msg = data["Message"] as Boolean
        async {
            val guild = GuildManager.getGuild(gid) ?: return@async
            val ginfo = GuildManager.getMember(p.uniqueId) ?: return@async
            if (ginfo.gid != gid) {
                return@async
            }
            if (ginfo.career < Career.MANAGER) {
                p.sendSyncMsg("§c你没有权限同意或拒绝加入请求")
                return@async
            }
            if (accept) {
                val s = GuildManager.apply(gid, target)
                p.sendSyncMsg(s)
                if (msg) {
                    if (s != "§c请求处理失败: 对方已经加入了别的公会") {
                        target.sendMsg(s)
                    }else{
                        target.sendMsg("§c请求处理失败: 你已经有公会了")
                    }
                }
            } else {
                val s = GuildManager.refuse(gid, target)
                p.sendSyncMsg(s)
            }
        }
    }

    fun acceptApply(gid: Int, uuid: UUID, from: Player, accept: Boolean, msg: Boolean = false) {
        val data = mutableMapOf<String, Any>()
        data["Gid"] = gid
        data["Target"] = uuid.toString()
        data["Player"] = from.name
        data["Accept"] = accept
        data["Message"] = msg
        this.sendData(data, from)
    }
}