/**
 * Copyright (c) 2020 August
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
package dev.floofy.api

import dev.floofy.api.core.Endpoint
import dev.floofy.api.data.Config
import dev.floofy.api.endpoints.NotFoundEndpoint
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class API: KoinComponent {
    private val health: HealthCheckHandler by inject()
    private val config: Config by inject()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val vertx: Vertx by inject()

    private fun onFailure(r: Endpoint, ctx: RoutingContext) {
        val throwable = ctx.failure()
        val res = ctx.response()

        logger.error("Unable to run route \"${r.method} ${r.path}\"")
        println(throwable)

        val obj = JsonObject().apply {
            put("note", "If this error keeps occuring, please report it August#5820 on Discord or at https://t.me/auguwu if you prefer Telegram")
            put("error", throwable.localizedMessage)
        }

        res.setStatusCode(ctx.statusCode()).end(obj)
    }

    fun start() {
        // Set the current thread name
        Thread.currentThread().name = "API-MainThread"
        logger.info("Now loading up API...")

        // Create a global router to use
        val router = Router.router(vertx)
        val notFound = getKoin().get<NotFoundEndpoint>()

        // Set 404 handler to the not found endpoint
        router.errorHandler(404, notFound::run)
        router.errorHandler(405) { ctx ->
            val res = ctx.response()
            val req = ctx.request()

            return@errorHandler res.setStatusCode(405).end(JsonObject().apply {
                put("message", "Endpoint \"${req.rawMethod()} ${req.path()}\" didn't use a valid method.")
            })
        }

        router
                .route("/health")
                .handler(health)

        val routes = getKoin().getAll<Endpoint>().filter { it !is NotFoundEndpoint }
        val v1 = routes.filter { it.version == 1 }
        val v2 = routes.filter { it.version == 2 }
        val global = routes.filter { it.version == 0 }

        val v1Router = Router.router(vertx)
        val v2Router = Router.router(vertx)

        for (g in global) {
            logger.info("Found route \"${g.method} ${g.path}\" under global scope")
            router
                .route(g.method, g.path)
                .failureHandler { this.onFailure(g, it) }
                .blockingHandler({
                    it
                        .response()
                        .putHeader("Content-Type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .putHeader("Access-Control-Allow-Methods", "GET,POST")

                    g.run(it)
                }, false)
        }

        for (r in v1) {
            logger.info("Found route \"${r.method} ${r.path}\" under v1 scope")
            if (config.defaultAPIVersion == 1) {
                router
                        .route(r.method, r.path)
                        .failureHandler { ctx -> this.onFailure(r, ctx) }
                        .blockingHandler({
                            it
                                .response()
                                .putHeader("Content-Type", "application/json")
                                .putHeader("Access-Control-Allow-Origin", "*")
                                .putHeader("Access-Control-Allow-Methods", "GET,POST")

                            r.run(it)
                        }, false)
            }

            v1Router
                    .route(r.method, r.path)
                    .failureHandler { ctx -> this.onFailure(r, ctx) }
                    .blockingHandler({
                        it
                            .response()
                            .putHeader("Content-Type", "application/json")
                            .putHeader("Access-Control-Allow-Origin", "*")
                            .putHeader("Access-Control-Allow-Methods", "GET,POST")

                        r.run(it)
                    }, false)
        }

        for (r in v2) {
            logger.info("Found route \"${r.method} ${r.path}\" under v2 scope")
            if (config.defaultAPIVersion == 2) {
                router
                        .route(r.method, r.path)
                        .failureHandler { ctx -> this.onFailure(r, ctx) }
                        .blockingHandler({
                            it
                                .response()
                                .putHeader("Content-Type", "application/json")
                                .putHeader("Access-Control-Allow-Origin", "*")
                                .putHeader("Access-Control-Allow-Methods", "GET,POST")

                            r.run(it)
                        }, false)
            }

            v2Router
                    .route(r.method, r.path)
                    .failureHandler { ctx -> this.onFailure(r, ctx) }
                    .blockingHandler({
                        it
                            .response()
                            .putHeader("Content-Type", "application/json")
                            .putHeader("Access-Control-Allow-Origin", "*")
                            .putHeader("Access-Control-Allow-Methods", "GET,POST")

                        r.run(it)
                    }, false)
        }

        // Mount sub-routers for api.augu.dev/v2 for an example
        router
                .mountSubRouter("/v2", v2Router)
                .mountSubRouter("/v1", v1Router)

        val http = vertx.createHttpServer()
        http.requestHandler(router)
        http.listen(config.port)

        logger.info("API is now listening at http://localhost:${config.port}, default API version: v${config.defaultAPIVersion}")
    }

    fun destroy() {
        logger.info("Destroyed service.")
        vertx.close()
    }
}
