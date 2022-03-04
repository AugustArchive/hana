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

package gay.floof.hana.core

import dev.kord.common.entity.Snowflake
import dev.kord.rest.service.RestClient
import gay.floof.hana.core.discord.commands.CreateApiKeyCommand
import gay.floof.hana.core.discord.commands.ListApiKeysCommand
import gay.floof.hana.core.discord.commands.RevokeApiKeyCommand
import gay.floof.hana.core.discord.executors.CreateApiKeyCommandExecutor
import gay.floof.hana.core.discord.executors.ListApiKeyCommandExecutor
import gay.floof.hana.core.discord.executors.RevokeApiKeyCommandExecutor
import gay.floof.hana.core.extensions.formatSize
import gay.floof.hana.core.extensions.inject
import gay.floof.hana.core.interfaces.SuspendAutoCloseable
import gay.floof.hana.core.plugins.KtorDocsReverseProxyPlugin
import gay.floof.hana.core.plugins.KtorLoggingPlugin
import gay.floof.hana.core.plugins.KtorSentryPlugin
import gay.floof.hana.core.threading.threadFactory
import gay.floof.hana.data.Environment
import gay.floof.hana.data.HanaConfig
import gay.floof.hana.routing.AbstractEndpoint
import gay.floof.utils.slf4j.logging
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.sentry.Sentry
import kotlinx.serialization.json.Json
import net.perfectdreams.discordinteraktions.common.commands.CommandManager
import net.perfectdreams.discordinteraktions.platforms.kord.commands.KordCommandRegistry
import net.perfectdreams.discordinteraktions.webserver.DefaultInteractionRequestHandler
import net.perfectdreams.discordinteraktions.webserver.installDiscordInteractions
import org.koin.core.context.GlobalContext
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Hana: SuspendAutoCloseable {
    companion object {
        val executorPool: ExecutorService = Executors.newFixedThreadPool(16, threadFactory("Hana-ExecutorPool"))
        val bootTime = System.currentTimeMillis()
    }

    private val log by logging<Hana>()
    lateinit var server: NettyApplicationEngine

    suspend fun start() {
        val runtime = Runtime.getRuntime()
        val os = ManagementFactory.getOperatingSystemMXBean()
        val threading = ManagementFactory.getThreadMXBean()

        log.info("* Runtime Information:")
        log.info("* Operating System: ${os.name} ${os.arch} with ${os.availableProcessors} available processors (${os.version})")
        log.info("* Threading: ${threading.threadCount} threads with ${threading.daemonThreadCount} threads as a daemon thread.")
        log.info("* Available Memory [Free / Total]: ${runtime.freeMemory().formatSize()}/${runtime.freeMemory().formatSize()}")
        log.info("* Versions:")
        log.info("*    JVM: v${System.getProperty("java.version", "Unknown")} (${System.getProperty("java.vendor", "Unknown")})")
        log.info("*    JRE: ${Runtime.version()}")
        log.info("*    Kotlin: v${KotlinVersion.CURRENT}")
        log.info("*    Hana: v${HanaInfo.VERSION} (${HanaInfo.COMMIT_HASH})")

        if (HanaInfo.dediNode != null)
            log.info("* Running under dedicated node: ${HanaInfo.dediNode}")

        log.info("")

        val koin = GlobalContext.get()
        val config by inject<HanaConfig>()

        // Check if we need to enable Sentry
        if (config.sentryDsn != null) {
            log.info("* Installing Sentry...")
            Sentry.init {
                it.dsn = config.sentryDsn
                it.release = "hana v${HanaInfo.VERSION} (${HanaInfo.COMMIT_HASH})"
            }

            Sentry.configureScope {
                it.tags += mapOf(
                    "helm.server.environment" to config.environment.asName(),
                    "helm.server.commit.sha" to HanaInfo.COMMIT_HASH,
                    "helm.server.build.date" to HanaInfo.BUILD_DATE,
                    "helm.server.version" to HanaInfo.VERSION,
                    "system.user" to System.getProperty("user.name"),
                    "system.os" to "${os.name} (${os.arch}; ${os.version})"
                )
            }

            log.info("* Sentry is now enabled with DSN ${config.sentryDsn}")
        }

        val applicationId = Snowflake(948997076216340570L)
        val commandManager = CommandManager()
        val restClient = RestClient(config.token)
        val registry = KordCommandRegistry(applicationId, restClient, commandManager)

        commandManager.register(
            CreateApiKeyCommandExecutor,
            CreateApiKeyCommand()
        )

        commandManager.register(
            ListApiKeyCommandExecutor,
            ListApiKeysCommand()
        )

        commandManager.register(
            RevokeApiKeyCommandExecutor,
            RevokeApiKeyCommand()
        )

        registry.updateAllCommandsInGuild(Snowflake(824066105102303232), true)
        registry.updateAllCommandsInGuild(Snowflake(743698927039283201), true)

        log.info("* Launching HTTP service...")
        val environment = applicationEngineEnvironment {
            this.developmentMode = config.environment == Environment.Development
            this.log = LoggerFactory.getLogger("gay.floof.hana.server.ktor.Application")

            connector {
                host = config.host
                port = config.port
            }

            module {
                val json: Json by inject()

                install(KtorDocsReverseProxyPlugin)
                install(KtorLoggingPlugin)
                install(KtorSentryPlugin)

                install(ContentNegotiation) {
                    json(json)
                }

                install(CORS) {
                    header("X-Forwarded-Proto")
                    anyHost()
                }

                install(DefaultHeaders) {
                    header("X-Powered-By", "noel/hana (+https://github.com/auguwu/hana; v${HanaInfo.VERSION})")
                    header("Cache-Control", "public, max-age=7776000")

                    if (config.server.securityHeaders) {
                        header("X-Frame-Options", "deny")
                        header("X-Content-Type-Options", "nosniff")
                        header("X-XSS-Protection", "1; mode=block")
                    }

                    for ((key, value) in config.server.extraHeaders) {
                        header(key, value)
                    }
                }

                routing {
                    val endpoints = koin.getAll<AbstractEndpoint>()
                    log.info("Found ${endpoints.size} endpoints to register.")

                    for (endpoint in endpoints) {
                        route(endpoint.path, endpoint.method) {
                            handle {
                                try {
                                    endpoint.call(call)
                                } catch (e: Exception) {
                                    log.error("Unable to handle request ${call.request.httpMethod.value} ${call.request.path()}:", e)
                                }
                            }
                        }
                    }

                    log.info("Registered all endpoints! Now registering Discord Interactions...")
                    installDiscordInteractions(
                        config.publicKey,
                        "/api/interactions",
                        DefaultInteractionRequestHandler(applicationId, commandManager, restClient)
                    )
                }
            }
        }

        server = embeddedServer(
            Netty,
            environment,
            configure = {
                requestQueueLimit = config.server.requestQueueLimit
                runningLimit = config.server.runningLimit
                shareWorkGroup = config.server.shareWorkGroup
                responseWriteTimeoutSeconds = config.server.responseWriteTimeoutSeconds
                requestReadTimeoutSeconds = config.server.requestReadTimeout
                tcpKeepAlive = config.server.tcpKeepAlive
            }
        )

        if (!config.server.securityHeaders)
            log.warn("It is not recommended to disable security headers when requesting to the API. :)")

        server.start(wait = true)
    }

    override suspend fun close() {
        log.info("Closing HTTP service...")

        if (!::server.isInitialized) {
            log.warn("Server was never established, skipping!")
            return
        }

        server.stop(1, 5, TimeUnit.SECONDS)
    }
}
