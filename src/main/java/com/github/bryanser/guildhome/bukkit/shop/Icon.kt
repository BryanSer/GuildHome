package com.github.bryanser.guildhome.bukkit.shop

import com.github.bryanser.brapi.kview.KIcon
import com.github.bryanser.brapi.kview.builder.KViewBuilder
import org.bukkit.configuration.ConfigurationSection

class Icon(config: ConfigurationSection) : Item(config) {
    override fun build(view: KViewBuilder<ShopViewContext>): KIcon<ShopViewContext> {
        return view.icon {
            initDisplay {
                super.display(this)
            }
        }
    }

    override fun info(gid: Int): String? {
        return null
    }
}