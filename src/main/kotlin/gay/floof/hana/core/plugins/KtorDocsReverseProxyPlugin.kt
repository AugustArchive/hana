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

package gay.floof.hana.core.plugins

import gay.floof.hana.core.extensions.inject
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.content.TextContent
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.utils.io.*

class KtorDocsReverseProxyPlugin {
    companion object: ApplicationFeature<ApplicationCallPipeline, Unit, KtorDocsReverseProxyPlugin> {
        override val key: AttributeKey<KtorDocsReverseProxyPlugin> = AttributeKey("KtorDocsReverseProxyPlugin")
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Unit.() -> Unit
        ): KtorDocsReverseProxyPlugin {
            val client: HttpClient by inject()

            pipeline.intercept(ApplicationCallPipeline.Call) {
                if (!context.request.path().startsWith("/docs")) {
                    proceed()
                    return@intercept
                }

                val response: HttpResponse = try {
                    client.request("https://auguwu.github.io/hana${call.request.uri.removePrefix("/docs")}")
                } catch (e: ClientRequestException) {
                    e.response
                }

                val headers = response.headers
                val location = headers[HttpHeaders.Location]
                val contentType = headers[HttpHeaders.ContentType]
                val contentLength = headers[HttpHeaders.ContentLength]

                if (location != null) {
                    call.response.header(HttpHeaders.Location, "https://auguwu.github.io/hana${call.request.uri.removePrefix("/docs")}")
                }

                when {
                    contentType?.startsWith("text/html") == true -> {
                        val text = response.receive<String>()
                        val filteredText = text.replace("https://auguwu.github.io/hana", "https://api.floofy.dev${call.request.uri.removePrefix("/docs")}")

                        call.respond(TextContent(filteredText, ContentType.Text.Html.withCharset(Charsets.UTF_8), response.status))
                    }

                    else -> {
                        call.respond(object: OutgoingContent.WriteChannelContent() {
                            override val contentLength: Long? = contentLength?.toLong()
                            override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
                            override val headers: Headers = Headers.build {
                                appendAll(headers.filter { key, _ -> !key.equals(HttpHeaders.ContentType, ignoreCase = true) && !key.equals(HttpHeaders.ContentLength, ignoreCase = true) })
                            }
                            override val status: HttpStatusCode? = response.status
                            override suspend fun writeTo(channel: ByteWriteChannel) {
                                response.content.copyAndClose(channel)
                            }
                        })
                    }
                }
            }

            return KtorDocsReverseProxyPlugin()
        }
    }
}
