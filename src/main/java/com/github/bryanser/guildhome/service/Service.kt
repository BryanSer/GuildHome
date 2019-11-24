package com.github.bryanser.guildhome.service

import com.github.bryanser.guildhome.Channel
import com.github.bryanser.guildhome.StringManager
import com.github.bryanser.guildhome.bukkit.BukkitMain
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.service.impl.*
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.TimeUnit

abstract class Service(
        val name: String,
        val bukkitSend: Boolean
) {

    abstract fun onReceive(data: Map<String, Any>)

    open fun sendData(data: MutableMap<String, Any>, p: Any) {
        if (bukkitSend && !bukkit) {
            throw IllegalStateException("这个数据不应该由Bukkit发送")
        }
        data["SendID"] = UUID.randomUUID().toString()
        data["Service"] = name
        val json = StringManager.toJson(data)
        if (DEBUG) {
            Bukkit.getLogger().info("DEBUG-发送json: $json")
            Bukkit.getLogger().info("DEBUG-发送服务名: $name")
        }
        Channel.sendProxy(json, p)
    }

    companion object {
        const val DEBUG = true
        val services = mutableMapOf<String, Service>()
        var bukkit: Boolean = false

        init {
            services[ApplyMemberService.name] = ApplyMemberService
            services[BroadcastMessageService.name] = BroadcastMessageService
            services[CreateGuildService.name] = CreateGuildService
            services[KickMemberService.name] = KickMemberService
            services[SetGuildIconService.name] = SetGuildIconService
            services[SetGuildMotdService.name] = SetGuildMotdService
            services[SetMemberCareerService.name] = SetMemberCareerService
            services[ApplyService.name] = ApplyService
            services[ExitGuildService.name] = ExitGuildService
        }

        fun async(func: () -> Unit) {
            if (bukkit) {
                Bukkit.getScheduler().runTaskAsynchronously(BukkitMain.Plugin, func)
            } else {
                BungeeMain.Plugin.proxy.scheduler.runAsync(BungeeMain.Plugin, func)
            }
        }

        fun sync(func: () -> Unit) {
            if (bukkit) {
                Bukkit.getScheduler().runTask(BukkitMain.Plugin, func)
            } else {
                BungeeMain.Plugin.proxy.scheduler.schedule(BungeeMain.Plugin, func, 1, TimeUnit.MILLISECONDS)
            }
        }

        fun String.sendMsg(msg: String) {
            if (bukkit) {
                Bukkit.getPlayer(this)?.sendMessage(msg)
            } else {
                BungeeMain.Plugin.proxy.getPlayer(this)?.sendSyncMsg(msg)
            }
        }

        fun UUID.sendMsg(msg: String) {
            if (bukkit) {
                Bukkit.getPlayer(this)?.sendMessage(msg)
            } else {
                BungeeMain.Plugin.proxy.getPlayer(this)?.sendSyncMsg(msg)
            }
        }

        fun String.toUUID(): UUID {
            return UUID.fromString(this)
        }

        inline fun ProxiedPlayer.sendSyncMsg(vararg msg: String) {
            sync {
                for (s in msg)
                    this.sendMessage(*TextComponent.fromLegacyText(s))
            }
        }

        inline fun Any?.asInt(): Int {
            return (this as Number).toInt()
        }
    }
}

