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

import gay.floof.hana.core.extensions.inject
import gay.floof.hana.core.plugins.ratelimiter.Ratelimiter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

val KtorRatelimitingPlugin = createApplicationPlugin("HanaRatelimitingPlugin") {
    val ratelimiter: Ratelimiter by inject()

    onCall { call ->
        // If it was already handled, let's not continue.
        if (call.isHandled)
            return@onCall

        val record = ratelimiter.get(call)
        call.response.header("X-RateLimit-Limit", record.limit)
        call.response.header("X-RateLimit-Reset", record.resetTime.toEpochMilliseconds())
        call.response.header("X-RateLimit-Remaining", record.remaining)
        call.response.header("X-RateLimit-Reset-Date", record.resetTime.toString())

        if (record.exceeded) {
            val retryAfter = (record.resetTime.epochSeconds - Clock.System.now().epochSeconds).coerceAtLeast(0)
            call.response.header(HttpHeaders.RetryAfter, retryAfter)
            call.respond(
                HttpStatusCode.TooManyRequests,
                buildJsonObject {
                    put("success", false)
                    putJsonArray("errors") {
                        add(
                            buildJsonObject {
                                put("code", "RATELIMITED")
                                put("message", "You have been ratelimited! ((ヾ(≧皿≦；)ノ＿))Fuuuuuu—-！")
                            }
                        )
                    }
                }
            )

            return@onCall
        }
    }
}
