package com.example.qrscanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage

fun getImageFromUri(
    context: Context,
    uri: Uri,
): InputImage = InputImage.fromFilePath(context, uri)
