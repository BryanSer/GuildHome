package com.github.bryanser.guildhome

import com.google.gson.Gson
import org.bukkit.inventory.ItemStack

object StringManager {
    val gson = Gson()

    fun toJson(map: Map<String, Any>): String = gson.toJson(map)



    @Suppress("UNCHECKED_CAST")
    fun fromJson(json: String): Map<String, Any> = gson.fromJson(json, Map::class.java) as Map<String, Any>


}