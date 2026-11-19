package io.github.freewebmovement.android.noui

import android.content.Context
import android.util.Log
import io.github.freewebmovement.fwmc.R
import java.io.File

/**
 * Seeds the FWMC root directory with the default public seed list.
 *
 * The bundled [R.raw.seeds] resource is copied to `<fwmc root>/seeds.txt`
 * only when the file is missing or empty, so user edits are never overwritten.
 * The Rust core reads that file on node start and imports it into SQLite.
 */
object SeedList {
    private const val TAG = "SeedList"
    private const val FILE_NAME = "seeds.txt"

    fun ensure(context: Context, rootDir: File) {
        runCatching {
            if (!rootDir.exists()) rootDir.mkdirs()
            val target = File(rootDir, FILE_NAME)
            if (target.exists() && target.length() > 0L) return
            context.resources.openRawResource(R.raw.seeds).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            Log.i(TAG, "Seeded default seed list: ${target.absolutePath}")
        }.onFailure { Log.w(TAG, "Failed to seed default seed list", it) }
    }
}
