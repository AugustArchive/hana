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

package dev.floofy.api_old.core

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors

fun streamToString(stream: InputStream): String {
    val reader = BufferedReader(InputStreamReader(stream))
    return reader.lines().collect(Collectors.joining(System.lineSeparator()))
}

object YiffUtil {
    /**
     * Returns a random image of Yiff
     * @returns The randomized file or `null` if it's empty
     */
    fun image(path: String): File? =
        this.images(path)?.random()

    /**
     * Returns a list of Yiff images availabke
     * @returns The randomized file or `null` if it's empty
     */
    fun images(path: String): List<File>? {
        val images = File(path)
        val files = mutableListOf<File>()
        val list = images.listFiles() ?: emptyArray()

        if (list.isEmpty()) return null

        for (i in list) {
            if (i.isDirectory) continue
            files.add(i)
        }

        return files
    }

    /**
     * Returns a [JsonObject] of all the sources available
     */
    fun sources(): JsonObject = try {
        val strings = streamToString(this::class.java.getResourceAsStream("/yiff/sources.json"))
        JsonObject(strings)
    } catch (ex: Exception) {
        JsonObject()
    }

    /**
     * Returns a [JsonObject] of all tags available
     */
    fun tags(): JsonObject = try {
        val strings = streamToString(this::class.java.getResourceAsStream("/yiff/tags.json"))
        JsonObject(strings)
    } catch (ex: Exception) {
        JsonObject()
    }

    /**
     * Gets the sources by the path
     * @param path The path to find the source(s) from
     */
    fun source(path: String): JsonArray = try {
        val all = sources()
        all.getJsonArray(path)
    } catch (ex: Exception) {
        JsonArray()
    }

    /**
     * Gets the tags by the path
     * @param path The path to get all tags from
     */
    fun tag(path: String): JsonObject = try {
        val all = tags()
        all.getJsonObject(path)
    } catch (ex: Exception) {
        JsonObject()
    }
}
