package com.github.bryanser.guildhome.bukkit.shop

import Br.API.Utils
import com.github.bryanser.brapi.kview.KIcon
import com.github.bryanser.brapi.kview.builder.KViewBuilder
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.material.Dye
import org.bukkit.material.Wool
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

@Suppress("LeakingThis")
abstract class Item(
        config: ConfigurationSection
) {
    val index: Int = config.name.toInt()
    var display: (ShopViewContext) -> ItemStack?

    init {
        display = loadDisplay(config)
    }


    open fun loadDisplay(config: ConfigurationSection): (ShopViewContext) -> ItemStack? {
        val dis = config.getString("Display")!!
        val item = Utils.readItemStack(dis)
        return d@{t->
            val i = item.clone()
            if(i.hasItemMeta()){
                val im = i.itemMeta
                if(im.hasDisplayName()){
                    im.displayName = papi(im.displayName,t.player)
                }
                if(im.hasLore()){
                    im.lore = im.lore.map{
                        papi(it,t.player)
                    }
                }
                i.itemMeta = im
            }
            i
        }
    }

    abstract fun info(gid: Int): String?

    abstract fun build(view: KViewBuilder<ShopViewContext>): KIcon<ShopViewContext>

    companion object {
        val PAPIRegEx = Pattern.compile("%[^%]+%")

        fun papi(t: String, p: Player): String {
            val replace = mutableSetOf<String>()
            val matcher = PAPIRegEx.matcher(t)
            var format = t
            while (matcher.find()) {
                val g = matcher.group(0)
                if (!replace.contains(g)) {
                    replace.add(g)
                }
            }
            for (s in replace) {
                try {
                    val str = PlaceholderAPI.setPlaceholders(p, s)
                    if (str != null) {
                        format = format.replace(s, str)
                    } else {
                        format = format.replace(s, "")
                    }
                } catch (e: Throwable) {
                    format = format.replace(s, "")
                }
            }
            return format
        }

        val items = HashMap<String, (ConfigurationSection) -> Item>()
        @Deprecated("")
        var itemReplacer: (ItemStack, Player) -> ItemStack = { i, p -> i }

        init {
            items["ICON"] = ::Icon
            items["EXP"] = ::Exp
            items["COMMAND"] = ::Command
            items["LOOT"] = ::Loot
            items["WELORE"] = ::WeLore
            items["WeLore"] = ::WeLore
            items["Buff"] = ::Buff
            items["BUFF"] = ::Buff
        }

    }
}


