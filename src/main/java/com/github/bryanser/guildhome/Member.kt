package com.github.bryanser.guildhome

import com.github.bryanser.guildhome.database.Career
import java.util.*

data class Member(
        val uuid: UUID,
        val gid: Int,
        val career: Career,
        val contribution: Int
) : IMember

interface IMember

class NullMember : IMember