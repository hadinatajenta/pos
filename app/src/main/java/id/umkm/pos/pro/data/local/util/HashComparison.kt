package id.umkm.pos.pro.data.local.util

import java.security.MessageDigest

internal fun String.matchesHash(candidateHash: String): Boolean =
    MessageDigest.isEqual(toByteArray(Charsets.UTF_8), candidateHash.toByteArray(Charsets.UTF_8))
