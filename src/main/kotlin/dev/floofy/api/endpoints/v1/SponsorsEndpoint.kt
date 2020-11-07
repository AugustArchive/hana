package dev.floofy.api.endpoints.v1

import dev.floofy.api.core.Endpoint
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

class SponsorsEndpoint: Endpoint(HttpMethod.GET, "/sponsors", 1) {
    override fun run(ctx: RoutingContext) {

    }
}

