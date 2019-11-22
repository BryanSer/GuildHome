package com.github.bryanser.guildhome

data class Guild(
        val id: Int,
        val name: String,
        var level: Int,
        var contribution: Int,
        var motd: String,
        var icon: String?
) {
    companion object {
        fun getMaxMemberSize(lv: Int): Int {
            return when (lv) {
                1 -> 30
                2 -> 50
                3 -> 80
                else -> 80
            }
        }
    }
}