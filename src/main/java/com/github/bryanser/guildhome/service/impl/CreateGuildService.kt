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
        val p = BungeeMain.Plugin.proxy.getPlayer(player)
        if (p == null) {
            CreateSuccessService.success(false, p)
            return
        }
        async {
            if (name.length > 5) {
                sync {
                    p.sendMessage(*TextComponent.fromLegacyText("§c无法创建${name}公会,公会名仅限五个字符哦"))
                    CreateSuccessService.success(false, p)
                }
                return@async
            }
            if(!name.matches("^[\\u4e00-\\u9fa50-9a-zA-Z]*".toRegex())){
                sync {
                    p.sendMessage(*TextComponent.fromLegacyText("§c无法创建${name}公会,公会名含有特殊字符"))
                    CreateSuccessService.success(false, p)
                }
                return@async
            }
            val pi = GuildManager.getMember(p.uniqueId)
            if (pi != null) {
                p.sendSyncMsg("§c无法创建${name}公会,你已经在一个公会里了")
                sync {
                    CreateSuccessService.success(false, p)
                }
                return@async
            }
            val guild = GuildManager.getGuildByName(name)
            if (guild != null) {
                p.sendSyncMsg("§c无法创建${name}公会,已存在相同名称公会")
                sync {
                    CreateSuccessService.success(false, p)
                }
                return@async
            }
            val (id, reason) = GuildSetManager.createGuild(name, p.uniqueId)
            if (id == null) {
                p.sendSyncMsg("§c无法创建${name}公会,$reason")
                sync {
                    CreateSuccessService.success(false, p)
                }
            } else {
                p.sendSyncMsg("§a§l恭喜! ${name} 公会创建成功")
                sync {
                    CreateSuccessService.success(true, p)
                }
            }
        }
    }

    fun createGuild(name: String, from: Player) {
        async {
            val pi = GuildManager.getMember(from.uniqueId)
            if (pi != null) {
                sync {
                    from.sendMessage("§c无法创建${name}公会,你已经在一个公会里了")
                    CreateSuccessService.onReceive(mutableMapOf(
                            "UUID" to from.uniqueId.toString(),
                            "Success" to false
                    ))
                }
                return@async
            }
            val guild = GuildManager.getGuildByName(name)
            if (guild != null) {
                sync {
                    from.sendMessage("§c无法创建${name}公会,已存在相同名称公会")
                    CreateSuccessService.onReceive(mutableMapOf(
                            "UUID" to from.uniqueId.toString(),
                            "Success" to false
                    ))
                }
                return@async
            }
            val data = mutableMapOf<String, Any>()
            data["Name"] = name
            data["Player"] = from.name
            sync {
                this.sendData(data, from)
            }
        }
    }
}