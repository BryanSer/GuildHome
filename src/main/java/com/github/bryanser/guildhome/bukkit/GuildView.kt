@file:Suppress("DEPRECATION")

package com.github.bryanser.guildhome.bukkit

import com.comphenix.protocol.utility.StreamSerializer
import com.github.bryanser.brapi.ItemBuilder
import com.github.bryanser.brapi.kview.KView
import com.github.bryanser.brapi.kview.KViewContext
import com.github.bryanser.brapi.kview.KViewHandler
import com.github.bryanser.brapi.kview.builder.KItem
import com.github.bryanser.brapi.kview.builder.KViewBuilder
import com.github.bryanser.guildhome.Guild
import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.Member
import com.github.bryanser.guildhome.bukkit.util.SignUtils
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.service.impl.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

object GuildView {
    const val KICK_NUM = 9
    const val VP_NUM = 1
    const val MANANGER_NUM = 2

    val defaultIcon: ItemStack = ItemBuilder.createItem(Material.DIAMOND) {}
        get() = field.clone()
    val unready: ItemStack = ItemBuilder.createItem(Material.STAINED_GLASS_PANE) {
        name("§cLoading...")
    }
        get() = field.clone()
    val noGuild: ItemStack = ItemBuilder.createItem(Material.STAINED_GLASS_PANE, durability = 4) {
        name("§c§l你不在任何公会哦,快申请加入一个吧")
    }
        get() = field.clone()

    class GuildViewContext(p: Player) : KViewContext("§5§l楼楼公会") {
        @Volatile
        var init: Boolean = false
        @Volatile
        var unfind: Boolean = false
        var ignoreClick: Boolean = true
        lateinit var guild: Guild
        val members = mutableMapOf<Career, MutableList<Member>>()
        lateinit var self: Member
        var page: Int = 0
        val vpSize: Int
            get() = (members[Career.VP]?.size ?: 0) + 1

        var exit = false

        val managerSize: Int
            get() = (members[Career.MANAGER]?.size ?: 0) + vpSize

        var applySize: Int = 0

        fun laterReload(p: Player, later: Long) {
            ignoreClick = true
            init = false
            Bukkit.getScheduler().runTaskLater(BukkitMain.Plugin, { reload(p) }, later)
        }

        fun calcIndex(i: Int): Pair<Int, Career> {
            var index = i + page * 36
            val career = when (i) {
                0 -> Career.PRESIDENT
                in 1 until vpSize -> {
                    index--
                    Career.VP
                }
                in vpSize until managerSize -> {
                    index -= vpSize
                    Career.MANAGER
                }
                else -> {
                    index -= managerSize
                    Career.MEMBER
                }
            }
            return index to career
        }

        fun reload(p: Player) {
            ignoreClick = true
            init = false
            unfind = false
            members.clear()
            Bukkit.getScheduler().runTaskAsynchronously(BukkitMain.Plugin) {
                val ginfo = GuildManager.getMember(p.uniqueId)
                if (ginfo == null) {
                    init = true
                    unfind = true
                    Bukkit.getScheduler().runTask(BukkitMain.Plugin) {
                        KViewHandler.openUI(player, GuildOverview.view)
                    }
                    return@runTaskAsynchronously
                }
                self = ginfo
                guild = GuildManager.getGuild(ginfo.gid)
                        ?: throw IllegalStateException("§4§l找不到公会数据,数据库读取失败")
                for (m in GuildManager.getMembers(ginfo.gid)) {
                    members.getOrPut(m.career) { mutableListOf() }.add(m)
                }
                if (self.career > Career.MEMBER) {
                    applySize = GuildManager.getApplySize(guild.id)
                }
                init = true
                ignoreClick = false
                Bukkit.getScheduler().runTaskLater(BukkitMain.Plugin, {
                    KViewHandler.updateUI(p)
                }, 5)
            }
        }

        init {
            reload(p)
        }
    }

    private inline fun KItem<GuildViewContext>.display(crossinline func: GuildViewContext.() -> ItemStack?) {
        initDisplay {
            if (!init) {
                return@initDisplay unready
            }
            if (unfind) {
                return@initDisplay noGuild
            }
            return@initDisplay func(this)
        }
    }

