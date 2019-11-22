package com.github.bryanser.guildhome.bungee

import com.github.bryanser.guildhome.Guild
import com.github.bryanser.guildhome.GuildManager
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.database.DatabaseHandler
import com.github.bryanser.guildhome.database.Key
import java.util.*

object GuildSetManager {

    fun setMember(player: UUID, gid: Int, career: Career) {
        DatabaseHandler.sql {
            val ps = this.prepareStatement(
                    "DELETE FROM ${DatabaseHandler.TABLE_GUILD_MEMBER} WHERE NAME = ?"
            )
            ps.setString(1, player.toString())
            ps.executeUpdate()
            val ps2 = this.prepareStatement(
                    "INSERT INTO ${DatabaseHandler.TABLE_GUILD_MEMBER} (NAME,GID,CAREER) VALUES (?, ?, ?)"
            )
            ps2.setString(1, player.toString())
            ps2.setInt(2, gid)
            ps2.setString(3, career.name)
            ps2.executeUpdate()
        }
    }

    fun removeMember(player: UUID, gid: Int) {
        DatabaseHandler.sql {
            val ps = this.prepareStatement(
                    "DELETE FROM ${DatabaseHandler.TABLE_GUILD_MEMBER} WHERE NAME = ? AND GID = ?"
            )
            ps.setString(1, player.toString())
            ps.setInt(2, gid)
            ps.executeUpdate()
        }
    }

    fun updateGuild(g: Guild) {
        DatabaseHandler.sql {
            val ps = this.prepareStatement(
                    "UPDATE ${DatabaseHandler.TABLE_GUILDHOME} SET LEVEL = ?, CONTRIBUTION = ?, MOTD = ?, ICON = ? WHERE ID = ? LIMIT 1"
            )
            ps.setInt(1, g.level)
            ps.setInt(2, g.contribution)
            ps.setString(3, g.motd)
            ps.setString(4, g.icon)
            ps.setInt(5, g.id)
            ps.executeUpdate()
        }
    }

    fun <T> updateGuild(gid: Int, key: Key<T>, value: T) {
        DatabaseHandler.sql {
            val ps = this.prepareStatement(
                    "UPDATE ${DatabaseHandler.TABLE_GUILDHOME} SET ${key.name} = ? WHERE ID = ? LIMIT 1"
            )
            key.setter(ps, 1, value)
            ps.setInt(2, gid)
            ps.executeUpdate()
        }
    }

    fun <T> updateMember(gid: Int, uuid: UUID, key: Key<T>, value: T) {
        DatabaseHandler.sql {
            val ps = this.prepareStatement("UPDATE ${DatabaseHandler.TABLE_GUILD_MEMBER} SET ${key.name} = ? WHERE GID = ? AND NAME = ? LIMIT 1")
            key.setter(ps, 1, value)
            ps.setInt(2, gid)
            ps.setString(3, uuid.toString())
            ps.executeUpdate()
        }
    }


    fun createGuild(name: String, owner: UUID): Pair<Int?, String?> {
        DatabaseHandler.select("SELECT ID FROM ${DatabaseHandler.TABLE_GUILDHOME} WHERE NAME = ?") {
            where().setString(1, name)
            select()
            if (next()) {
                return null to "已存在同名公会"
            }
        }
        val pi = GuildManager.getMember(owner)
        if (pi != null) {
            return null to "你已经有公会了"
        }
        DatabaseHandler.sql {
            val ps = this.prepareStatement(
                    "INSERT INTO ${DatabaseHandler.TABLE_GUILDHOME} (NAME,MOTD) VALUES (?, ?)"
            )
            ps.setString(1, name)
            ps.setString(2, "无")
            ps.executeUpdate()
        }
        var id: Int = -1
        DatabaseHandler.select("SELECT ID FROM ${DatabaseHandler.TABLE_GUILDHOME} WHERE NAME = ?") {
            where().setString(1, name)
            select()
            if (next()) {
                id = get(Key.ID)
            } else {
                return null to "发生未知错误"
            }
        }
        if (id != -1) {
            DatabaseHandler.sql {
                val ps = this.prepareStatement("INSERT INTO ${DatabaseHandler.TABLE_GUILD_MEMBER} (NAME,GID,CAREER) VALUES (?, ?, ?)")
                ps.setString(1, owner.toString())
                ps.setInt(2, id)
                ps.setString(3, Career.PRESIDENT.name)
                ps.executeUpdate()
            }
        } else {
            return null to "发生未知错误"
        }
        return id to null
    }

}