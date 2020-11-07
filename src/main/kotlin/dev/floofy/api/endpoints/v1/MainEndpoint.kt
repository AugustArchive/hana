package dev.floofy.api.endpoints.v1

import dev.floofy.api.core.Endpoint
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

class MainEndpoint: Endpoint(HttpMethod.GET, "/", 1) {
    override fun run(ctx: RoutingContext) {

    }
}