    val view: KViewBuilder<GuildViewContext> by lazy {
        KViewHandler.createKView("GuildView", 6, ::GuildViewContext) {
            icon(4) {
                display {
                    val icon = loadIcon(guild.icon)
                    ItemBuilder.createItem(icon.type, icon.amount, icon.durability.toInt()) {
                        name("§b§l你的公会: ${guild.name}")
                        lore {
                            +"§a§l公会总贡献值: ${guild.contribution}"
                            +"§6§l公会等级: ${guild.level}"
                            +"§c§l你的职位: ${self.career.display}"
                            +"§e§l公会信息: "
                            for (motd in guild.motd.split("\n")) {
                                +motd
                            }
                            if (self.career >= Career.VP) {
                                +" "
                                +"§3§l=========公会基本管理========="
                                +"§f按shift+右键 §3§l设置手持物品为公会图标"
                                +"§f按shift+左键 §3§l在木牌填写设置公会信息"
                            }
                        }
                    }
                }
                click(ClickType.SHIFT_LEFT) {
                    if (ignoreClick) {
                        return@click
                    }
                    if (self.career < Career.VP) {
                        return@click
                    }
                    val tmp = guild.motd.split("\n")
                    val msg = Array(4) {
                        tmp.getOrElse(it) { "" }
                    }
                    Bukkit.getScheduler().runTask(BukkitMain.Plugin) {
                        player.closeInventory()
                        SignUtils.getSignUtils().sendSignRequest(player, msg) { p, s ->
                            SetGuildMotdService.setMotd(guild.id, Array(4) {
                                ChatColor.translateAlternateColorCodes('&', s.getOrElse(it) { "" })
                            }, p)
                        }
                    }
                }
                click(ClickType.SHIFT_RIGHT) {
                    if (ignoreClick) {
                        return@click
                    }
                    if (self.career < Career.VP) {
                        return@click
                    }
                    val item = player.itemInHand
                    if (item == null || item.type == Material.AIR || item.amount == 0) {
                        player.sendMessage("§c§l你的手上没有任何物品哦")
                        return@click
                    }
                    val icon = saveIcon(item)
                    SetGuildIconService.setGuildIcon(guild.id, icon, player)
                }
            }
            for (i in 0 until 36) {
                icon(i + 9) {
                    display {
                        val (index, career) = calcIndex(i)
                        val m = members[career]?.getOrNull(index) ?: return@display null
                        ItemBuilder.createItem(Material.SKULL_ITEM, durability = 3) {
                            val p = Bukkit.getOfflinePlayer(m.uuid)
                            name("${m.career.display}: ${p.name}")
                            lore {
                                +"§6§l该玩家贡献值: §a§l${m.contribution}"
                                if (self.career > m.career) {
                                    +" "
                                    +"§3§l=========公会基本管理========="
                                    if (m.career < self.career) {
                                        +"§c按下数字键 §6${KICK_NUM} §c踢出成员"
                                    }
                                    if (self.career == Career.PRESIDENT) {
                                        +"§d按下数字键 §6${VP_NUM} §d任命/取消副会长"
                                        +"§d按下数字键 §6${MANANGER_NUM} §d任命/取消管理员"
                                        +"§c§l[只能有两位副会长与五位管理员]"
                                    }
                                    if (self.career == Career.VP) {
                                        +"§d按下数字键 §6${MANANGER_NUM} §d任命/取消管理员"
                                        +"§c§l[只能有五位管理员]"
                                    }
                                }
                            }
                            onBuild {
                                val im = itemMeta as SkullMeta
                                im.owner = p?.name ?: return@onBuild this
                                itemMeta = im
                                this
                            }
                        }
                    }
                    number { number ->
                        val num = number + 1
                        if (ignoreClick) {
                            return@number
                        }
                        if (self.career == Career.MEMBER) {
                            return@number
                        }
                        val (index, career) = calcIndex(i)
                        val m = members[career]?.getOrNull(index) ?: return@number
                        if (m.career == Career.PRESIDENT) {
                            return@number
                        }
                        if (self.career <= m.career) {
                            return@number
                        }
                        val p = Bukkit.getOfflinePlayer(m.uuid) ?: return@number
                        if (self.career == Career.PRESIDENT && num == VP_NUM) {
                            if (m.career == Career.VP) {
                                player.sendMessage("§4§l你正在撤销对方的副会长职位")
                                SetMemberCareerService.setMemberCareer(guild.id, m.uuid, Career.MEMBER, player)
                            } else {
                                val size = this.members[Career.VP]?.size ?: 0
                                if (size >= 2) {
                                    player.sendMessage("§e§l你无法任命更多副会长,公会里已经有两位副会长了")
                                    return@number
                                }
                                player.sendMessage("§4§l你正在任命对方为你公会的副会长")
                                SetMemberCareerService.setMemberCareer(guild.id, m.uuid, Career.VP, player)
                            }
                        } else if (self.career >= Career.VP && num == MANANGER_NUM) {
                            if (m.career == Career.MANAGER) {
                                player.sendMessage("§4§l你正在撤销对方的管理员职位")
                                SetMemberCareerService.setMemberCareer(guild.id, m.uuid, Career.MEMBER, player)
                            }
                            val size = this.members[Career.MANAGER]?.size ?: 0
                            if (size >= 5) {
                                player.sendMessage("§e§l你无法任命更多管理员,公会里已经有五位管理员了")
                                return@number
                            }
                            player.sendMessage("§4§l你正在任命对方为你公会的管理员")
                            SetMemberCareerService.setMemberCareer(guild.id, m.uuid, Career.MANAGER, player)
                        } else if (num == KICK_NUM) {
                            BroadcastMessageService.broadcast(guild.id, player,
                                    "§6========§c[公会公告]§6========",
                                    "§a§l公会的 ${p.name} §b§l已被 ${player.name} §c§l踢出了公会"
                            )
                            KickMemberService.kick(guild.id, m.uuid, player)
                        } else {
                            return@number
                        }
                        laterReload(player, 10)
                    }
                }
            }
            icon(45) {
                val prev = ItemBuilder.createItem(Material.ARROW) {
                    name("§6§l上一页")
                }
                initDisplay {
                    if (page > 0) {
                        prev
                    } else {
                        null
                    }
                }
                click {
                    if (page > 0) {
                        page--
                    }
                }
            }
            icon(53) {
                val next = ItemBuilder.createItem(Material.ARROW) {
                    name("§6§l下一页")
                }
                initDisplay {
                    if (page < 2) {
                        next
                    } else {
                        null
                    }
                }
                click {
                    if (page < 2) {
                        page++
                    }
                }
            }
            icon(52) {
                val dis = ItemBuilder.createItem(Material.OBSIDIAN) {
                    name("§c退出公会")
                }
                val dis2 = ItemBuilder.createItem(Material.OBSIDIAN, amount = 64) {
                    name("§c确认退出公会")
                    lore {
                        +"§6按下数字8键确认退出公会"
                    }
                }
                display {
                    if (self.career == Career.PRESIDENT) {
                        return@display null
                    }
                    if (exit) {
                        dis2
                    } else {
                        dis
                    }
                }
                click {
                    exit = !exit
                }
                number { i ->
                    if (self.career == Career.PRESIDENT) {
                        return@number
                    }
                    if (i == 7) {
                        player.sendMessage("§6成功退出公会")
                        ExitGuildService.exit(player)
                        Bukkit.getScheduler().runTask(BukkitMain.Plugin, player::closeInventory)
                    }
                }
            }
            icon(47) {
                display {
                    if (self.career > Career.MEMBER) {
                        return@display ItemBuilder.createItem(Material.PAPER, if (applySize > 0) {
                            applySize
                        } else {
                            1
                        }) {
                            name("§a§l共有 (${applySize}) 条入会申请待处理")
                            lore("§b§l点击打开申请表操作")
                        }
                    }
                    null
                }
                click {
                    if (ignoreClick) {
                        return@click
                    }
                    if (self.career > Career.MEMBER) {
                        KViewHandler.openUI(player, applyView)
                    }
                }
            }
            icon(49) {
                val display = ItemBuilder.createItem(Material.SIGN) {
                    name("§6点击打开所有公会列表")
                }
                display {
                    display
                }
                click {
                    if (!ignoreClick) {
                        KViewHandler.openUI(player, GuildOverview.view)
                    }
                }
            }

        }
    }

