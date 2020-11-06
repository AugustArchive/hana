package dev.floofy.api.core

import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

abstract class Endpoint(val method: HttpMethod, val path: String) {
    abstract fun run(ctx: RoutingContext)
}
