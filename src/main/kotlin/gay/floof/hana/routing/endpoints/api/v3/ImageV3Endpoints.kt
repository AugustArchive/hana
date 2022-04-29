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

package gay.floof.hana.routing.endpoints.api.v3

import gay.floof.hana.core.extensions.inject
import gay.floof.hana.core.s3.S3Service
import gay.floof.hana.routing.AbstractEndpoint
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

object ImageEndpointUtil {
    private val httpClient: HttpClient by inject()
    private val s3: S3Service by inject()

    suspend fun getImage(parent: String): JsonObject {
        val url = s3.getObjects(parent).random()

        // Get the image as a InputStream
        // TODO: possibly cache this?
        val res = httpClient.get(url)
        val stream = withContext(Dispatchers.IO) {
            ByteArrayInputStream(res.body())
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

        return buildJsonObject {
            put("success", true)
            put(
                "data",
                buildJsonObject {
                    put("artist", "unknown")
                    put("source", "unknown")
                    put("height", height)
                    put("width", width)
                    put("url", url)
                }
            )
        }
    }

    suspend fun sendImageFromBytes(call: ApplicationCall, parent: String) {
        val url = s3.getObjects(parent).random()

        // Get the image as a InputStream
        // TODO: possibly cache this?
        val res = httpClient.get(url)
        val content = res.body<ByteArray>()
        val stream = ByteArrayInputStream(content)

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

        val header = when (url.split('.').last()) {
            "png" -> ContentType.Image.PNG
            "jpg" -> ContentType.Image.JPEG
            "gif" -> ContentType.Image.GIF
            else -> ContentType.Image.Any
        }

        call.response.header("X-Hana-Image-Url", url)
        call.response.header("X-Hana-Image-Width", width)
        call.response.header("X-Hana-Image-Height", height)

        call.respondBytes(
            content,
            status = HttpStatusCode.OK,
            contentType = header
        )
    }
}

class YiffEndpoint: AbstractEndpoint("/yiff", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        call.respond(ImageEndpointUtil.getImage("yiff"))
    }
}

class YiffImageEndpoint: AbstractEndpoint("/yiff/random", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        ImageEndpointUtil.sendImageFromBytes(call, "yiff")
    }
}

// class YiffV3Endpoint: AbstractEndpoint("/v3/yiff", HttpMethod.Get) {
//    override suspend fun call(call: ApplicationCall) {
//        call.respond(ImageEndpointUtil.getImage("yiff"))
//    }
// }
//
// class YiffV3ImageEndpoint: AbstractEndpoint("/v3/yiff/random", HttpMethod.Get) {
//    override suspend fun call(call: ApplicationCall) {
//        ImageEndpointUtil.sendImageFromBytes(call, "yiff")
//    }
// }

class KadiEndpoint: AbstractEndpoint("/kadi", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        call.respond(ImageEndpointUtil.getImage("kadi"))
    }
}

class KadiImageEndpoint: AbstractEndpoint("/kadi/random", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        ImageEndpointUtil.sendImageFromBytes(call, "kadi")
    }
}

// class KadiV3Endpoint: AbstractEndpoint("/v3/kadi", HttpMethod.Get) {
//    override suspend fun call(call: ApplicationCall) {
//        call.respond(ImageEndpointUtil.getImage("kadi"))
//    }
// }
//
// class KadiV3ImageEndpoint: AbstractEndpoint("/v3/kadi/random", HttpMethod.Get) {
//    override suspend fun call(call: ApplicationCall) {
//        ImageEndpointUtil.sendImageFromBytes(call, "kadi")
//    }
// }

class WahsEndpoint: AbstractEndpoint("/wah", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        call.respond(ImageEndpointUtil.getImage("wahs"))
    }
}

class WahsImageEndpoint: AbstractEndpoint("/wah/random", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        ImageEndpointUtil.sendImageFromBytes(call, "wahs")
    }
}

// class WahsV3Endpoint: AbstractEndpoint("/v3/wah", HttpMethod.Get) {
//    override suspend fun call(call: ApplicationCall) {
//        call.respond(ImageEndpointUtil.getImage("wahs"))
//    }
// }
//
// class WahsV3ImageEndpoint: AbstractEndpoint("/v3/wah/random", HttpMethod.Get) {
//    override suspend fun call(call: ApplicationCall) {
//        ImageEndpointUtil.sendImageFromBytes(call, "wahs")
//    }
// }
