package com.github.bryanser.guildhome.database

import java.sql.PreparedStatement
import java.sql.ResultSet

data class Key<V>(
        val name: String,
        val getter: ResultSet.(String) -> V,
        val setter: PreparedStatement.(Int, V) -> Unit
) {
    companion object {
        val ID = Key<Int>("ID", ResultSet::getInt, PreparedStatement::setInt)
        val GID = Key<Int>("GID", ResultSet::getInt, PreparedStatement::setInt)
        val NAME = Key<String>("NAME", ResultSet::getString, PreparedStatement::setString)
        val LEVEL = Key<Int>("LEVEL", ResultSet::getInt, PreparedStatement::setInt)
        val CONTRIBUTION = Key<Int>("CONTRIBUTION", ResultSet::getInt, PreparedStatement::setInt)
        val MOTD = Key<String>("MOTD", ResultSet::getString, PreparedStatement::setString)
        val ICON = Key<String?>("ICON", ResultSet::getString, PreparedStatement::setString)
        val CARREER = Key<Career>("CAREER", { s ->
            val c = this.getString(s)
            com.github.bryanser.guildhome.database.Career.valueOf(c)
        }) { i, v ->
            this.setString(i, v.toString())
        }
        val TIME = Key<Long>("TIME", ResultSet::getLong, PreparedStatement::setLong)
    }
}