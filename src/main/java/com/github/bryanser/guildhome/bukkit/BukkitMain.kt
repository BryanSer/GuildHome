package com.github.bryanser.guildhome.bukkit

import Br.API.CallBack
import com.github.bryanser.brapi.kview.KViewHandler
import com.github.bryanser.guildhome.Channel
import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.DatabaseHandler
import com.github.bryanser.guildhome.service.BukkitListener
import com.github.bryanser.guildhome.service.Service
import com.github.bryanser.guildhome.service.impl.ApplyMemberService
import com.github.bryanser.guildhome.service.impl.ApplyService
import com.github.bryanser.guildhome.service.impl.CreateGuildService
import com.zaxxer.hikari.HikariConfig
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class BukkitMain : JavaPlugin() {

    override fun onEnable() {
        Plugin = this
        Service.bukkit = true
        connectSQL()
        register()
    }

    fun register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, Channel.BUKKIT2BUNGEE)
        Bukkit.getMessenger().registerIncomingPluginChannel(this, Channel.BUNGEE2BUKKIT, BukkitListener())
        Channel.sendProxy = { it, p ->
            (p as Player).sendPluginMessage(this, Channel.BUKKIT2BUNGEE, it.toByteArray())
        }
    }

    fun connectSQL() {
        val f = File(this.dataFolder, "config.yml")
        if (!f.exists()) {
            this.saveDefaultConfig()
        }
        val cfg = YamlConfiguration.loadConfiguration(f)
        val db = cfg.getConfigurationSection("Mysql")
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
        DatabaseHandler.init(config)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return false
        }
        if (args.isEmpty()) {
            KViewHandler.openUI(sender, GuildOverview.view)
            return true
        }
        if (args[0].equals("create", true)) {
            val name = args[1]
            CreateGuildService.createGuild(name, sender)
            return true
        }
        if (args[0].equals("apply", true)) {
            val name = args[1]
            ApplyService.apply(name, sender)
            return true
        }
        if (args[0].equals("invite", true)) {
            val target = Bukkit.getPlayerExact(args[1])
            if (target == null || !target.isOnline) {
                sender.sendMessage("§c找不到玩家${args[1]}")
                return true
            }
            if(target == sender){
                sender.sendMessage("§c你不能邀请你自己")
                return true
            }
            sender.sendMessage("§6邀请已发送")
            Bukkit.getScheduler().runTaskAsynchronously(this) {
                val member = GuildManager.getMember(sender.uniqueId)
                if (member == null) {
                    Bukkit.getScheduler().runTask(this) {
                        sender.sendMessage("§c你还没有在任何一个公会里 无法邀请对方")
                    }
                    return@runTaskAsynchronously
                }
                val guild = GuildManager.getGuild(member.gid) ?: return@runTaskAsynchronously
                Bukkit.getScheduler().runTask(this) {
                    target.sendMessage("§6玩家${sender.name}邀请你加入他的公会${guild.name}")
                    CallBack.sendButtonRequest(target, arrayOf("§a==同意==", "§c==拒绝=="), { p, i ->
                        if (i == 0) {
                            p.sendMessage("§6正在同意来自${target.name}的公会邀请")
                            sender.sendMessage("§e§l${target.name}同意了你的公会邀请")
                            if (member.career >= Career.MANAGER) {
                                ApplyMemberService.acceptApply(guild.id, target.uniqueId, sender, true)
                            } else {
                                p.chat("/GuildHome apply ${guild.name}")
                            }
                        } else {
                            p.sendMessage("§6已成功拒绝来自${target.name}的公会邀请")
                            sender.sendMessage("§c${target.name}拒绝了你的公会邀请")
                        }
                    }, 30)
                }
            }
            return true
        }
        return false
    }

    companion object {
        lateinit var Plugin: BukkitMain
    }
}
