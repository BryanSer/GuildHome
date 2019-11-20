package com.github.bryanser.guildhome.database

enum class Career(
        val level: Int,
        val display:String
) {
    MEMBER(0,"§a成员"),
    MANAGER(1,"§b管理员"),
    VP(2,"§b§l副会长"),
    PRESIDENT(3,"§6§l会长");
}