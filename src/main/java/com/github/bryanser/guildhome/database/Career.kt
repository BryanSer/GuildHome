package com.github.bryanser.guildhome.database

enum class Career(
        val level: Int,
        val display:String
) {
    MEMBER(0,"§a会员"),
    MANAGER(1,"§e§l管理员"),
    VP(2,"§6§l副会长"),
    PRESIDENT(3,"§c§l会长");
}