package dev.floofy.api.endpoints

import dev.floofy.api.core.Endpoint
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

class MainEndpoint: Endpoint(HttpMethod.GET, "/") {
    override fun run(ctx: RoutingContext) {

    }
}
