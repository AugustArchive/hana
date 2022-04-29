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

package gay.floof.hana.core.s3

import gay.floof.hana.data.HanaConfig
import gay.floof.utils.slf4j.logging
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import java.net.URI

class S3Service(config: HanaConfig) {
    private val images = mutableMapOf<String, MutableList<String>>()

    private val log by logging<S3Service>()
    private val client: S3Client

    init {
        log.info("Configuring S3 client...")

        val builder = S3Client.builder()
            .region(Region.of(config.s3.region))

        if (config.s3.enforceNewPathStyle)
            builder.serviceConfiguration {
                it.pathStyleAccessEnabled(true)
            }

        if (config.s3.secretKey != null || config.s3.accessKey != null) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(object: AwsCredentials {
                    override fun accessKeyId(): String = config.s3.accessKey!!
                    override fun secretAccessKey(): String = config.s3.secretKey!!
                })
            )
        }

        if (config.s3.endpoint != null) {
            builder.endpointOverride(URI.create(config.s3.endpoint))
        }

        client = builder.build()

        log.info("Constructed S3 client, now initializing image cache...")
        var request = ListObjectsV2Request.builder()
            .bucket(config.s3.bucket)
            .build()

        while (true) {
            val objects = client.listObjectsV2(request)
            for (content in objects.contents()) {
                val isDir = content.key().split("").last() == "/"
                if (isDir) continue

                val key = content.key()
                when (key.split("/").first()) {
                    "yiff" -> {
                        if (!images.containsKey("yiff"))
                            images["yiff"] = mutableListOf()

                        images["yiff"]!!.add("https://cdn.floofy.dev/$key")
                    }

                    "polarbois" -> {
                        if (!images.containsKey("polarbois"))
                            images["polarbois"] = mutableListOf()

                        images["polarbois"]!!.add("https://cdn.floofy.dev/$key")
                    }

                    "wahs" -> {
                        if (!images.containsKey("wahs"))
                            images["wahs"] = mutableListOf()

                        images["wahs"]!!.add("https://cdn.floofy.dev/$key")
                    }
                }
            }

            if (objects.nextContinuationToken() == null) {
                break
            }

            request = request.toBuilder()
                .continuationToken(objects.nextContinuationToken())
                .build()
        }

        log.info("Finished image cache. :D")
    }

    fun getObjects(parent: String): List<String> = images[parent] ?: listOf()
}
