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

import java.awt.Dimension
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import kotlin.jvm.Throws

/**
 * Kotlin class to handle getting the image's dimensions
 * @credit https://stackoverflow.com/a/12164026
 */
class Image(val file: File) {
    @Throws(IOException::class)
    fun dimensions(): Dimension {
        val pos = file.name.lastIndexOf('.')
        if (pos == -1) throw IOException("No extension was available for file '${file.absolutePath}'")

        val suffix = file.name.substring(pos + 1)
        val iter = ImageIO.getImageReadersBySuffix(suffix)

        while (iter.hasNext()) {
            val reader = iter.next()
            try {
                val stream = FileImageInputStream(file)
                reader.input = stream

                val width = reader.getWidth(reader.minIndex)
                val height = reader.getHeight(reader.minIndex)
                return Dimension(width, height)
            } finally {
                reader.dispose()
            }
        }

        throw IOException("Not a known image file: '${file.absolutePath}'")
    }
}
