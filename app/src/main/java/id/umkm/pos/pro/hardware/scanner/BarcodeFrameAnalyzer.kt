package id.umkm.pos.pro.hardware.scanner

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class BarcodeFrameAnalyzer(
    private val frameReader: BarcodeFrameReader,
    private val onCode: (String) -> Unit,
    private val throttleMillis: Long = DEFAULT_THROTTLE_MILLIS,
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
            val height = image.height
            val luminance = LuminanceFrame.extract(plane.buffer, rowStride, image.width, height) ?: return

            val code = frameReader.read(luminance, rowStride, image.width, height) ?: return
            hasReported = true
            onCode(code)
        } finally {
            image.close()
        }
    }

    companion object {
        const val DEFAULT_THROTTLE_MILLIS = 150L
    }
}
