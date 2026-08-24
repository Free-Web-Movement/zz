package io.github.freewebmovement.zz.ui.content.user

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import java.security.MessageDigest

private fun ByteArray.toHexDigest(): String =
    MessageDigest.getInstance("MD5").digest(this).joinToString("") { "%02x".format(it) }.take(10)

/** Android 侧头像显示缓存：<filesDir>/avatar_cache/<地址>.jpg，界面用 file:// URI 必定可读。 */
internal object AvatarLocalStore {

    private fun fileFor(ctx: Context, address: String): File {
        val safe = address.replace(Regex("[^A-Za-z0-9]"), "_").ifEmpty { "unknown" }
        return File(File(ctx.filesDir, "avatar_cache").apply { mkdirs() }, "$safe.jpg")
    }

    /** 保存 JPEG 字节并返回可显示的本地 URI。URI 附带内容指纹，避免 Coil 同路径命中旧缓存。 */
    fun saveJpeg(ctx: Context, address: String, jpeg: ByteArray): Uri {
        val f = fileFor(ctx, address)
        f.writeBytes(jpeg)
        return fingerprinted(f, jpeg)
    }

    private fun fingerprinted(f: File, jpeg: ByteArray): Uri =
        Uri.fromFile(f).buildUpon()
            .appendQueryParameter("v", jpeg.toHexDigest())
            .appendQueryParameter("t", System.currentTimeMillis().toString())
            .build()

    /** 将资料接口返回的 data:image base64 转成本地文件 URI；解码失败返回 null。 */
    fun fromDataUrl(ctx: Context, address: String, dataUrl: String): Uri? {
        val b64 = dataUrl.takeIf { it.startsWith("data:image") }?.substringAfter("base64,", "") ?: return null
        if (b64.isEmpty()) return null
        val bytes = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull() ?: return null
        return runCatching { saveJpeg(ctx, address, bytes) }.getOrNull()
    }
}
