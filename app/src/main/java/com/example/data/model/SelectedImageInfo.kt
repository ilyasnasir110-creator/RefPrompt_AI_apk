package com.example.data.model

import android.net.Uri

data class SelectedImageInfo(
    val uri: Uri,
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeFormatted: String = "",
    val mimeType: String = "image/jpeg",
    val fileName: String = "image.jpg",
    val localCachedPath: String? = null
)
