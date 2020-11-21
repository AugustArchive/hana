package dev.floofy.api.core

import io.sentry.Sentry as SentryCore
import dev.floofy.api.data.Config
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
