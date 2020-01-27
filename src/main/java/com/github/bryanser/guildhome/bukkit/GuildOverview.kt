package com.github.bryanser.guildhome.bukkit

import com.github.bryanser.brapi.ItemBuilder
import com.github.bryanser.brapi.kview.KView
import com.github.bryanser.brapi.kview.KViewContext
import com.github.bryanser.brapi.kview.KViewHandler
import com.github.bryanser.guildhome.Guild
import com.github.bryanser.guildhome.GuildInfo
import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.service.impl.ApplyService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

object GuildOverview {
    const val MAX_PAGE = 10
    class GuildOverviewContext(p: Player) : KViewContext(
            "§3§l楼楼公会列表"
    ) {
        @Volatile
        var init: Boolean = false
        var page: Int = 0
        var hasGuild: Boolean = false
        lateinit var guilds: List<GuildInfo>

        init {
            Bukkit.getScheduler().runTaskAsynchronously(BukkitMain.Plugin) {
                guilds = GuildManager.getAllGuild()
                hasGuild = GuildManager.getMember(player.uniqueId) != null
                init = true
                Bukkit.getScheduler().runTaskLater(BukkitMain.Plugin, {
                    KViewHandler.updateUI(p)
                }, 5)
            }
        }
    }

    val view: KView<GuildOverviewContext> by lazy {
        KViewHandler.createKView("GuildOverview", 6, ::GuildOverviewContext) {
            for (i in 0 until 45) {
                icon(i) {
                    initDisplay {
                        if (!init) {
                            return@initDisplay GuildView.unready
                        }
                        val index = i + page * 45
                        val gi = guilds.getOrNull(index) ?: return@initDisplay null
                        gi.getDisplay(!hasGuild)
                    }
                    click {
                        if (!hasGuild) {
                            val index = i + page * 45
                            val gi = guilds.getOrNull(index) ?: return@click
                            ApplyService.apply(gi.name, player)
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
                    val display = ItemBuilder.createItem(Material.EMERALD) {
                        name("§6§l打开我的公会")
                    }
                    initDisplay {
                        if (init) {
                            if (hasGuild) {
                                display
                            } else {
                                GuildView.noGuild
                            }
                        } else {
                            null
                        }
                    }
                    click {
                        if (hasGuild)
                            KViewHandler.openUI(player, GuildView.view)
                    }
                }
                icon(53) {
                    val next = ItemBuilder.createItem(Material.ARROW) {
                        name("§6§l下一页")
                    }
                    initDisplay {
                        if (page < 999) {
                            next
                        } else {
                            null
                        }
                    }
                    click {
                        if (page < 999) {
                            page++
                        }
                    }
                }
            }
        }
    }
}