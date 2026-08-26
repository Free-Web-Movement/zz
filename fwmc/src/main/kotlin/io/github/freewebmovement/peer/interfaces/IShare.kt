package io.github.freewebmovement.peer.interfaces

import java.io.File


interface IShare {
    fun apk(file: File)
    fun myApk(): File
    fun downloadDir(): File
}

