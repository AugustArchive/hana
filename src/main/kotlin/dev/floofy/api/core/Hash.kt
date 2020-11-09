package dev.floofy.api.core

import com.google.common.hash.Hashing

object Hash {
    fun validateGitHubSignature(key: String, signature: String): Boolean {
        val value = Hashing
                .hmacSha256(key.toByteArray())
                .newHasher()
                .putString(signature, Charsets.UTF_8)
                .hash()
                .toString()

        return signature == value
    }
}
