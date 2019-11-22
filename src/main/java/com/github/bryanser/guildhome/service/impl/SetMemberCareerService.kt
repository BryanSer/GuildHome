package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.bungee.GuildSetManager
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.Key
import com.github.bryanser.guildhome.service.Service
import org.bukkit.entity.Player
import java.util.*

object SetMemberCareerService : Service(
        "set member career",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val player = data["Player"] as String
        val p = BungeeMain.Plugin.proxy.getPlayer(player) ?: return
        val gid = data["Gid"] as Int
        val target = UUID.fromString(data["Target"] as String)!!
        val career = Career.valueOf(data["Career"] as String)
        async {
            val guild = GuildManager.getGuild(gid) ?: return@async
            if (guild.id != gid) {
                return@async
            }
            val ginfo = GuildManager.getMember(p.uniqueId) ?: return@async
            if (ginfo.career < Career.MANAGER) {
                p.sendSyncMsg("§c权限异常")
                return@async
            }
            val tinfo = GuildManager.getMember(target)
            if (tinfo == null) {
                p.sendSyncMsg("§c找不到对方公会信息")
                return@async
            }
            if (tinfo.gid != gid) {
                p.sendSyncMsg("§c你不能修改别的公会的信息")
                return@async
            }
            if (ginfo.career <= tinfo.career) {
                p.sendSyncMsg("§c权限异常")
                return@async
            }
            if (career == Career.MEMBER) {
                GuildSetManager.updateMember(gid, target, Key.CARREER, career)
                p.sendSyncMsg("§c已成功取消对方职位")
                return@async
            }
            GuildSetManager.updateMember(gid, target, Key.CARREER, career)
            p.sendSyncMsg("§c已成功设定对方职位")
        }
    }

    fun setMemberCareer(gid: Int, target: UUID, career: Career, from: Player) {
        val data = mutableMapOf<String, Any>()
        data["Player"] = from.name
        data["Gid"] = gid
        data["Target"] = target.toString()
        data["Career"] = career.name
        sendData(data, from)
    }
}