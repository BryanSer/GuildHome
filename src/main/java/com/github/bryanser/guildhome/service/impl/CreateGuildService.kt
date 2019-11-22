package com.github.bryanser.guildhome.service.impl

import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.bukkit.BukkitMain
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.bungee.GuildSetManager
import com.github.bryanser.guildhome.service.Service
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object CreateGuildService : Service(
        "create service",
        true
) {
    override fun onReceive(data: Map<String, Any>) {
        val name = data["Name"] as String
        val player = data["Player"] as String
        val p = BungeeMain.Plugin.proxy.getPlayer(player) ?: return
        async {
            val pi = GuildManager.getMember(p.uniqueId)
            if (pi != null) {
                p.sendMessage(*TextComponent.fromLegacyText("§c无法创建${name}公会, 你已经在一个公会里了"))
                return@async
            }
            val guild = GuildManager.getGuildByName(name)
            if (guild != null) {
                p.sendMessage(*TextComponent.fromLegacyText("§c无法创建${name}公会, 已存在同名公会"))
                return@async
            }
            val (id, reason) = GuildSetManager.createGuild(name, p.uniqueId)
            if (id == null) {
                p.sendSyncMsg("§c无法创建${name}公会, $reason")
            } else {
                p.sendSyncMsg("§6公会创建成功: ID-$id")
            }
        }
    }

    fun createGuild(name: String, from: Player) {
        async {
            val pi = GuildManager.getMember(from.uniqueId)
            if (pi != null) {
                sync {
                    from.sendMessage("§c无法创建${name}公会, 你已经在一个公会里了")
                }
                return@async
            }
            val guild = GuildManager.getGuildByName(name)
            if (guild != null) {
                sync {
                    from.sendMessage("§c无法创建${name}公会, 已存在同名公会")
                }
                return@async
            }
            val data = mutableMapOf<String, Any>()
            data["Name"] = name
            data["Player"] = from.name
            sync{
                this.sendData(data, from)
            }
        }
    }
}