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
        if(evt.getTag() == Channel.BUKKIT2BUNGEE){
            val json = StringManager.fromJson(String(evt.data))
            if(Service.DEBUG){
                BungeeMain.Plugin.logger.info("DEBUG-接收json: $json")
            }
            val service = json["Service"] as String
            val ser = Service.services[service] ?: return
            if(!ser.bukkitSend){
                throw IllegalStateException("这个数据包只能由Bukkit接收")
            }
            ser.onReceive(json)
            evt.isCancelled = true
        }
    }
}