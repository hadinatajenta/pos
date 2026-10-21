package id.umkm.pos.pro.core.contract

interface PinHasher {
    fun hash(secret: String): String
    fun verify(secret: String, encodedHash: String): Boolean
}
