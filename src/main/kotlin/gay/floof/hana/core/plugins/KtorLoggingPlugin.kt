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
import gay.floof.utils.slf4j.logging
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.prometheus.client.Histogram

class KtorLoggingPlugin {
    private val log by logging<KtorLoggingPlugin>()
    private val startTimePhase = PipelinePhase("StartTimePhase")
    private val logResponsePhase = PipelinePhase("LogResponsePhase")
    private val prometheusObserver = AttributeKey<Histogram.Timer>("PrometheusObserver")
    private val startTimeKey = AttributeKey<Long>("StartTimeKey")

    private fun install(pipeline: Application) {
        pipeline.environment.monitor.subscribe(ApplicationStarted) {
            log.info("Started HTTP service in ${System.currentTimeMillis() - Hana.bootTime}ms")
        }

        pipeline.environment.monitor.subscribe(ApplicationStopped) {
            log.info("HTTP service has completely stopped!")
        }

        pipeline.addPhase(startTimePhase)
        pipeline.intercept(startTimePhase) {
            call.attributes.put(startTimeKey, System.currentTimeMillis())
        }

        pipeline.intercept(ApplicationCallPipeline.Setup) {
            val metrics: MetricsHandler by inject()
            if (metrics.enabled) {
                val timer = metrics.requestLatency!!.startTimer()
                call.attributes.put(prometheusObserver, timer)
            }
        }

        pipeline.addPhase(logResponsePhase)
        pipeline.intercept(logResponsePhase) {
            logResponse(call)
        }
    }

    private fun logResponse(call: ApplicationCall) {
        val timeSpent = System.currentTimeMillis() - call.attributes[startTimeKey]
        val status = call.response.status() ?: HttpStatusCode.OK
        val timer = call.attributes.getOrNull(prometheusObserver)

        timer?.observeDuration()
        log.info("${status.value} ${status.description} :: ${call.request.httpMethod.value} ${call.request.path()} [${timeSpent}ms]")
    }

    companion object: ApplicationFeature<Application, Unit, KtorLoggingPlugin> {
        override val key: AttributeKey<KtorLoggingPlugin> = AttributeKey("KtorLoggingPlugin")
        override fun install(pipeline: Application, configure: Unit.() -> Unit): KtorLoggingPlugin =
            KtorLoggingPlugin().apply { install(pipeline) }
    }
}
