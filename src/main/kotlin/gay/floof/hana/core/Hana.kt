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
import gay.floof.hana.core.discord.commands.EditApiKeyCommand
import gay.floof.hana.core.discord.commands.InfoOnApiKeyCommand
import gay.floof.hana.core.discord.commands.RevokeApiKeyCommand
import gay.floof.hana.core.discord.executors.CreateApiKeyCommandExecutor
import gay.floof.hana.core.discord.executors.EditApiKeyCommandExecutor
import gay.floof.hana.core.discord.executors.InfoOnApiKeyCommandExecutor
import gay.floof.hana.core.discord.executors.RevokeApiKeyCommandExecutor
import gay.floof.hana.core.extensions.formatSize
import gay.floof.hana.core.extensions.inject
import gay.floof.hana.core.extensions.retrieve
import gay.floof.hana.core.interfaces.SuspendAutoCloseable
import gay.floof.hana.core.metrics.MetricsHandler
import gay.floof.hana.core.plugins.KtorBlockNsfwEndpoints
import gay.floof.hana.core.plugins.KtorLoggingPlugin
import gay.floof.hana.core.plugins.KtorRatelimitingPlugin
import gay.floof.hana.core.plugins.ratelimiter.Ratelimiter
import gay.floof.hana.core.threading.threadFactory
import gay.floof.hana.data.Environment
import gay.floof.hana.data.HanaConfig
import gay.floof.hana.routing.AbstractEndpoint
import gay.floof.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sentry.Sentry
import kotlinx.serialization.json.*
import net.perfectdreams.discordinteraktions.common.commands.CommandManager
import net.perfectdreams.discordinteraktions.platforms.kord.commands.KordCommandRegistry
import net.perfectdreams.discordinteraktions.webserver.DefaultInteractionRequestHandler
import net.perfectdreams.discordinteraktions.webserver.installDiscordInteractions
import org.apache.commons.lang3.time.StopWatch
import org.koin.core.context.GlobalContext
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Hana: SuspendAutoCloseable {
    companion object {
        val executorPool: ExecutorService = Executors.newFixedThreadPool(16, threadFactory("Hana-ExecutorPool"))
        val bootTime = StopWatch.createStarted()
    }

    private val routesRegistered = mutableListOf<Pair<String, HttpMethod>>()
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
                    "hana.environment" to config.environment.asName(),
                    "hana.commit.sha" to HanaInfo.COMMIT_HASH,
                    "hana.build.date" to HanaInfo.BUILD_DATE,
                    "hana.version" to HanaInfo.VERSION,
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
            CreateApiKeyCommand(koin.get())
        )

        commandManager.register(
            RevokeApiKeyCommandExecutor,
            RevokeApiKeyCommand()
        )

        commandManager.register(
            EditApiKeyCommandExecutor,
            EditApiKeyCommand()
        )

        commandManager.register(
            InfoOnApiKeyCommandExecutor,
            InfoOnApiKeyCommand()
        )

        registry.updateAllCommandsInGuild(Snowflake(824066105102303232))
        registry.updateAllCommandsInGuild(Snowflake(743698927039283201))

        log.info("* Launching HTTP service...")
        val environment = applicationEngineEnvironment {
            this.developmentMode = config.environment == Environment.Development
            this.log = LoggerFactory.getLogger("gay.floof.hana.server.ktor.Application")

            connector {
                host = config.host
                port = config.port
            }

            module {
                install(KtorBlockNsfwEndpoints)
                install(KtorRatelimitingPlugin)
                install(KtorLoggingPlugin)
                install(AutoHeadResponse)

                install(ContentNegotiation) {
                    json(GlobalContext.retrieve())
                }

                install(CORS) {
                    headers += "X-Forwarded-Proto"
                    anyHost()
                }

//                install(DefaultHeaders) {
//                    header("X-Powered-By", "Noel/Hana (+https://github.com/auguwu/hana; v${HanaInfo.VERSION})")
//                    header("Cache-Control", "public, max-age=7776000")
//
//                    if (config.server.securityHeaders) {
//                        header("X-Frame-Options", "deny")
//                        header("X-Content-Type-Options", "nosniff")
//                        header("X-XSS-Protection", "1; mode=block")
//                    }
//
//                    for ((key, value) in config.server.extraHeaders) {
//                        header(key, value)
//                    }
//                }

                install(StatusPages) {
                    status(HttpStatusCode.NotFound) { call, _ ->
                        call.respond(
                            HttpStatusCode.NotFound,
                            buildJsonObject {
                                put("success", false)
                                put(
                                    "errors",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("code", "UNKNOWN_ROUTE")
                                                put("message", "Route ${call.request.httpMethod.value} ${call.request.uri} was not found.")
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }

                    exception<Exception> { call, cause ->
                        if (Sentry.isEnabled()) {
                            Sentry.captureException(cause)
                        }

                        this@Hana.log.error("Unable to handle request ${call.request.httpMethod.value} ${call.request.path()}:", cause)

                        // If a response was already sent, let's not send a new one.
                        if (call.isHandled) return@exception

                        call.respond(
                            HttpStatusCode.InternalServerError,
                            buildJsonObject {
                                put("success", false)
                                put(
                                    "errors",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("code", "INTERNAL_SERVER_ERROR")
                                                put(
                                                    "message",
                                                    if (config.environment == Environment.Development) {
                                                        "Unknown error has occurred: ${cause.message}"
                                                    } else {
                                                        "Unknown error has occurred, please report it to Noel -- https://discord.gg/ATmjFH9kMH under #hana-support"
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                }

                routing {
                    val endpoints = koin.getAll<AbstractEndpoint>()
                    this@Hana.log.info("Found ${endpoints.size} endpoints to register.")

                    for (endpoint in endpoints) {
                        val pathMethodPair = Pair(endpoint.path, endpoint.method)
                        if (routesRegistered.contains(pathMethodPair))
                            continue

                        routesRegistered.add(pathMethodPair)
                        this@Hana.log.info("Registering endpoint ${endpoint.method.value} ${endpoint.path}")
                        route(endpoint.path, endpoint.method) {
                            handle {
                                val metrics: MetricsHandler by inject()
                                metrics.requestsCount?.labels(endpoint.path, endpoint.method.value)?.inc()

                                endpoint.call(call)
                            }
                        }
                    }

                    this@Hana.log.info("Installing Discord interactions handler...")
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

        val ratelimiter: Ratelimiter by inject()
        try {
            ratelimiter.close()
        } catch (e: Throwable) {
            // don't do anything
        }

        server.stop(1, 5, TimeUnit.SECONDS)
    }
}
