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

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
import aws.sdk.kotlin.runtime.endpoint.CredentialScope
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListObjectsRequest
import gay.floof.hana.data.HanaConfig
import gay.floof.utils.slf4j.logging
import kotlinx.coroutines.runBlocking

class S3Service(config: HanaConfig) {
    private val images = mutableMapOf<String, List<String>>()

    private val log by logging<S3Service>()
    private val client: S3Client = S3Client.invoke {
        credentialsProvider = if (config.s3.accessKey == null && config.s3.secretKey == null) {
            DefaultChainCredentialsProvider()
        } else {
            StaticCredentialsProvider(
                Credentials(
                    accessKeyId = config.s3.accessKey!!,
                    secretAccessKey = config.s3.secretKey!!
                )
            )
        }

        region = config.s3.region
        endpointResolver = if (config.s3.endpoint != null) {
            AwsEndpointResolver { _, _ ->
                AwsEndpoint(config.s3.endpoint)
            }
        } else {
            null
        }
    }

    init {
        log.info("* Initializing image cache...")

        runBlocking {
            val request = ListObjectsRequest {
                bucket = config.s3.bucket
            }

            val response = client.listObjects(request)
            if (response.contents == null)
                throw IllegalStateException("Unable to retrieve objects from S3!")

            // Retrieve all polar bois
            val polarbois = response.contents!!.filter { it.key!!.contains("polarbois/") }
            val yiff = response.contents!!.filter { it.key!!.contains("yiff/") }
            val wahs = response.contents!!.filter { it.key!!.contains("wahs/") }
            val kadi = response.contents!!.filter { it.key!!.contains("kadi/") }

            images["polarbois"] = polarbois.map { "https://cdn.floofy.dev/${it.key}" }
            images["yiff"] = yiff.map { "https://cdn.floofy.dev/${it.key}" }
            images["wahs"] = wahs.map { "https://cdn.floofy.dev/${it.key}" }
            images["kadi"] = kadi.map { "https://cdn.floofy.dev/${it.key}" }

            log.info("Done!")
        }
    }

    fun getObjects(parent: String): List<String> = images[parent] ?: listOf()
}
