package com.capstone.capstone.ar

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import com.google.ar.core.Frame
import java.io.ByteArrayOutputStream

/**
 * 工具类：将 ARCore 帧的相机图像（YUV_420_888）转换为 JPEG
 * - 自动处理 stride / pixelStride 差异
 * - 支持指定压缩质量
 */
object ImageUtils {

    /** 从 ARCore 帧提取 JPEG 字节数组 */
    fun acquireJpegFromFrame(frame: Frame, quality: Int = 80): ByteArray? {
        var image: Image? = null
        return try {
            image = frame.acquireCameraImage()
            if (image.format != ImageFormat.YUV_420_888) {
                Log.w("ImageUtils", "Unexpected image format: ${image.format}")
                null
            } else {
                val nv21 = yuv420888ToNv21(image)
                val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                ByteArrayOutputStream().use { baos ->
                    yuv.compressToJpeg(Rect(0, 0, image.width, image.height), quality.coerceIn(40, 95), baos)
                    baos.toByteArray()
                }
            }
        } catch (e: Exception) {
            Log.e("ImageUtils", "acquireCameraImage failed", e)
            null
        } finally {
            try { image?.close() } catch (_: Exception) {}
        }
    }

    /** YUV_420_888 → NV21 */
    private fun yuv420888ToNv21(image: Image): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        val out = ByteArray(ySize + uSize + vSize)

        var offset = 0
        offset += copyPlane(yPlane, image.width, image.height, out, offset, 1)

        val chromaWidth = image.width / 2
        val chromaHeight = image.height / 2
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixStride = uPlane.pixelStride
        val vPixStride = vPlane.pixelStride

        for (row in 0 until chromaHeight) {
            var uIndex = row * uRowStride
            var vIndex = row * vRowStride
            for (col in 0 until chromaWidth) {
                val v = vBuf.get(vIndex).toInt() and 0xFF
                val u = uBuf.get(uIndex).toInt() and 0xFF
                out[offset++] = v.toByte()
                out[offset++] = u.toByte()
                uIndex += uPixStride
                vIndex += vPixStride
            }
        }
        return out
    }

    /** 复制单通道平面数据（处理 stride 不规则情况） */
    private fun copyPlane(
        plane: Image.Plane,
        width: Int,
        height: Int,
        out: ByteArray,
        outOffset: Int,
        outPixelStride: Int
    ): Int {
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        var offset = outOffset
        val row = ByteArray(rowStride)

        for (i in 0 until height) {
            if (pixelStride == 1) {
                buffer.get(out, offset, width)
                offset += width
                if (i < height - 1) buffer.position(buffer.position() + rowStride - width)
            } else {
                val length = (width - 1) * pixelStride + 1
                buffer.get(row, 0, length)
                var j = 0
                while (j < width) {
                    out[offset] = row[j * pixelStride]
                    offset += outPixelStride
                    j++
                }
            }
        }
        return offset - outOffset
    }
}