    class GuildApplyContext(p: Player) : KViewContext("§6§l公会申请列表") {
        @Volatile
        var init: Boolean = false
        val apply = mutableListOf<Pair<UUID, Long>>()
        var page: Int = 0
        var ignoreClick = true
        lateinit var guild: Guild
        lateinit var self: Member
        fun laterReload(p: Player, later: Long) {
            ignoreClick = true
            init = false
            Bukkit.getScheduler().runTaskLater(BukkitMain.Plugin, { reload(p) }, later)
        }

        init {
            reload(p)
        }

        fun reload(p: Player) {
            ignoreClick = true
            init = false
            apply.clear()
            Bukkit.getScheduler().runTaskAsynchronously(BukkitMain.Plugin) {
                val ginfo = GuildManager.getMember(p.uniqueId)
                if (ginfo == null) {
                    Bukkit.getScheduler().runTask(BukkitMain.Plugin) {
                        p.closeInventory()
                    }
                    return@runTaskAsynchronously
                }
                self = ginfo
                guild = GuildManager.getGuild(ginfo.gid)
                        ?: throw IllegalStateException("§6§l找不到公会数据,数据库读取失败")
                apply.addAll(GuildManager.getApplys(guild.id))
                init = true
                Bukkit.getScheduler().runTaskLater(BukkitMain.Plugin, {
                    KViewHandler.updateUI(p)
                }, 5)
                ignoreClick = false
            }
        }
    }

