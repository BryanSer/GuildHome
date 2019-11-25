@file:Suppress("DEPRECATION")

package com.github.bryanser.guildhome.bungee

import com.github.bryanser.guildhome.Channel
import com.github.bryanser.guildhome.Guild
import com.github.bryanser.guildhome.database.DatabaseHandler
import com.github.bryanser.guildhome.service.BungeeListener
import com.zaxxer.hikari.HikariConfig
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.api.scheduler.GroupedThreadFactory
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.*

class BungeeMain : Plugin() {

    override fun onEnable() {
        Plugin = this
        connectSQL()
        register()
    }

    fun register() {
        this.proxy.registerChannel(Channel.BUKKIT2BUNGEE)
        this.proxy.registerChannel(Channel.BUNGEE2BUKKIT)
        Channel.sendProxy = { s, p ->
            (p as ProxiedPlayer).server.sendData(Channel.BUNGEE2BUKKIT, s.toByteArray())
        }
        this.proxy.pluginManager.registerListener(this, BungeeListener())
    }

    fun setLevelMaxMember(config: Configuration) {
        val lvs = config.getSection("Level")
        for (key in lvs.keys) {
            val data = lvs.getInt("$key.MaxMember")
            Guild.maxMember[key.toInt()] = data
        }
    }

    fun connectSQL() {
        val f = File(this.dataFolder, "config.yml")
        if (!f.exists()) {
            saveResource(this, "config.yml", f)
        }
        val cfgpro = ConfigurationProvider.getProvider(YamlConfiguration::class.java)
        val cfg = cfgpro.load(f)
        setLevelMaxMember(cfg)
        val db = cfg.getSection("Mysql")
        val sb = StringBuilder(String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s",
                db.getString("host"),
                db.getInt("port"),
                db.getString("database"),
                db.getString("user"),
                db.getString("password")
        ))
        for (s in db.getStringList("options")) {
            sb.append('&')
            sb.append(s)
        }
        val config = HikariConfig()
        config.jdbcUrl = sb.toString()
        try {
            config.threadFactory = GroupedThreadFactory(this, "Hikari Thread Factory")
        } catch (t: Throwable) {
            val con = GroupedThreadFactory::class.java.getConstructor(net.md_5.bungee.api.plugin.Plugin::class.java)
            val gtf = con.newInstance(this)
            config.threadFactory = gtf
        }

        DatabaseHandler.init(config)
    }

    fun saveResource(plugin: Plugin, path: String, file: File) {
        file.createNewFile()
        val data = plugin.getResourceAsStream(path)
        val writer = BufferedWriter(FileWriter(file))
        val reader = BufferedReader(InputStreamReader(data))
        var read: String? = reader.readLine()
        while (read != null) {
            writer.write(read)
            writer.write("\n")
            read = reader.readLine()
        }
        writer.flush()
        writer.close()
        reader.close()
        data.close()
    }

    companion object {
        lateinit var Plugin: BungeeMain
    }
}