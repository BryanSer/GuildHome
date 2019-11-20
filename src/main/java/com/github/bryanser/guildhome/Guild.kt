package com.github.bryanser.guildhome

data class Guild(
        val id: Int,
        val name: String,
        var level: Int,
        var contribution: Int,
        var motd: String,
        var icon: String?
)