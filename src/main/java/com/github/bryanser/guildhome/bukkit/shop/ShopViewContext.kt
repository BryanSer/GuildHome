package com.github.bryanser.guildhome.bukkit.shop

import com.github.bryanser.brapi.ItemBuilder
import com.github.bryanser.brapi.Utils
import com.github.bryanser.brapi.kview.KView
import com.github.bryanser.brapi.kview.KViewContext
import com.github.bryanser.brapi.kview.KViewHandler
import com.github.bryanser.brapi.kview.builder.KItem
import com.github.bryanser.brapi.kview.builder.KViewMaker
import com.github.bryanser.guildhome.Guild
import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.Member
import com.github.bryanser.guildhome.bukkit.BukkitMain
import com.github.bryanser.guildhome.bukkit.GuildView
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.lang.IllegalStateException

class ShopViewContext(p: Player) : KViewContext(title) {
    @Volatile
    var init = false
    lateinit var guild: Guild
    lateinit var self: Member

    fun laterReload(p: Player, later: Long) {
        init = false
        Bukkit.getScheduler().runTaskLater(BukkitMain.Plugin, { reload(p) }, later)
    }

    init {
        reload(p)
    }

    fun reload(p: Player) {
        init = false
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
            init = true
            Bukkit.getScheduler().runTaskLater(BukkitMain.Plugin, {
                KViewHandler.updateUI(p)
            }, 5)
        }
    }

    companion object {
        lateinit var title: String
        lateinit var views: Array<KView<ShopViewContext>>

        var hasShop = false
        fun loadShop() {
            val f = File(BukkitMain.Plugin.dataFolder, "shop.yml")
            if (!f.exists()) {
                Utils.saveResource(BukkitMain.Plugin, "shop.yml")
            }
            val config = YamlConfiguration.loadConfiguration(f)
            val setting = config.getConfigurationSection("Setting")
            title = ChatColor.translateAlternateColorCodes('&', setting.getString("title"))
            val maxPage = setting.getInt("maxPage", 1)
            hasShop = maxPage > 0
            val contents = config.getConfigurationSection("Contents")
            views = Array(maxPage) {
                val viewPage = it
                KViewHandler.createKView("GuildHome Shop $viewPage", 6, ::ShopViewContext) {
                    for (i in 0..44) {
                        val index = i + viewPage * 45
                        if (contents.contains("$index")) {
                            val sub = contents.getConfigurationSection("$index")
                            val type = sub.getString("Type")
                            val con = Item.items[type] ?: continue
                            val item = con(sub)
                            +index += item.build(this)
                        }
                    }
                    if (viewPage > 0) {
                        icon(45) {
                            val prev = ItemBuilder.createItem(Material.ARROW) {
                                name("§6上一页")
                            }
                            display {
                                prev
                            }
                            click {
                                if (init) {
                                    KViewHandler.openUI(player, views[viewPage - 1])
                                }
                            }
                        }
                    }
                    if (viewPage + 1 < maxPage) {
                        icon(53) {
                            val next = ItemBuilder.createItem(Material.ARROW) {
                                name("§6下一页")
                            }
                            display {
                                next
                            }
                            click {
                                if (init) {
                                    KViewHandler.openUI(player, views[viewPage + 1])
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@KViewMaker
internal inline fun KItem<ShopViewContext>.display(crossinline func: ShopViewContext.() -> ItemStack?) {
    initDisplay {
        if (!init) {
            return@initDisplay GuildView.unready
        }
        return@initDisplay func(this)
    }
}