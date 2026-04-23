package com.reader.vellum.util

import java.security.MessageDigest

object BookIdentity {
    fun stableBookId(uriString: String): String = uriString

    fun filesystemKey(uriString: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uriString.toByteArray())
        return buildString(digest.size * 2) {
            digest.forEach { byte -> append("%02x".format(byte)) }
        }
    }
}
