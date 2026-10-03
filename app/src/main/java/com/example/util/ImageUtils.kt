package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.SelectedImageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

object ImageUtils {

    suspend fun processSelectedUri(context: Context, uri: Uri): SelectedImageInfo = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var fileName = "reference_image.jpg"
        var fileSize: Long = 0

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: "reference_image.jpg"
                }
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

        // Cache image locally into app cache
        val cacheDir = File(context.cacheDir, "selected_images")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val localFile = File(cacheDir, "ref_${System.currentTimeMillis()}_$fileName")

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(localFile).use { output ->
                input.copyTo(output)
            }
        }

        if (fileSize <= 0 && localFile.exists()) {
            fileSize = localFile.length()
        }

        // Get dimensions
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(localFile.absolutePath, options)
        val width = options.outWidth
        val height = options.outHeight

        SelectedImageInfo(
            uri = uri,
            width = width,
            height = height,
            fileSizeFormatted = formatFileSize(fileSize),
            mimeType = mimeType,
            fileName = fileName,
            localCachedPath = localFile.absolutePath
        )
    }

    suspend fun saveImageForHistory(context: Context, sourceFilePath: String): String = withContext(Dispatchers.IO) {
        val historyDir = File(context.filesDir, "history_images")
        if (!historyDir.exists()) historyDir.mkdirs()

        val destFile = File(historyDir, "hist_${System.currentTimeMillis()}.jpg")
        val sourceFile = File(sourceFilePath)

        if (sourceFile.exists()) {
            // Compress and create optimized copy
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            if (bitmap != null) {
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                return@withContext destFile.absolutePath
            } else {
                sourceFile.copyTo(destFile, overwrite = true)
                return@withContext destFile.absolutePath
            }
        }
        sourceFilePath
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "Unknown size"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val group = digitGroups.coerceIn(0, units.size - 1)
        val df = DecimalFormat("#,##0.#")
        return "${df.format(size / Math.pow(1024.0, group.toDouble()))} ${units[group]}"
    }
}
