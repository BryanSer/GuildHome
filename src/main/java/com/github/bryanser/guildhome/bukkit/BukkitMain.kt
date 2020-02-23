package com.github.bryanser.guildhome.bukkit

import Br.API.CallBack
import com.github.bryanser.brapi.Utils
import com.github.bryanser.brapi.kview.KViewHandler
import com.github.bryanser.guildhome.*
import com.github.bryanser.guildhome.bukkit.shop.*
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.DatabaseHandler
import com.github.bryanser.guildhome.database.UserName
import com.github.bryanser.guildhome.service.BukkitListener
import com.github.bryanser.guildhome.service.Service
import com.github.bryanser.guildhome.service.impl.ApplyMemberService
import com.github.bryanser.guildhome.service.impl.ApplyService
import com.github.bryanser.guildhome.service.impl.CreateGuildService
import com.github.bryanser.guildhome.service.impl.DisbandGuildService
import com.zaxxer.hikari.HikariConfig
import me.clip.placeholderapi.external.EZPlaceholderHook
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class BukkitMain : JavaPlugin() {

    override fun onEnable() {
        Plugin = this
        Service.bukkit = true
        connectSQL()
        register()
        GuildConfig.init()
        Bukkit.getPluginManager().registerEvents(GuildConfig, this)
        ShopViewContext.loadShop()
        Loot.load()
        Exp.load()
        WeLore.load()
        Buff.load()
    }

    fun register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, Channel.BUKKIT2BUNGEE)
        Bukkit.getMessenger().registerIncomingPluginChannel(this, Channel.BUNGEE2BUKKIT, BukkitListener())
        Channel.sendProxy = { it, p ->
            (p as Player).sendPluginMessage(this, Channel.BUKKIT2BUNGEE, it.toByteArray())
        }
        object : EZPlaceholderHook(this, "guildhome") {
            override fun onPlaceholderRequest(p: Player, params: String): String? {
                val member = GuildConfig.cache[p.uniqueId] as? Member ?: return ""
                val guild = GuildConfig.guilds[member.gid] ?: return ""
                return when (params) {
                    "name" -> {
                        guild.name
                    }
                    "president" -> {
                        guild.realName
                                ?: ""
                    }
                    "level" -> "${guild.level}"
                    "size" -> "${guild.memberSize}"
                    "score" -> "${guild.score}"
                    "contribution" -> "${guild.contribution}"
                    "selfcontribution" -> "${member.contribution}"
                    "career" -> member.career.display
                    "motd_0" -> {
                        val motd = guild.motd.split("\n")
                        motd.getOrElse(0) { "" }
                    }
                    "motd_1" -> {
                        val motd = guild.motd.split("\n")
                        motd.getOrElse(1) { "" }
                    }
                    "motd_2" -> {
                        val motd = guild.motd.split("\n")
                        motd.getOrElse(2) { "" }
                    }
                    "motd_3" -> {
                        val motd = guild.motd.split("\n")
                        motd.getOrElse(3) { "" }
                    }
                    else -> ""
                }
            }
        }.hook()

    }

    fun connectSQL() {
        val f = File(this.dataFolder, "config.yml")
        if (!f.exists()) {
            this.saveDefaultConfig()
        }
        val cfg = YamlConfiguration.loadConfiguration(f)
        GuildConfig.loadConfig(cfg)
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
        Loot.save()
        Exp.save()
        WeLore.save()
        Buff.save()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return false
        }
        if (args.isEmpty()) {
            KViewHandler.openUI(sender, GuildOverview.view)
            return true
        }
        if (args[0].equals("reload", true) && sender.isOp) {
            val f = File(this.dataFolder, "config.yml")
            if (!f.exists()) {
                this.saveDefaultConfig()
            }
            val cfg = YamlConfiguration.loadConfiguration(f)
            GuildConfig.loadConfig(cfg)
            ShopViewContext.loadShop()
            sender.sendMessage("§6重载成功")
            return true
        }
        if (args[0].equals("disband", true)) {
            DisbandGuildService.disband(sender)
            return true
        }
        if (args[0].equals("create", true) && args.size > 1) {
            val has = Utils.economy!!.getBalance(sender)
            if (has < GuildConfig.create_cost) {
                sender.sendMessage("§c§l你没有足够的节操来创建公会,需要${GuildConfig.create_cost}节操才可创建")
                return true
            }
            sender.sendMessage("§6§l正在预扣除节操来创建公会,在完成创建前请勿离开服务器")
            Utils.economy!!.withdrawPlayer(sender, GuildConfig.create_cost)
            val name = args[1]
            CreateGuildService.createGuild(name, sender)
            return true
        }
        if (args[0].equals("apply", true) && args.size > 1) {
            val name = args[1]
            ApplyService.apply(name, sender)
            return true
        }
        if (args[0].equals("inv", true) && args.size > 1) {
            val target = Bukkit.getPlayerExact(args[1])
            if (target == null || !target.isOnline) {
                sender.sendMessage("§c找不到在线玩家 §a${args[1]} §b请确认名称是否正确或在线")
                return true
            }
            if (target == sender) {
                sender.sendMessage("§a你不能邀请你自己哦")
                return true
            }
            sender.sendMessage("§6§l邀请已发送给该玩家")
            Bukkit.getScheduler().runTaskAsynchronously(this) {
                val member = GuildManager.getMember(sender.uniqueId)
                if (member == null) {
                    Bukkit.getScheduler().runTask(this) {
                        sender.sendMessage("§b§l你还没有在任何一个公会里,无法邀请对方加入")
                    }
                    return@runTaskAsynchronously
                }
                val guild = GuildManager.getGuild(member.gid) ?: return@runTaskAsynchronously
                Bukkit.getScheduler().runTask(this) {
                    target.sendMessage("§6§l玩家 §b§l${sender.name} §6§l邀请你加入他的公会 §e§l${guild.name}")
                    CallBack.sendButtonRequest(target, arrayOf("§a§l[点我同意]", "§c§l[点我拒绝]"), { p, i ->
                        if (i == 0) {
                            p.sendMessage("§e§l正在同意来自 §a§l${target.name} §e§l的公会邀请")
                            sender.sendMessage("§e§l${target.name} §a§l同意了你的公会邀请")
                            if (member.career >= Career.MANAGER) {
                                ApplyMemberService.acceptApply(guild.id, target.uniqueId, sender, true)
                            } else {
                                p.chat("/GuildHome apply ${guild.name}")
                            }
                        } else {
                            p.sendMessage("§c§l已成功拒绝来自 §b§l${target.name} §c§l的公会邀请")
                            sender.sendMessage("§c§l${target.name} §e§l拒绝了你的公会邀请")
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
