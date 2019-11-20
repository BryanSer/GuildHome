package com.github.bryanser.guildhome

import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.DatabaseHandler
import java.util.*

object GuildManager {
    fun getGuild(id: Int): Guild? {
        var g: Guild? = null
        DatabaseHandler.sql {
            val ps = this.prepareStatement("SELECT * FROM ${DatabaseHandler.TABLE_GUILDHOME} WHERE ID = ? LIMIT 1")
            ps.setInt(1, id)
            val rs = ps.executeQuery()
            if (rs.next()) {
                g = Guild(
                        id,
                        rs.getString("NAME"),
                        rs.getInt("LEVEL"),
                        rs.getInt("CONTRIBUTION"),
                        rs.getString("MOTD"),
                        rs.getString("ICON")
                )
            }
        }
        return g
    }

    fun getGuildByName(name: String): Int? {
        var id: Int? = null
        DatabaseHandler.sql {
            val ps = this.prepareStatement("SELECT ID FROM ${DatabaseHandler.TABLE_GUILDHOME} WHERE NAME = ? LIMIT 1")
            ps.setString(1, name)
            val rs = ps.executeQuery()
            if (rs.next()) {
                id = rs.getInt("ID")
            }
        }
        return id
    }

    fun getMember(uuid: UUID): Pair<Int, Career>? {
        var r: Pair<Int, Career>? = null
        DatabaseHandler.sql {
            val ps = this.prepareStatement(
                    "SELECT * FROM ${DatabaseHandler.TABLE_GUILD_MEMBER} WHERE NAME = ? LIMIT 1"
            )
            ps.setString(1, uuid.toString())
            val rs = ps.executeQuery()
            if (rs.next()) {
                val id = rs.getInt("GID")
                val career = Career.valueOf(rs.getString("CAREER"))
                r = id to career
            }
        }
        return r
    }
}