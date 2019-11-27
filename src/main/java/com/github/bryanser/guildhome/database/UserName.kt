package com.github.bryanser.guildhome.database

import com.github.bryanser.guildhome.service.Service
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object UserName {
    const val NONE_NAME = "§fcdc1jh09"

    val cache = ConcurrentHashMap<UUID, String>()

    fun getName(uuid: UUID): String? {
        var name = cache[uuid]
        if (name != null) {
            if(name == NONE_NAME){
                return null
            }
            return name
        }
        val op = Bukkit.getOfflinePlayer(uuid)
        if(op != null){
            name = op.name
            cache[uuid] = name
            return name
        }

        DatabaseHandler.sql {
            val ps = this.prepareStatement("SELECT NAME FROM GuildUserName WHERE UUID = ?")
            ps.setString(1, uuid.toString())
            val rs = ps.executeQuery()
            if (rs.next()) {
                name = rs.getString(1)
            }
        }
        if(name != null) {
            cache[uuid] = name!!
        }else{
            cache[uuid] = NONE_NAME
        }
        return name
    }

    operator fun invoke(uuid:UUID):String?{
        return getName(uuid)
    }

    operator fun get(uuid:UUID):String?{
        return getName(uuid)
    }

    operator fun set(uuid: UUID, value: String) {
        Service.async {
            val name = getName(uuid)
            if (name == value) {
                return@async
            }
            cache[uuid] = value
            if (name == null) {
                DatabaseHandler.sql {
                    val ps = this.prepareStatement("INSERT INTO GuildUserName VALUES (?, ?)")
                    ps.setString(1, uuid.toString())
                    ps.setString(2, value)
                    ps.executeUpdate()
                }
            } else {
                DatabaseHandler.sql {
                    DatabaseHandler.sql {
                        val ps=  this.prepareStatement("UPDATE GuildUserName SET NAME = ? WHERE UUID = ?")
                        ps.setString(1, value)
                        ps.setString(2, uuid.toString())
                        ps.executeUpdate()
                    }
                }
            }
        }
    }
}