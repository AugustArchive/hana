/**
 * Copyright (c) 2020-2021 August
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
package dev.floofy.api.core

import dev.floofy.api.data.Config
import io.sentry.Sentry as SentryCore
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents a "class" instance of the API's Sentry instance
 * This is used for catching errors ahead of time.
 */
class Sentry(private val config: Config) {
    /**
     * The logger instance
     */
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * If sentry is enabled
     */
    private val enabled: Boolean = config.sentryDSN != null

    fun install() {
        if (!enabled) {
            logger.warn("Sentry is not enabled in this instance, this is optional yet recommended.")
            return
        }

        logger.info("Installing Sentry...")
        SentryCore.init {
            it.dsn = config.sentryDSN
        }

        SentryCore.configureScope { scope ->
            scope.setTag("project.versions.kotlin", KotlinVersion.CURRENT.toString())
            scope.setTag("project.versions.java", System.getProperty("java.version", "Unknown"))
        }

        logger.info("Successfully installed Sentry, maybe? I don't know, I assume you did it correct... which I doubt.")
    }

    fun report(ex: Throwable) {
        if (!enabled) return

        SentryCore.captureException(ex)
    }
}
