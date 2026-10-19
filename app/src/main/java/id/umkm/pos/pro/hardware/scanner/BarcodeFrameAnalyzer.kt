package id.umkm.pos.pro.hardware.scanner

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class BarcodeFrameAnalyzer(
    private val frameReader: BarcodeFrameReader,
    private val onCode: (String) -> Unit,
    private val throttleMillis: Long = DEFAULT_THROTTLE_MILLIS,
    private val voteCollector: BarcodeVoteCollector = BarcodeVoteCollector(),
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedAtMillis = 0L
    private var hasReported = false

    override fun analyze(image: ImageProxy) {
        try {
            if (hasReported) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastAnalyzedAtMillis < throttleMillis) return
            lastAnalyzedAtMillis = now

            val plane = image.planes.firstOrNull() ?: return
            val rowStride = plane.rowStride
            val luminance = LuminanceFrame.extract(plane.buffer, rowStride, image.width, image.height) ?: return

            val code = readUpright(luminance, rowStride, image.width, image.height, image.imageInfo.rotationDegrees)
                ?: return
            val confirmed = voteCollector.submit(code) ?: return
            hasReported = true
            onCode(confirmed)
        } finally {
            image.close()
        }
    }

    private fun readUpright(
        luminance: ByteArray,
        rowStride: Int,
        width: Int,
        height: Int,
        rotationDegrees: Int,
    ): String? {
        if (rotationDegrees != 90 && rotationDegrees != 270) {
            return frameReader.read(luminance, rowStride, width, height)
        }
        val rotated = LuminanceFrame.rotateQuarterTurn(
            luminance = luminance,
            rowStride = rowStride,
            width = width,
            height = height,
            clockwise = rotationDegrees == 90,
        ) ?: return null
        return frameReader.read(rotated.luminance, rotated.rowStride, rotated.width, rotated.height)
    }

    companion object {
        const val DEFAULT_THROTTLE_MILLIS = 100L
    }
}
