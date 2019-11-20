package com.github.bryanser.guildhome.database

import java.sql.ResultSet

data class Key<V>(
        val name: String,
        val getter: ResultSet.(String) -> V
) {
    companion object {
        val ID = Key<Int>("ID", ResultSet::getInt)
        val GID = Key<Int>("GID", ResultSet::getInt)
        val NAME = Key<String>("NAME", ResultSet::getString)
        val LEVEL = Key<Int>("LEVEL", ResultSet::getInt)
        val CONTRIBUTION = Key<Int>("CONTRIBUTION", ResultSet::getInt)
        val MOTD = Key<String>("MOTD", ResultSet::getString)
        val ICON = Key<String?>("ICON", ResultSet::getString)
        val Career = Key<Career>("CAREER") { s ->
            val c = this.getString(s)
            com.github.bryanser.guildhome.database.Career.valueOf(c)
        }
    }
}