package com.github.bryanser.guildhome.service

import com.github.bryanser.guildhome.Channel
import com.github.bryanser.guildhome.StringManager
import com.github.bryanser.guildhome.bungee.BungeeMain
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class BungeeListener : Listener {
    @EventHandler
    fun onMessage(evt: PluginMessageEvent) {
        if (evt.getTag() == Channel.BUKKIT2BUNGEE) {
            val json = String(evt.data)
            if (Service.DEBUG) {
                BungeeMain.Plugin.logger.info("DEBUG-接收json: $json")
            }
            val data =  Service.authJson(json)
            if(data == null){
                BungeeMain.Plugin.logger.warning("[警告] 有试图传入未经签名的通信信息, from:${evt.sender.address}")
                return
            }
            val service = data["Service"] as String
            val ser = Service.services[service] ?: return
            if (!ser.bukkitSend) {
                throw IllegalStateException("这个数据包只能由Bukkit接收")
            }
            ser.onReceive(data)
            evt.isCancelled = true
        }
    }
}