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
import dev.floofy.api.core.Image
import dev.floofy.api.core.YiffUtil
import dev.floofy.api.data.Config
import dev.floofy.api.end
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import java.io.IOException

class YiffEndpoint(private val config: Config): Endpoint(HttpMethod.GET, "/yiff/random") {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        val yiff = YiffUtil.image("${config.imagesPath}/yiff")
        if (yiff == null) {
            res.setStatusCode(404).end(JsonObject().apply {
                put("message", "No images were found.")
            })

            return
        }

        res.setStatusCode(200).sendFile(yiff.canonicalPath)
        return
    }
}

class RandomYiffEndpoint(private val config: Config): Endpoint(HttpMethod.GET, "/yiff") {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        val file = YiffUtil.image("${config.imagesPath}/yiff")
        if (file == null) {
            res.setStatusCode(404).end(JsonObject().apply {
                put("message", "No images were found.")
            })

            return
        }

        val converter = Image(file)
        val dimensions = try {
            converter.dimensions()
        } catch (ex: IOException) {
            println(ex)
            null
        }

        val sources = YiffUtil.source(file.name)
        val tags = YiffUtil.tag(file.name)

        return res.setStatusCode(200).end(JsonObject().apply {
            put("characters", tags.getJsonArray("characters", JsonArray()))
            put("copyright", tags.getJsonArray("copyright", JsonArray()))
            put("sources", sources)
            put("artists", tags.getJsonArray("artists", JsonArray()))
            put("height", dimensions?.height ?: 0)
            put("width", dimensions?.width ?: 0)
            put("url", "https://cdn.floofy.dev/yiff/${file.name}")
        })
    }
}

class YiffStatsEndpoint(private val config: Config): Endpoint(HttpMethod.GET, "/yiff/stats") {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        val images = YiffUtil.images("${config.imagesPath}/yiff") ?: listOf()
        val sources = YiffUtil.sources()
        val tags = YiffUtil.tags()

        val hasSources = mutableListOf<String>()
        val hasTags = mutableListOf<String>()

        for (image in images) {
            val source = sources.getJsonArray(image.name)
            if (source != null) hasSources.add(image.name)

            val tag = tags.getJsonObject(image.name)
            if (tag != null) hasTags.add(image.name)
        }

        return res.setStatusCode(200).end(JsonObject().apply {
            put("has_sources", ((images.size / hasSources.size) / 100).toFloat())
            put("has_tags", ((images.size / hasTags.size) / 100).toFloat())
            put("sources", sources.size())
            put("images", images.size)
            put("tags", tags.size())
        })
    }
}
