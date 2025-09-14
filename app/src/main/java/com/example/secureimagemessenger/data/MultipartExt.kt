// MultipartExt.kt
package com.example.secureimagemessenger.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

fun String.toTextBody(): RequestBody =
    this.toRequestBody("text/plain".toMediaType())

fun ByteArray.toPngBody(): RequestBody =
    this.toRequestBody("image/png".toMediaType())

fun ByteArray.toPngPart(partName: String, fileName: String): MultipartBody.Part =
    MultipartBody.Part.createFormData(partName, fileName, this.toPngBody())

fun File.toImagePart(partName: String, fileName: String = name): MultipartBody.Part =
    MultipartBody.Part.createFormData(partName, fileName, this.asRequestBody("image/*".toMediaType()))
