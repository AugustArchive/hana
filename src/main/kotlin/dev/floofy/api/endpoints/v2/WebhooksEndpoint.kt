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
package dev.floofy.api.endpoints.v2

import dev.floofy.api.core.Endpoint
import dev.floofy.api.core.Hash
import dev.floofy.api.data.Config
import dev.floofy.api.end
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class WebhooksEndpoint: Endpoint(HttpMethod.GET, "/webhooks") {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        return res.setStatusCode(200).end(JsonObject().apply {
            put("message", "Webhooks endpoint, mainly for showing embeds on Discord from GitHub and Sentry. This is an internal endpoint, how did you get here?")
        })
    }
}

class GitHubWebhooksEndpoint(
    private val config: Config
): Endpoint(HttpMethod.POST, "/webhooks/github") {
    override fun run(ctx: RoutingContext) {
        val req = ctx.request()
        val res = ctx.response()
        val body = ctx.bodyAsJson
        println(body)

        if (config.githubSecret == null) return res.setStatusCode(500).end(JsonObject().apply {
            put("message", "Missing `github_secret` key, did the runner add it?")
        })

        println(req.headers())
        val validated = Hash.validateGitHubSignature(config.githubSecret, req.getHeader("X-Hub-Signature-256"))
        println("Validated?: $validated")

        return res.setStatusCode(201).end()
    }
}

class SponsorsWebhookEndpoint: Endpoint(HttpMethod.POST, "/webhooks/github/sponsors") {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()

        return res.setStatusCode(201).end()
    }
}

class SentryWebhookEndpoint: Endpoint(HttpMethod.POST, "/webhooks/sentry") {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()

        return res.setStatusCode(201).end()
    }
}
