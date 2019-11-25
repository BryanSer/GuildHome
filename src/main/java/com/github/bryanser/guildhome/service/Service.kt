package com.github.bryanser.guildhome.service

import com.github.bryanser.guildhome.Channel
import com.github.bryanser.guildhome.StringManager
import com.github.bryanser.guildhome.bukkit.BukkitMain
import com.github.bryanser.guildhome.bungee.BungeeMain
import com.github.bryanser.guildhome.service.impl.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import org.bukkit.Bukkit
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import javax.naming.RefAddr

abstract class Service(
        val name: String,
        val bukkitSend: Boolean
) {

    abstract fun onReceive(data: Map<String, Any>)

    open fun sendData(data: MutableMap<String, Any>, p: Any) {
        if (bukkitSend && !bukkit) {
            throw IllegalStateException("这个数据不应该由Bukkit发送")
        }
//        data["SendID"] = UUID.randomUUID().toString()
        data["Service"] = name
        val realData = mutableMapOf<String, Any>()
        val sign = sign(data.toString())
        realData["data"] = data
        realData["sign"] = sign
        val result = StringManager.toJson(realData)
        if (DEBUG) {
            if (bukkit) {
                Bukkit.getLogger().info("DEBUG-发送json: $result")
                Bukkit.getLogger().info("DEBUG-发送服务名: $name")
            } else {
                BungeeMain.Plugin.proxy.logger.info("DEBUG-发送json: $result")
                BungeeMain.Plugin.proxy.logger.info("DEBUG-发送服务名: $name")
            }
        }
        Channel.sendProxy(result, p)
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
            services[CreateSuccessService.name] = CreateSuccessService
            services[DisbandGuildService.name] = DisbandGuildService
            services[DonateService.name] = DonateService
        }

        fun sign(json: String): String {
            return json.hashSHA256(SALT)
        }

        fun authJson(json: String): Map<String, Any>? {


            return null
        }

        const val SALT = "GN2PDN201B0HC21OIHE"

        val instance = MessageDigest.getInstance("SHA-256")
        fun String.hashSHA256(salt: String): String {
            val ba = instance.digest("$this-guildhome-$salt".toByteArray())
            return byteArrayToHexString(ba)
        }

        private val hexDigIts = "0123456789ABCDEF".toCharArray()
        fun byteArrayToHexString(b: ByteArray): String {
            val resultSb = StringBuffer()
            for (i in b.indices) {
                resultSb.append(byteToHexString(b[i]))
            }
            return resultSb.toString()
        }

        fun byteToHexString(b: Byte): String {
            var n = b.toInt()
            if (n < 0) {
                n += 256
            }
            val d1 = n / 16
            val d2 = n % 16
            return hexDigIts[d1] + "" + hexDigIts[d2]
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

