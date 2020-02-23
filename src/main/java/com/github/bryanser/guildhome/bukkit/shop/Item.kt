package com.github.bryanser.guildhome.bukkit.shop

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
        return d@{
            val str = papi(dis, it.player).replace("%%", "%")
//            Bukkit.getLogger().info("替换后信息: $str")
            readItemStack(str)
        }
    }

    abstract fun build(view: KViewBuilder<ShopViewContext>): KIcon<ShopViewContext>

    companion object {
        val PAPIRegEx = Pattern.compile("%[^%]*%")

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

        fun readItemStack(s: String): ItemStack? {
            val item: ItemStack
            item = try {
                ItemStack(Material.getMaterial(s.split(" ").toTypedArray()[0].toInt()))
            } catch (e: NumberFormatException) {
                ItemStack(Material.getMaterial(s.split(" ").toTypedArray()[0]))
            }
            var i = 0
            for (data in s.split(" ").toTypedArray()) {
                var data = data
                if (i == 0) {
                    i++
                    continue
                }
                if (i == 1) {
                    try {
                        item.amount = data.toInt()
                    } catch (e: NumberFormatException) {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l在读取物品: $s 时出现错误"))
                    }
                    i++
                    continue
                }
                if (i == 2) {
                    try {
                        item.durability = data.toShort()
                    } catch (e: NumberFormatException) {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l在读取物品: $s 时出现错误"))
                    }
                    i++
                    continue
                }
                if (data.toLowerCase().contains("name:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    data = ChatColor.translateAlternateColorCodes('&', data)
                    data = data.replace("_".toRegex(), " ")
                    val im = item.itemMeta
                    im.displayName = data
                    item.itemMeta = im
                    continue
                }
                if (data.toLowerCase().contains("lore:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    val lores = data.split("|").toTypedArray()
                    val LoreList: MutableList<String> = ArrayList()
                    for (o in lores.indices) {
                        lores[o] = lores[o].replace("_".toRegex(), " ")
                        lores[o] = ChatColor.translateAlternateColorCodes('&', lores[o])
                    }
                    LoreList.addAll(Arrays.asList(*lores))
                    val im = item.itemMeta
                    im.lore = LoreList
                    item.itemMeta = im
                    continue
                }
                if (data.toLowerCase().contains("hide:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    val im = item.itemMeta
                    for (str in data.split(",").toTypedArray()) {
                        im.addItemFlags(ItemFlag.valueOf(str))
                    }
                    item.itemMeta = im
                    continue
                }
                if (data.toLowerCase().contains("ench:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    val str = data.split("-").toTypedArray()
                    var e: Enchantment? = null
                    e = try {
                        Enchantment.getById(str[0].toInt())
                    } catch (ee: NumberFormatException) {
                        Enchantment.getByName(str[0])
                    }
                    val lv = str[1].toInt()
                    item.addUnsafeEnchantment(e, lv)
                }
                if (data.toLowerCase().contains("color:")) {
                    data = data.substring(data.indexOf(":") + 1)
                    if (item.data is Dye) {
                        val d = item.data as Dye
                        d.color = DyeColor.valueOf(data)
                        item.data = d
                        continue
                    }
                    if (item.data is Wool) {
                        val w = item.data as Wool
                        w.color = DyeColor.valueOf(data)
                        item.data = w
                        continue
                    }
                    if (item.itemMeta is LeatherArmorMeta) {
                        val lam = item.itemMeta as LeatherArmorMeta
                        lam.color = Color.fromRGB(data.toInt())
                        item.itemMeta = lam
                        continue
                    }
                }
            }
            return item
        }
    }
}


