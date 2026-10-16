package id.umkm.pos.pro.domain.pos

import kotlin.math.floor

fun roundHalfUp(value: Double): Long = floor(value + 0.5).toLong()

fun floorToWhole(value: Double): Long = floor(value).toLong()
