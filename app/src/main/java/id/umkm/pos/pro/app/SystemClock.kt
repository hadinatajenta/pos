package id.umkm.pos.pro.app

import id.umkm.pos.pro.core.contract.Clock

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
