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

import gay.floof.hana.core.plugins.ratelimiter.Ratelimiter
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.*
import kotlinx.datetime.Clock
import org.koin.core.context.GlobalContext

private data class RatelimitedResponse(
    val message: String,
    val success: Boolean
)

class KtorRatelimitingPlugin(val ratelimiter: Ratelimiter) {
    companion object: ApplicationFeature<ApplicationCallPipeline, Unit, KtorRatelimitingPlugin> {
        override val key: AttributeKey<KtorRatelimitingPlugin> = AttributeKey("KtorRatelimitingPlugin")
        override fun install(pipeline: ApplicationCallPipeline, configure: Unit.() -> Unit): KtorRatelimitingPlugin {
            val koin = GlobalContext.get()
            val ratelimiter = Ratelimiter(koin.get(), koin.get(), koin.get())

            pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) {
                val ip = this.call.request.origin.remoteHost
                if (ip == "0:0:0:0:0:0:0:1") {
                    proceed()
                    return@intercept
                }

                val record = ratelimiter.get(call)
                context.response.header("X-RateLimit-Limit", record.limit)
                context.response.header("X-RateLimit-Remaining", record.remaining)
                context.response.header("X-RateLimit-Reset", record.resetTime.toEpochMilliseconds())
                context.response.header("X-RateLimit-Reset-Date", record.resetTime.toString())

                if (record.exceeded) {
                    val retryAfter = (record.resetTime.epochSeconds - Clock.System.now().epochSeconds).coerceAtLeast(0)
                    context.response.header(HttpHeaders.RetryAfter, retryAfter)
                    context.respond(
                        HttpStatusCode.TooManyRequests,
                        RatelimitedResponse(
                            success = false,
                            message = "You have been ratelimited! ((ヾ(≧皿≦；)ノ＿))Fuuuuuu—-！"
                        )
                    )

                    finish()
                } else {
                    proceed()
                }
            }

            return KtorRatelimitingPlugin(ratelimiter)
        }
    }
}
