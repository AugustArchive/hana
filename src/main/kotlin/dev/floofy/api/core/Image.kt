package dev.floofy.api.core

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
                Dimension(width, height)
            } finally {
                reader.dispose()
            }
        }

        throw IOException("Not a known image file: '${file.absolutePath}'")
    }
}
