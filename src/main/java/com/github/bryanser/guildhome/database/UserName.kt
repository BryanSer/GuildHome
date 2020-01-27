package com.github.bryanser.guildhome.database

import com.github.bryanser.guildhome.service.Service
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object UserName {
//    const val NONE_NAME = "§fcdc1jh09"
//    val cache = ConcurrentHashMap<UUID, String>()

//    fun getName(uuid: UUID, sync: Boolean = false): String? {
//        var name = cache[uuid]
//        if (name != null) {
//            if (name == NONE_NAME) {
//                return null
//            }
//            return name
//        }
//        val op = Bukkit.getOfflinePlayer(uuid)
//        if (op != null) {
//            name = op.name
//            cache[uuid] = name ?: NONE_NAME
//            return name
//        }
//        if (sync) {
//            return null
//        }
//
//        DatabaseHandler.sql {
//            val ps = this.prepareStatement("SELECT NAME FROM GuildUserName WHERE UUID = ?")
//            ps.setString(1, uuid.toString())
//            val rs = ps.executeQuery()
//            if (rs.next()) {
//                name = rs.getString(1)
//            }
//        }
//        if (name != null) {
//            cache[uuid] = name!!
//        } else {
//            cache[uuid] = NONE_NAME
//        }
//        return name
//    }
//
//    operator fun invoke(uuid: UUID, sync: Boolean = false): String? {
//        return getName(uuid, sync)
//    }
//
//    operator fun get(uuid: UUID, sync: Boolean = false): String? {
//        return getName(uuid, sync)
//    }

    operator fun set(uuid: UUID, value: String) {
        Service.async {
            try {
                DatabaseHandler.sql {
                    val search = this.prepareStatement("SELECT * FROM GuildUserName WHERE UUID = ? LIMIT 1")
                    search.setString(1, uuid.toString())
                    if (search.executeQuery().next()) {
                        val ps = this.prepareStatement("UPDATE GuildUserName SET NAME = ? WHERE UUID = ?")
                        ps.setString(1, value)
                        ps.setString(2, uuid.toString())
                        ps.executeUpdate()
                    } else {
                        val ps = this.prepareStatement("INSERT INTO GuildUserName VALUES (?, ?)")
                        ps.setString(1, uuid.toString())
                        ps.setString(2, value)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Throwable) {
            }
        }
    }
}