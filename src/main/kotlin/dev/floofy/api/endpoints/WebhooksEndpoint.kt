package dev.floofy.api.endpoints

import dev.floofy.api.core.Endpoint
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

class WebhooksEndpoint: Endpoint(HttpMethod.GET, "/webhooks") {
    override fun run(ctx: RoutingContext) {

    }
}

class GitHubWebhooksEndpoint: Endpoint(HttpMethod.POST, "/webhooks/github") {
    override fun run(ctx: RoutingContext) {

    }
}

class SponsorsWebhookEndpoint: Endpoint(HttpMethod.POST, "/webhooks/github/sponsors") {
    override fun run(ctx: RoutingContext) {

    }
}

class SentryWebhookEndpoint: Endpoint(HttpMethod.POST, "/webhooks/sentry") {
    override fun run(ctx: RoutingContext) {

    }
}
