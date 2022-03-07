/*
 * 🥀 hana: API to proxy different APIs like GitHub Sponsors, source code for api.floofy.dev
 * Copyright (c) 2020-2022 Noel <cutie@floofy.dev>
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

package gay.floof.hana.routing.endpoints.api.v2

import gay.floof.hana.core.s3.S3Service
import gay.floof.hana.routing.AbstractEndpoint
import gay.floof.hana.utils.toJsonPrimitive
import gay.floof.hana.utils.writeJson
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class YiffV2Endpoint(private val s3: S3Service, private val httpClient: HttpClient): AbstractEndpoint("/api/v2/yiff", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        val url = s3.getObjects("yiff").random()

        // Get the image as a InputStream
        // TODO: possibly cache this?
        val res: HttpResponse = httpClient.get(url)
        val stream = withContext(Dispatchers.IO) {
            ByteArrayInputStream(res.receive())
        }

        val streamImage = withContext(Dispatchers.IO) {
            ImageIO.createImageInputStream(stream)
        }

        val height: Int
        val width: Int

        if (streamImage == null) {
            height = 0
            width = 0
        } else {
            val formatReader = ImageIO.getImageWritersByFormatName(url.split('.').last()).next()
            val reader = ImageIO.getImageReader(formatReader)
            reader.setInput(streamImage, true)

            height = withContext(Dispatchers.IO) {
                reader.getHeight(0)
            }

            width = withContext(Dispatchers.IO) {
                reader.getWidth(0)
            }
        }

        call.writeJson(
            data = mapOf(
                "artist" to "unknown".toJsonPrimitive(),
                "source" to "unknown".toJsonPrimitive(),
                "height" to height.toJsonPrimitive(),
                "width" to width.toJsonPrimitive(),
                "url" to url.toJsonPrimitive()
            )
        )
    }
}

class YiffImageV2Endpoint(private val s3: S3Service, private val httpClient: HttpClient): AbstractEndpoint("/api/v2/yiff/random", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        val url = s3.getObjects("yiff").random()
        val res = httpClient.get<HttpResponse>(url)

        val content = withContext(Dispatchers.IO) {
            res.receive<ByteArray>()
        }

        val header = when (url.split('.').last()) {
            "png" -> ContentType.Image.PNG
            "jpg" -> ContentType.Image.JPEG
            "gif" -> ContentType.Image.GIF
            else -> ContentType.Image.Any
        }

        call.respondBytes(
            content,
            status = HttpStatusCode.OK,
            contentType = header
        )
    }
}

class KadiV2Endpoint(private val s3: S3Service, private val httpClient: HttpClient): AbstractEndpoint("/api/v2/kadi", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        val url = s3.getObjects("kadi").random()

        // Get the image as a InputStream
        // TODO: possibly cache this?
        val res: HttpResponse = httpClient.get(url)
        val stream = withContext(Dispatchers.IO) {
            ByteArrayInputStream(res.receive())
        }

        val streamImage = withContext(Dispatchers.IO) {
            ImageIO.createImageInputStream(stream)
        }

        val height: Int
        val width: Int

        if (streamImage == null) {
            height = 0
            width = 0
        } else {
            val formatReader = ImageIO.getImageWritersByFormatName(url.split('.').last()).next()
            val reader = ImageIO.getImageReader(formatReader)
            reader.setInput(streamImage, true)

            height = withContext(Dispatchers.IO) {
                reader.getHeight(0)
            }

            width = withContext(Dispatchers.IO) {
                reader.getWidth(0)
            }
        }

        call.writeJson(
            data = mapOf(
                "source" to "Noel - https://floofy.dev".toJsonPrimitive(),
                "height" to height.toJsonPrimitive(),
                "width" to width.toJsonPrimitive(),
                "url" to url.toJsonPrimitive()
            )
        )
    }
}

class KadiImageV2Endpoint(private val s3: S3Service, private val httpClient: HttpClient): AbstractEndpoint("/api/v2/kadi/random", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        val url = s3.getObjects("kadi").random()
        val res = httpClient.get<HttpResponse>(url)

        val content = withContext(Dispatchers.IO) {
            res.receive<ByteArray>()
        }

        val header = when (url.split('.').last()) {
            "png" -> ContentType.Image.PNG
            "jpg" -> ContentType.Image.JPEG
            "gif" -> ContentType.Image.GIF
            else -> ContentType.Image.Any
        }

        call.respondBytes(
            content,
            status = HttpStatusCode.OK,
            contentType = header
        )
    }
}
