package com.github.bryanser.guildhome

import com.github.bryanser.guildhome.bungee.GuildSetManager
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

    fun getMember(uuid: UUID): Member? {
        var r: Member? = null
        DatabaseHandler.sql {
            val ps = this.prepareStatement(
                    "SELECT * FROM ${DatabaseHandler.TABLE_GUILD_MEMBER} WHERE NAME = ? LIMIT 1"
            )
            ps.setString(1, uuid.toString())
            val rs = ps.executeQuery()
            if (rs.next()) {
                val id = rs.getInt("GID")
                val career = Career.valueOf(rs.getString("CAREER"))
                r = Member(uuid, id, career, rs.getInt("CONTRIBUTION"))
            }
        }
        return r
    }

    fun getMembers(gid: Int): List<Member> {
        val list = mutableListOf<Member>()
        DatabaseHandler.sql {
            val ps = this.prepareStatement("SELECT * FROM ${DatabaseHandler.TABLE_GUILD_MEMBER} WHERE GID = ?")
            ps.setInt(1, gid)
            val rs = ps.executeQuery()
            while (rs.next()) {
                val uuid = UUID.fromString(rs.getString("NAME"))
                val id = rs.getInt("GID")
                val career = Career.valueOf(rs.getString("CAREER"))
                list += Member(uuid, id, career, rs.getInt("CONTRIBUTION"))
            }
        }
        return list
    }

    fun addApply(gid: Int, from: UUID):String {
        DatabaseHandler.sql{
            val ps = this.prepareStatement("SELECT * FROM ${DatabaseHandler.TABLE_GUILD_APPLY} WHERE GID = ? AND NAME = ?")
            ps.setInt(1,gid)
            ps.setString(2,from.toString())
            val rs = ps.executeQuery()
            if(rs.next()){
                return "§c申请失败, 你已经申请过了"
            }
        }
        DatabaseHandler.sql {
            val ps = this.prepareStatement("INSERT INTO ${DatabaseHandler.TABLE_GUILD_APPLY} VALUES (?, ?, ?)")
            ps.setString(1, from.toString())
            ps.setInt(2, gid)
            ps.setLong(3, System.currentTimeMillis())
            ps.executeUpdate()
        }

        return "§6申请成功"
    }

    fun getApplySize(gid: Int): Int {
        var size = 0
        DatabaseHandler.sql {
            val ps = this.prepareStatement("SELECT COUNT(*) FROM ${DatabaseHandler.TABLE_GUILD_APPLY} WHERE GID = ?")
            ps.setInt(1, gid)
            val rs = ps.executeQuery()
            if (rs.next()) {
                size = rs.getInt(1)
            }
        }
        return size
    }

    fun getApplys(gid: Int): List<Pair<UUID, Long>> {
        val list = mutableListOf<Pair<UUID, Long>>()
        DatabaseHandler.sql {
            val ps = this.prepareStatement("SELECT * FROM ${DatabaseHandler.TABLE_GUILD_APPLY} WHERE GID = ?")
            ps.setInt(1, gid)
            val rs = ps.executeQuery()
            while (rs.next()) {
                list += UUID.fromString(rs.getString("NAME")) to rs.getLong("TIME")
            }
        }
        return list
    }

    fun apply(gid: Int, uuid: UUID): String {
        val m = this.getMember(uuid)
        if (m != null) {
            DatabaseHandler.sql(false) {
                val ps = this.prepareStatement("DELETE FROM ${DatabaseHandler.TABLE_GUILD_APPLY} WHERE NAME = ?")
                ps.setString(1, uuid.toString())
                ps.executeQuery()
            }
            return "§c请求处理失败: 对方已经加入了别的公会"
        }
        val guild = this.getGuild(gid) ?: return "§c请求处理失败: 找不到公会"
        val size = this.getMembers(gid).size
        if (size >= Guild.getMaxMemberSize(guild.level)) {
            return "§c请求处理失败: 公会人数已满"
        }
        DatabaseHandler.sql(true) {
            val ps = this.prepareStatement("DELETE FROM ${DatabaseHandler.TABLE_GUILD_APPLY} WHERE NAME = ?")
            ps.setString(1, uuid.toString())
            ps.executeQuery()
            GuildSetManager.setMember(uuid, gid, Career.MEMBER)
        }
        return "§6请求同意成功"
    }

    fun refuse(gid: Int, uuid: UUID): String {
        val m = this.getMember(uuid)
        if (m != null) {
            DatabaseHandler.sql(false) {
                val ps = this.prepareStatement("DELETE FROM ${DatabaseHandler.TABLE_GUILD_APPLY} WHERE NAME = ?")
                ps.setString(1, uuid.toString())
                ps.executeQuery()
            }
            return "§c请求处理失败: 对方已经加入了别的公会"
        }
        val guild = this.getGuild(gid) ?: return "§c请求处理失败: 找不到公会"
        DatabaseHandler.sql(false) {
            val ps = this.prepareStatement("DELETE FROM ${DatabaseHandler.TABLE_GUILD_APPLY} WHERE NAME = ? AND GID = ?")
            ps.setString(1, uuid.toString())
            ps.setInt(2, gid)
            ps.executeQuery()
        }
        return "§6请求拒绝成功"
    }
}