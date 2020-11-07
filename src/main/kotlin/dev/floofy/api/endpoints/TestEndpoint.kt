package dev.floofy.api.endpoints

import dev.floofy.api.core.Endpoint
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

class TestEndpoint: Endpoint(HttpMethod.GET, "/", 2) {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        return res.setStatusCode(200).end("Hello, world!")
    }
}
