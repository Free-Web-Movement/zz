package io.github.freewebmovement.zz.bussiness

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import io.github.freewebmovement.zz.system.net.PeerServer
import java.io.File

class Share(private var context: Context) {
    fun apk(file: File) {
        val uri =
            FileProvider.getUriForFile(context, context.packageName+".provider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setType("*/*")
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun myApk(): File {
        // get the base.apk
        val baseApkLocation =
            PeerServer.app.applicationContext.packageManager.getApplicationInfo(
                PeerServer.app.applicationContext.packageName,
                PackageManager.GET_META_DATA
            ).sourceDir
        // get the file
        return File(baseApkLocation)
    }
    fun downloadDir(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)!!
    }
}
