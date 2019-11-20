package com.github.bryanser.guildhome.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

object DatabaseHandler {
    const val TABLE_GUILDHOME = "GuildHome"
    const val TABLE_GUILD_MEMBER = "GuildMember"


    class Selecting(
            private val preparedStatement: PreparedStatement
    ) {
        lateinit var set: ResultSet

        fun where(): PreparedStatement {
            return preparedStatement
        }

        fun select() {
            set = preparedStatement.executeQuery()
        }

        fun next(): Boolean {
            return set.next()
        }

        fun <V> get(key: Key<V>): V {
            val g = key.getter
            return set.g(key.name)
        }
    }

    inline fun select(sql: String, func: Selecting.() -> Unit) {
        sql {
            val ps = this.prepareStatement(sql)
            val s = Selecting(ps)
            s.func()
        }
    }

    lateinit var pool: HikariDataSource

    fun init(config: HikariConfig) {
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config.idleTimeout = 60000
        config.connectionTimeout = 60000
        config.validationTimeout = 3000
        config.maxLifetime = 60000
        pool = HikariDataSource(config)
    }

    fun createTable() {
        sql {
            val sta = createStatement()
            sta.execute("""
                CREATE TABLE IF NOT EXISTS GuildHome(
                    ID INT PRIMARY KEY AUTO_INCREMENT,
                    NAME VARCHAR(30) NOT NULL,
                    LEVEL INT NOT NULL DEFAULT 0,
                    CONTRIBUTION INT NOT NULL DEFAULT 0,
                    MOTD TEXT NOT NULL,
                    ICON TEXT DEFAULT NULL
                ) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4
            """)
            sta.execute("""
                CREATE TABLE IF NOT EXISTS GuildMember(
                    NAME VARCHAR(80) NOT NULL PRIMARY KEY,
                    GID INT NOT NULL,
                    CAREER VARCHAR(15) NOT NULL DEFAULT 'MEMBER',
                    FOREIGN KEY (GID) REFERENCES GuildHome(ID)
                ) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4
            """)
            sta.close()
        }
    }


    inline fun sql(func: Connection.() -> Unit) {
        val conn = pool.connection
        try {
            conn.func()
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            pool.evictConnection(conn)
        }
    }
}