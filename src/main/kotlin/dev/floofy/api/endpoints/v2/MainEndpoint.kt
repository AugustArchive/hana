/**
 * Copyright (c) 2020-2021 August
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
import dev.floofy.api.data.Application
import dev.floofy.api.end
import dev.floofy.api.loadProperties
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class MainEndpoint: Endpoint(HttpMethod.GET, "/") {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        val stream = this::class.java.getResourceAsStream("/app.properties")
        val props = loadProperties(stream)

        val app = Application(
            version = props.getProperty("app.version", "v2.0.0"),
            commit = props.getProperty("app.commit", "unknown")
        )

        return res.setStatusCode(200).end(JsonObject().apply {
            put("hello", "world")
            put("commit", app.commit.slice(0..8))
            put("version", app.version)
        })
    }
}
