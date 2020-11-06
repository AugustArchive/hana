package dev.floofy.api.endpoints

import dev.floofy.api.core.Endpoint
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

class SponsorsEndpoint: Endpoint(HttpMethod.GET, "/sponsors") {
    override fun run(ctx: RoutingContext) {

    }
}

class ListSponsorsEndpoint: Endpoint(HttpMethod.GET, "/sponsors/:login") {
    override fun run(ctx: RoutingContext) {

    }
}
