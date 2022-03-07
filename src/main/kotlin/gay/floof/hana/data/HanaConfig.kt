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

package gay.floof.hana.data

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
enum class Environment {
    @SerialName("production")
    Production,

    @SerialName("development")
    Development;

    fun asName(): String = when (this) {
        Development -> "development"
        Production -> "production"
    }
}

@kotlinx.serialization.Serializable
data class HanaConfig(
    val githubSecret: String? = null,
    val secretKeyBase: String,
    val environment: Environment = Environment.Development,
    val sentryDsn: String? = null,
    val publicKey: String,
    val database: PostgresConfig = PostgresConfig(),
    val instatus: InstatusConfig? = null,
    val server: KtorConfig = KtorConfig(),
    val redis: RedisConfig = RedisConfig(),
    val metrics: Boolean = true,
    val token: String,
    val port: Int = 9932,
    val host: String = "0.0.0.0",
    val s3: S3Config = S3Config()
)
