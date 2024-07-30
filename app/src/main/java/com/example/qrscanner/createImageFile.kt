package com.example.qrscanner

import android.content.Intent
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun createImageFile(mainActivity: MainActivity): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
}

fun openImagePickerIntent(): Intent {
    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    return intent
}
