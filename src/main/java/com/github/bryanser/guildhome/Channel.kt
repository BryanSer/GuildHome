package com.github.bryanser.guildhome

import net.md_5.bungee.api.connection.ProxiedPlayer
import org.bukkit.entity.Player

object Channel {
    const val BUKKIT2BUNGEE = "guildhome:tobungee"
    const val BUNGEE2BUKKIT = "guildhome:tobukkit"

    lateinit var sendProxy: (String, Any) -> Unit

    fun sendMessage(msg: String, p: Player) {
        sendProxy(msg, p)
    }

    fun sendMessage(msg: String, p: ProxiedPlayer) {
        sendProxy(msg, p)
    }

}