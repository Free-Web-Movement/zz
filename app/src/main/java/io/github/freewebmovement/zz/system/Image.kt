package io.github.freewebmovement.zz.system

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import android.util.Base64


class Image {
    companion object {
        @Suppress("DEPRECATION")
        private fun toBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap {
            val bitmap = when {
                Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                    contentResolver,
                    uri
                )

                else -> {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
            }
            return bitmap
        }

        fun toBase64(context: Context, uri: Uri): String {
            val bitmap = toBitmap(context.contentResolver, uri)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val bytes = stream.toByteArray()
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }
    }
}