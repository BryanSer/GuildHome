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
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

object GuildView {
    val defaultIcon: ItemStack = ItemBuilder.createItem(Material.DIAMOND) {}
        get() = field.clone()
    val unready: ItemStack = ItemBuilder.createItem(Material.REDSTONE_BLOCK) {
        name("§cLoading...")
    }
        get() = field.clone()
    val noGuild: ItemStack = ItemBuilder.createItem(Material.GLASS) {
        name("§c你还没有属于任何公会")
    }
        get() = field.clone()

    class GuildViewContext(p: Player) : KViewContext("§6公会菜单") {
        @Volatile
        var init: Boolean = false
        @Volatile
        var unfind: Boolean = false
        var ignoreClick: Boolean = true
        lateinit var guild: Guild
        val members = mutableMapOf<Career, MutableList<Member>>()
        lateinit var self: Member
        var page: Int = 0
        val vpSize: Int by lazy {
            (members[Career.VP]?.size ?: 0) + 1
        }
        val managerSize: Int by lazy {
            (members[Career.MANAGER]?.size ?: 0) + vpSize
        }
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
                        KViewHandler.updateUI(p)
                    }
                    return@runTaskAsynchronously
                }
                self = ginfo
                guild = GuildManager.getGuild(ginfo.gid)
                        ?: throw IllegalStateException("找不到公会数据 数据库约束失败")
                for (m in GuildManager.getMembers(ginfo.gid)) {
                    members.getOrPut(m.career) { mutableListOf() }.add(m)
                }
                if (self.career > Career.MEMBER) {
                    applySize = GuildManager.getApplySize(guild.id)
                }
                init = true
                Bukkit.getScheduler().runTask(BukkitMain.Plugin) {
                    KViewHandler.updateUI(p)
                }
                ignoreClick = false
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
            icon(6) {
                display {
                    val icon = loadIcon(guild.icon)
                    ItemBuilder.createItem(icon.type, icon.amount, icon.durability.toInt()) {
                        name("§b§l公会: ${guild.name}")
                        lore {
                            +"§a公会总贡献值: ${guild.contribution}"
                            +"§6公会等级: ${guild.level}"
                            +"§c职位: ${self.career.display}"
                            +"§a公会信息: "
                            for (motd in guild.motd.split("\n")) {
                                +motd
                            }
                            if (self.career >= Career.MANAGER) {
                                +" "
                                +"§4§l===管理==="
                                +"§4shift+右键设定公会图标为手上物品"
                                +"§4shift+左键设定公会信息"
                            }
                        }
                    }
                }
                click(ClickType.SHIFT_LEFT) {
                    if (ignoreClick) {
                        return@click
                    }
                    if (self.career < Career.MANAGER) {
                        return@click
                    }
                    val tmp = guild.motd.split("\n")
                    val msg = Array(4) {
                        tmp.getOrElse(it) { "" }
                    }
                    Bukkit.getScheduler().runTask(BukkitMain.Plugin) {
                        player.closeInventory()
                        SignUtils.getSignUtils().sendSignRequest(player, msg) { p, s ->
                            SetGuildMotdService.setMotd(guild.id, Array(4) { s.getOrElse(it) { "" } }, p)
                        }
                    }
                }
                click(ClickType.SHIFT_RIGHT) {
                    if (ignoreClick) {
                        return@click
                    }
                    if (self.career < Career.MANAGER) {
                        return@click
                    }
                    val item = player.itemInHand
                    if (item == null || item.type == Material.AIR || item.amount == 0) {
                        player.sendMessage("§c你的手上毛都没有")
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
                            if (p == null) {
                                name("找不到头像: ${m.uuid}")
                                return@createItem
                            }
                            name("${m.career.display}: ${p.name}")
                            lore {
                                +"§6贡献值: ${m.contribution}"
                                if (self.career >= Career.MANAGER) {
                                    +" "
                                    +"§4§l===管理==="
                                    if (m.career == Career.MEMBER) {
                                        +"§c数字1键踢出成员"
                                    }
                                    if (self.career == Career.PRESIDENT) {
                                        +"§c§l数字2键任命或取消副会长"
                                        +"§c§l数字3键任命或取消管理员"
                                        +"§4注意 只能有两位副会长与五位管理员"
                                    }
                                    if (self.career == Career.VP) {
                                        +"§c§l数字3键任命或取消管理员"
                                        +"§4注意 只能有五位管理员"
                                    }
                                }
                            }
                            onBuild {
                                val im = itemMeta as SkullMeta
                                im.owningPlayer = p
                                itemMeta = im
                                this
                            }
                        }
                    }
                    number { number ->
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
                        if (self.career == Career.PRESIDENT && number == 1) {
                            if (m.career == Career.VP) {
                                player.sendMessage("§6正在撤销对方的副会长职位")
                                SetMemberCareerService.setMemberCareer(guild.id, m.uuid, Career.MEMBER, player)
                            } else {
                                val size = this.members[Career.VP]?.size ?: 0
                                if (size >= 2) {
                                    player.sendMessage("§c无法任命, 公会里已经有两位副会长了")
                                    return@number
                                }
                                player.sendMessage("§6正在任命对方的副会长职位")
                                SetMemberCareerService.setMemberCareer(guild.id, m.uuid, Career.VP, player)
                            }
                        } else if (self.career >= Career.VP && number == 2) {
                            if (m.career == Career.MANAGER) {
                                player.sendMessage("§6正在撤销对方的管理员职位")
                                SetMemberCareerService.setMemberCareer(guild.id, m.uuid, Career.MEMBER, player)
                            }
                            val size = this.members[Career.MANAGER]?.size ?: 0
                            if (size >= 5) {
                                player.sendMessage("§c无法任命, 公会里已经有五位管理员了")
                                return@number
                            }
                            player.sendMessage("§6正在任命对方的管理员职位")
                            SetMemberCareerService.setMemberCareer(guild.id, m.uuid, Career.MANAGER, player)
                        } else if (number == 0) {
                            BroadcastMessageService.broadcast(guild.id, player,
                                    "§6=======§c[公会公告]§6=======",
                                    "§a玩家${p.name}已被${player.name}踢出了公会"
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
                    name("§6上一页")
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
                    name("§6下一页")
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
            icon(47) {
                display {
                    if (self.career > Career.MEMBER) {
                        return@display ItemBuilder.createItem(Material.PAPER, if (applySize > 0) {
                            applySize
                        } else {
                            1
                        }) {
                            name("§6等待受理的申请: #(${applySize})")
                            lore("§b点击打开")
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

        }
    }

    class GuildApplyContext(p: Player) : KViewContext("§b申请列表") {
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
                        ?: throw IllegalStateException("找不到公会数据 数据库约束失败")
                apply.addAll(GuildManager.getApplys(guild.id))
                init = true
                Bukkit.getScheduler().runTask(BukkitMain.Plugin) {
                    KViewHandler.updateUI(p)
                }
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
                                ?: return@display2 ItemBuilder.createItem(Material.SKULL_ITEM) {
                                    name("找不到头像")
                                }
                        ItemBuilder.createItem(Material.SKULL_ITEM, durability = 3) {
                            name("§a玩家: ${p.name}")
                            lore {
                                +"§a申请日期: ${dateFormat.format(Date(time))}"
                                +"  "
                                +"§b左键点击同意请求"
                                +"§c右键点击拒绝请求"
                            }
                            onBuild {
                                val im = itemMeta as SkullMeta
                                im.owningPlayer = p
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
                    }
                    click(ClickType.RIGHT) {
                        if (ignoreClick) {
                            return@click
                        }
                        val (uuid, _) = apply.getOrNull(i + page * 45) ?: return@click
                        ApplyMemberService.acceptApply(guild.id, uuid, player, true)
                    }
                }
            }
            icon(45) {
                val prev = ItemBuilder.createItem(Material.ARROW) {
                    name("§6上一页")
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
                    name("§6下一页")
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
