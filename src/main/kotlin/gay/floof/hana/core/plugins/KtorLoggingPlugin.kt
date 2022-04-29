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

package gay.floof.hana.core.plugins

import gay.floof.hana.core.Hana
import gay.floof.hana.core.extensions.inject
import gay.floof.hana.core.metrics.MetricsHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.util.*
import io.prometheus.client.Histogram
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.TimeUnit

val KtorLoggingPlugin = createApplicationPlugin("HanaLogging") {
    val prometheusObserver = AttributeKey<Histogram.Timer>("PrometheusObserver")
    val stopwatchKey = AttributeKey<StopWatch>("Stopwatch")
    val log = LoggerFactory.getLogger("gay.floof.hana.core.plugins.KtorLoggingPluginKt")
    val metrics by inject<MetricsHandler>()

    environment?.monitor?.subscribe(ApplicationStarted) {
        log.info("Started API in ${Hana.bootTime.getTime(TimeUnit.SECONDS)} seconds!")
    }

    environment?.monitor?.subscribe(ApplicationStopped) {
        log.warn("HTTP service has died :(")
    }

    on(CallSetup) { call ->
        MDC.put("user_agent", call.request.userAgent())

        call.attributes.put(stopwatchKey, StopWatch.createStarted())

        if (metrics.enabled) {
            call.attributes.put(prometheusObserver, metrics.requestLatency!!.startTimer())
        }
    }

    on(ResponseSent) { call ->
        MDC.remove("user_agent")

        val method = call.request.httpMethod
        val version = call.request.httpVersion
        val endpoint = call.request.uri
        val status = call.response.status() ?: HttpStatusCode(-1, "Unknown HTTP Method")
        val stopwatch = call.attributes[stopwatchKey]
        val userAgent = call.request.userAgent()
        val observer = call.attributes.getOrNull(prometheusObserver)

        stopwatch.stop()
        observer?.observeDuration()

        log.info("${method.value} $version $endpoint [$userAgent] :: ${status.value} ${status.description} [${stopwatch.getTime(TimeUnit.MILLISECONDS)}ms]")
    }
}
