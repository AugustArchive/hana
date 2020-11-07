package dev.floofy.api.endpoints

import dev.floofy.api.core.Endpoint
import dev.floofy.api.data.Application
import dev.floofy.api.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class TestEndpoint(private val metadata: Application): Endpoint(HttpMethod.GET, "/", 2) {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        return res.setStatusCode(200).end(JsonObject().apply {
            put("hello", "world")
            put("version", metadata.version)
            put("commit", metadata.commit)
        })
    }
}
