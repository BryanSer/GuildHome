package com.github.bryanser.guildhome.service

import com.github.bryanser.guildhome.Channel
import com.github.bryanser.guildhome.StringManager
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener


class BukkitListener : PluginMessageListener {
    override fun onPluginMessageReceived(channel: String, player: Player?, message: ByteArray) {
        if(channel == Channel.BUNGEE2BUKKIT){
            val json = StringManager.fromJson(String(message))
            val service = json["Service"] as String
            val ser = Service.services[service] ?: return
            if(ser.bukkitSend){
                throw IllegalStateException("这个数据包只能由Bungee接收")
            }
            ser.onReceive(json)
        }
    }
}