    private inline fun KItem<GuildApplyContext>.display2(crossinline func: GuildApplyContext.() -> ItemStack?) {
        initDisplay {
            if (!init) {
                return@initDisplay unready
            }
            return@initDisplay func(this)
        }
    }

    val applyView: KView<GuildApplyContext> by lazy {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        KViewHandler.createKView("GuildApplyView", 6, ::GuildApplyContext) {
            for (i in 0 until 45) {
                icon(i) {
                    display2 {
                        val (uuid, time) = apply.getOrNull(i + page * 45) ?: return@display2 null
                        val p = Bukkit.getOfflinePlayer(uuid)
                        ItemBuilder.createItem(Material.SKULL_ITEM, durability = 3) {
                            name("§a§l玩家: ${p?.name ?: "§6§l找不到名字"}")
                            lore {
                                +"§a申请日期: ${dateFormat.format(Date(time))}"
                                +"  "
                                +"§b§l鼠标左键同意请求"
                                +"§c§l鼠标右键拒绝请求"
                            }
                            onBuild {
                                val im = itemMeta as SkullMeta
                                im.owner = p.name ?: return@onBuild this
                                itemMeta = im
                                this
                            }
                        }
                    }
                    click(ClickType.LEFT) {
                        if (ignoreClick) {
                            return@click
                        }
                        val (uuid, _) = apply.getOrNull(i + page * 45) ?: return@click
                        ApplyMemberService.acceptApply(guild.id, uuid, player, true)
                        laterReload(player, 20)
                    }
                    click(ClickType.RIGHT) {
                        if (ignoreClick) {
                            return@click
                        }
                        val (uuid, _) = apply.getOrNull(i + page * 45) ?: return@click
                        ApplyMemberService.acceptApply(guild.id, uuid, player, false)
                        laterReload(player, 20)
                    }
                }
            }
            icon(45) {
                val prev = ItemBuilder.createItem(Material.ARROW) {
                    name("§6§l上一页")
                }
                initDisplay {
                    if (page > 0) {
                        prev
                    } else {
                        null
                    }
                }
                click {
                    if (page > 0) {
                        page--
                    }
                }
            }
            icon(49) {
                initDisplay(ItemBuilder.createItem(Material.DIAMOND) { name("§r[§b§l点击返回公会界面§r]") })
                click {
                    KViewHandler.openUI(player, view)
                }
            }
            icon(53) {
                val next = ItemBuilder.createItem(Material.ARROW) {
                    name("§6§l下一页")
                }
                initDisplay {
                    if (page < 2) {
                        next
                    } else {
                        null
                    }
                }
                click {
                    if (page < 2) {
                        page++
                    }
                }
            }
        }
    }


    @Suppress("NAME_SHADOWING")
    fun saveIcon(item: ItemStack): String {
        val item = ItemStack(item.type, item.amount, item.durability)
        return StreamSerializer.getDefault().serializeItemStack(item)
    }

    fun loadIcon(str: String?): ItemStack {
        val item = StreamSerializer.getDefault().deserializeItemStack(str ?: return defaultIcon)
        return item
    }
}
