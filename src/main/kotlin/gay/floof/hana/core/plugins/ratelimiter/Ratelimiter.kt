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

package gay.floof.hana.core.plugins.ratelimiter

import gay.floof.hana.core.managers.JwtManager
import gay.floof.hana.core.managers.RedisManager
import gay.floof.utils.slf4j.logging
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.time.StopWatch
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@kotlinx.serialization.Serializable
data class Ratelimit(
    val remaining: Int,
    val limit: Int,
    val resetTime: Instant,
    val isTokenBased: Boolean = false,
    val isImageManipulation: Boolean = false
) {
    val exceeded: Boolean
        get() = !this.expired && this.remaining == 0

    val expired: Boolean
        get() = resetTime <= Clock.System.now()

    fun consume(): Ratelimit = copy(remaining = (remaining - 1).coerceAtLeast(0))
}

@OptIn(DelicateCoroutinesApi::class)
class Ratelimiter(
    private val redis: RedisManager,
    private val json: Json,
    private val jwt: JwtManager
) {
    private val purgeOldJob: Job
    private val log by logging<Ratelimiter>()
    private val purgeMutex = Mutex()
    private val cached = mutableMapOf<String, Ratelimit>()

    init {
        val watch = StopWatch.createStarted()
        val count = redis.commands.hlen("hana:ratelimits").get()
        watch.stop()

        log.info("Took ${watch.getTime(TimeUnit.MILLISECONDS)}ms to retrieve $count ratelimits! Now re-ordering...")
        watch.reset()
        watch.start()

        val result = redis.commands.hgetall("hana:ratelimits").get() as Map<String, String>
        for ((key, value) in result) {
            val ratelimit = json.decodeFromString<Ratelimit>(value)
            cached[key] = ratelimit
        }

        watch.stop()
        log.info("Took ${watch.getTime(TimeUnit.MILLISECONDS)}ms to re-order cached ratelimits in-memory.")

        // Clear the expired ratelimits
        GlobalScope.launch {
            val locked = purgeMutex.tryLock()
            if (locked) {
                try {
                    purgeOld()
                } finally {
                    purgeMutex.unlock()
                }
            }
        }

        purgeOldJob = GlobalScope.launch {
            log.warn("Created purging old ratelimits job...")

            delay(1.minutes.inWholeMilliseconds)
            while (isActive) {
                val locked = purgeMutex.tryLock()
                if (locked) {
                    try {
                        purgeOld()
                    } finally {
                        purgeMutex.unlock()
                    }
                }

                delay(1.minutes.inWholeMilliseconds)
            }
        }
    }

    private suspend fun purgeOld() {
        log.info("Finding expired ratelimits...")

        val ratelimits = cached.filter { it.value.expired }
        log.info("Found ${ratelimits.size} ratelimits to purge!")

        for (key in ratelimits.keys) {
            redis.commands.hdel("hana:ratelimits", key).await()
            cached.remove(key)
        }
    }

    // https://github.com/go-chi/httprate/blob/master/httprate.go#L25-L47
    private fun getRealHost(call: ApplicationCall): String {
        val headers = call.request.headers

        val ip: String
        if (headers.contains("True-Client-IP")) {
            ip = headers["True-Client-IP"]!!
        } else if (headers.contains("X-Real-IP")) {
            ip = headers["X-Real-IP"]!!
        } else if (headers.contains(HttpHeaders.XForwardedFor)) {
            var index = headers[HttpHeaders.XForwardedFor]!!.indexOf(", ")
            if (index != -1) {
                index = headers[HttpHeaders.XForwardedFor]!!.length
            }

            ip = headers[HttpHeaders.XForwardedFor]!!.slice(0..index)
        } else {
            ip = call.request.origin.remoteHost
        }

        return ip
    }

    suspend fun get(call: ApplicationCall): Ratelimit {
        val authorization = call.request.header("Authorization")
        var limit = 1200
        var remaining = 1200
        var resetTime = Clock.System.now().plus(1.hours)
        var isImageMani = false
        var key = getRealHost(call)

        if (authorization != null) {
            // Check if the path is any image manipulation endpoints
            val routeRegex = "\\/api\\/?(\\/v\\d)?\\/(manipulation|yiff|kadi|sponsors)?".toRegex().toPattern().matcher(call.request.uri) as Matcher
            if (routeRegex.matches()) {
                when (routeRegex.group(2)) {
                    "manipulation" -> {
                        isImageMani = true
                        limit = 100
                        remaining = 100
                        resetTime = Clock.System.now().plus(15.minutes)
                    }

                    else -> {
                        limit = 2500
                        remaining = 2500
                    }
                }
            }

            // If it's not valid, then let's just use 1200 since
            // it's not a valid JWT. so no lax / stricter ratelimiting. :blep:
            if (!jwt.isValid(authorization)) {
                limit = 1200
                remaining = 1200
            } else {
                key = authorization
            }
        }

        var ratelimit = cached[key]
        if (ratelimit == null) {
            val r = Ratelimit(
                remaining,
                limit,
                resetTime,
                isTokenBased = key != getRealHost(call),
                isImageManipulation = isImageMani
            )

            ratelimit = r
            cached[key] = r
            redis.commands.hmset(
                "hana:ratelimits",
                mapOf(
                    key to json.encodeToString(Ratelimit.serializer(), r)
                )
            )
        }

        val newRl = ratelimit.consume()
        cached[key] = newRl
        redis.commands.hmset(
            "hana:ratelimits",
            mapOf(
                key to json.encodeToString(Ratelimit.serializer(), newRl)
            )
        )

        return newRl
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun close() {
        log.warn("Told to close off ratelimiter!")

        // weird compiler error that i have to cast this
        // but whatever...
        val mapped = cached.toMap() as Map<String, Any>

        // redo cache
        val newMap = mutableMapOf<String, String>()
        for ((key, value) in mapped) {
            newMap[key] = json.encodeToString(Ratelimit.serializer(), value as Ratelimit)
        }

        if (newMap.isNotEmpty()) {
            redis.commands.hmset("nino:ratelimits", newMap).await()
        }

        purgeOldJob.cancelAndJoin()
    }
}
