package com.github.bryanser.guildhome.bukkit

import Br.API.CallBack
import com.github.bryanser.brapi.Utils
import com.github.bryanser.brapi.kview.KViewHandler
import com.github.bryanser.guildhome.*
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.DatabaseHandler
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
    }

    fun register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, Channel.BUKKIT2BUNGEE)
        Bukkit.getMessenger().registerIncomingPluginChannel(this, Channel.BUNGEE2BUKKIT, BukkitListener())
        Channel.sendProxy = { it, p ->
            (p as Player).sendPluginMessage(this, Channel.BUKKIT2BUNGEE, it.toByteArray())
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            object : EZPlaceholderHook(this, "guildhome") {
                val cache = ConcurrentHashMap<UUID, IMember>()
                val guilds = ConcurrentHashMap<Int, GuildInfo>()
                val players = CopyOnWriteArrayList<UUID>()

                init {
                    Bukkit.getScheduler().runTaskTimer(this@BukkitMain, {
                        val list = Bukkit.getOnlinePlayers().map(Player::getUniqueId).toList()
                        players.clear()
                        players.addAll(list)
                    }, 300, 300)
                    Bukkit.getScheduler().runTaskTimerAsynchronously(this@BukkitMain, {
                        for (name in players) {
                            val m = GuildManager.getMember(name) ?: NullMember()
                            cache[name] = m
                        }
                    }, 600, 600)
                    Bukkit.getScheduler().runTaskTimerAsynchronously(this@BukkitMain, {
                        for (g in GuildManager.getAllGuild()) {
                            guilds[g.gid] = g
                        }
                    }, 700, 700)
                }

                override fun onPlaceholderRequest(p: Player, params: String): String? {
                    val member = cache[p.uniqueId] as? Member ?: return "无所属公会"
                    val guild = guilds[member.gid] ?: return "数据读取中"
                    return when (params) {
                        "name" -> {
                            guild.name
                        }
                        "president" -> {
                            Bukkit.getOfflinePlayer(UUID.fromString(guild.president))?.name
                                    ?: ""
                        }
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
    }

    var create_cost: Double = 1000.0

    fun connectSQL() {
        val f = File(this.dataFolder, "config.yml")
        if (!f.exists()) {
            this.saveDefaultConfig()
        }
        val cfg = YamlConfiguration.loadConfiguration(f)
        create_cost = cfg.getDouble("Cost.Create", 1000.0)
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
        if(args[0].equals("disband",true)){
            DisbandGuildService.disband(sender)
            return true
        }
        if (args[0].equals("create", true)) {
            val has = Utils.economy!!.getBalance(sender)
            if (has < create_cost) {
                sender.sendMessage("§c你没有足够的节操来创建公会, 至少需要${create_cost}")
                return true
            }
            sender.sendMessage("§6正在预扣除节操来创建公会 在完成创建前请勿离开服务器")
            Utils.economy!!.withdrawPlayer(sender, create_cost)
            val name = args[1]
            CreateGuildService.createGuild(name, sender)
            return true
        }
        if (args[0].equals("apply", true)) {
            val name = args[1]
            ApplyService.apply(name, sender)
            return true
        }
        if (args[0].equals("inv", true)) {
            val target = Bukkit.getPlayerExact(args[1])
            if (target == null || !target.isOnline) {
                sender.sendMessage("§c找不到玩家${args[1]}")
                return true
            }
            if (target == sender) {
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
