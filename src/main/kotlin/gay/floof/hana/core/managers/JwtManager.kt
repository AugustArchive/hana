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

package gay.floof.hana.core.managers

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import gay.floof.hana.core.database.asyncTransaction
import gay.floof.hana.core.database.tables.ApiKeyEntity
import gay.floof.hana.core.database.tables.ApiKeysTable
import gay.floof.hana.data.HanaConfig

/**
 * Represents management for creating and revoking API keys. API keys are stored in
 * PostgreSQL and will be validated through this management system.
 */
class JwtManager(config: HanaConfig) {
    private val algorithm = Algorithm.HMAC512(config.secretKeyBase)

    /**
     * Checks if the JWT [token] specified is a valid token.
     */
    suspend fun isValid(token: String): Boolean {
        // Check if the token is a valid JWT string
        val res = try {
            val verifier = JWT.require(algorithm)
                .withIssuer("noel/hana")
                .build()

            verifier.verify(token)
            true
        } catch (e: Exception) {
            false
        }

        if (res) return true

        // Check if any token exists in the database, if not,
        // let's return `false`
        asyncTransaction {
            ApiKeyEntity.find {
                ApiKeysTable.token eq token
            }.firstOrNull()
        } ?: return false

        return false
    }

    suspend fun create(
        id: Long,
        name: String,
        description: String = "no description provided",
        nsfwEnabled: Boolean = false,
        imEnabled: Boolean = false
    ): String {
        val permsToAdd = mutableListOf<String>()

        if (nsfwEnabled)
            permsToAdd.add("nsfw")

        if (imEnabled)
            permsToAdd.add("im")

        val token = JWT.create()
            .withIssuer("noel/hana")
            .withClaim("app_name", name)
            .withClaim("owner_id", id)
            .sign(algorithm)

        asyncTransaction {
            ApiKeyEntity.new(id) {
                this.name = name
                this.description = description
                this.token = token
                this.permissions = permsToAdd.joinToString("|")
            }
        }

        return token
    }
}
