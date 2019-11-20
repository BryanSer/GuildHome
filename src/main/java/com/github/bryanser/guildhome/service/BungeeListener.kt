package com.github.bryanser.guildhome.service

import com.github.bryanser.guildhome.Channel
import com.github.bryanser.guildhome.StringManager
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class BungeeListener : Listener {
    @EventHandler
    fun onMessage(evt: PluginMessageEvent) {
        if(evt.getTag() == Channel.BUKKIT2BUNGEE){
            val json = StringManager.fromJson(String(evt.data))
            val service = json["Service"] as String
            val ser = Service.services[service] ?: return
            ser.onReceive(json)
        }
    }
}