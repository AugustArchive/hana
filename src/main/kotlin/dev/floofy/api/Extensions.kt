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

package dev.floofy.api

import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject

import java.util.Properties
import java.io.InputStream

/**
 * Inline function to load a properties file using an [InputStream]
 * @param stream The stream to use
 */
fun loadProperties(stream: InputStream): Properties = Properties().apply { load(stream) }

/**
 * Inline function to create a [java.lang.Thread] thread.
 * @param name The name of the thread
 * @param block The chunk of code to run when it's executed
 */
fun createThread(name: String, block: () -> Unit): Thread = (object: Thread(name) {
    override fun run() { block() }
})

/**
 * Extension to pass in [JsonObject] as a parameter and converted
 * to a string from [HttpServerResponse].
 */
fun HttpServerResponse.end(json: JsonObject): Unit = this.end(json.toString())
