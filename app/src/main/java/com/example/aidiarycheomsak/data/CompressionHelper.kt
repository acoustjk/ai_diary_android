package com.example.aidiarycheomsak.data

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CompressionHelper {
    fun compress(data: String): String {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(data.toByteArray(StandardCharsets.UTF_8))
        }
        val compressedBytes = bos.toByteArray()
        return Base64.encodeToString(compressedBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun decompress(base64Str: String): String? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.URL_SAFE or Base64.NO_WRAP)
            val bis = ByteArrayInputStream(decodedBytes)
            val gzip = GZIPInputStream(bis)
            val bytes = gzip.readBytes()
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
