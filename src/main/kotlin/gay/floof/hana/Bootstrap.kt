/*
 * 🥀 hana: API to proxy different APIs like GitHub Sponsors, source code for api.floofy.dev
 * Copyright (c) 2020-2022 Noel <cutie@floofy.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package gay.floof.hana

import com.charleskorn.kaml.Yaml
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.util.IsolationLevel
import gay.floof.hana.core.Hana
import gay.floof.hana.core.database.tables.ApiKeysTable
import gay.floof.hana.core.extensions.inject
import gay.floof.hana.core.hanaModule
import gay.floof.hana.core.managers.RedisManager
import gay.floof.hana.data.Environment
import gay.floof.hana.data.HanaConfig
import gay.floof.hana.routing.routingModule
import gay.floof.hana.utils.BannerPrinter
import gay.floof.utils.slf4j.logging
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Bootstrap: AutoCloseable {
    private val log by logging<Bootstrap>()

    @JvmStatic
    fun main(args: Array<String>) {
        Thread.currentThread().name = "Hana-BootstrapThread"
        BannerPrinter.print()

        val runtime = Runtime.getRuntime()
        runtime.addShutdownHook(
            thread(start = false, name = "Hana-ShutdownThread") {
                close()
            }
        )

        log.info("* Initializing configuration...")
        val configPathEnv = System.getenv("HANA_CONFIG_PATH")
        val configFile = File(configPathEnv ?: "./config.yml")

        if (!configFile.exists())
            throw IllegalStateException("Missing configuration path in \$ROOT/config.yml or under HANA_CONFIG_PATH=... environment variable!")

        val config = Yaml.default.decodeFromString(HanaConfig.serializer(), configFile.readText(Charsets.UTF_8))

        log.info("* Initialized configuration! Now connecting to PostgreSQL...")
        val dataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.name}"
                username = config.database.username
                password = config.database.password
                schema = config.database.schema
                driverClassName = "org.postgresql.Driver"
                isAutoCommit = false
                transactionIsolation = IsolationLevel.TRANSACTION_REPEATABLE_READ.name
                leakDetectionThreshold = 30 * 1000
                poolName = "Hana-HikariPool"
            }
        )

        Database.connect(
            dataSource,
            databaseConfig = DatabaseConfig.invoke {
                defaultRepetitionAttempts = 5
                defaultIsolationLevel = IsolationLevel.TRANSACTION_REPEATABLE_READ.levelId
                sqlLogger = if (config.environment == Environment.Development) {
                    Slf4jSqlDebugLogger
                } else {
                    null
                }
            }
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(ApiKeysTable)
        }

        log.info("* Connected to PostgreSQL! Now connecting to Redis...")

        val redis = RedisManager(config)
        redis.connect()

        log.info("* Connected to Redis! Now initializing Koin...")
        val koin = startKoin {
            modules(
                hanaModule,
                routingModule,
                module {
                    single { config }
                    single { dataSource }
                    single { redis }
                }
            )
        }

        val hana = koin.koin.get<Hana>()
        runBlocking {
            try {
                hana.start()
            } catch (e: Exception) {
                log.error("* Unable to bootstrap hana:", e)
                exitProcess(1)
            }
        }
    }

    override fun close() {
        log.info("Shutting down hana...")

        runBlocking {
            val hana: Hana by inject()

            hana.close()
        }

        val redis: RedisManager by inject()
        val ds: HikariDataSource by inject()

        redis.close()
        ds.close()
    }
}
