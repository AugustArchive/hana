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

package gay.floof.hana.routing.endpoints

import gay.floof.hana.routing.AbstractEndpoint
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

@kotlinx.serialization.Serializable
private data class MainResponse(
    val message: String,
    val docsUri: String
) {
    companion object {
        fun new(): MainResponse = MainResponse(
            message = "hello world!",
            docsUri = "https://api.floofy.dev/docs"
        )
    }
}

class MainEndpoint: AbstractEndpoint(method = HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) = call.respond(MainResponse.new())
}
