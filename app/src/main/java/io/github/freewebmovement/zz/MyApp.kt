package io.github.freewebmovement.zz

import android.content.Context
import android.net.Uri
import android.os.Environment
import io.github.freewebmovement.peer.IInstrumentedHandler
import io.github.freewebmovement.peer.PeerClient
import io.github.freewebmovement.peer.PeerServer
import io.github.freewebmovement.peer.database.AppDatabase
import io.github.freewebmovement.peer.interfaces.IApp
import io.github.freewebmovement.peer.interfaces.IPreference
import io.github.freewebmovement.peer.interfaces.IShare
import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.UserJSON
import io.github.freewebmovement.peer.system.Settings
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.zz.system.Image
import io.github.freewebmovement.zz.system.net.IPList
import kotlinx.coroutines.CoroutineScope
import java.io.File

class MyApp(private var context: Context): IApp {

    lateinit var ipList: IPList

    private lateinit var _preference: IPreference
    override var preference: IPreference
        get() = _preference
        set(value) {
            _preference = value
        }

    private lateinit var _scope: CoroutineScope

    override var scope: CoroutineScope
        get() = _scope
        set(value) {
            _scope = value
        }
    private lateinit var _share: IShare
    override var share: IShare
        get() = _share
        set(value) {
            _share = value
        }
    private lateinit var _crypto: Crypto
    override var crypto: Crypto
        get() = _crypto
        set(value) {
            _crypto = value
        }
    private lateinit var _handler: IInstrumentedHandler
    override var handler: IInstrumentedHandler
        get() = _handler
        set(value) {
            _handler = value
        }
    private lateinit var _client: PeerClient
    override var client: PeerClient
        get() = _client
        set(value) {
            _client = value
        }
     private lateinit var _server: PeerServer
    override var server: PeerServer
        get() = _server
        set(value) {
            _server = value
        }
     private lateinit var _db: AppDatabase
    override var db: AppDatabase
        get() = _db
        set(value) {
            _db = value
        }
    private lateinit var _settings: Settings
    override var settings: Settings
        get() = _settings
        set(value) {
            _settings = value
        }

    override fun setIpInfo(json: PublicKeyJSON) {
        json.ip = ipList.getPublicIP()
        json.port = settings.localServerPort
        json.type = ipList.getPublicType()
    }

    override fun getDownloadDir(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.packageName
        )
    }

    override fun getProfile(): UserJSON {
        val nickname = settings.mineProfileNickname
        val intro = settings.mineProfileIntro
        val imageUri = settings.mineProfileImageUri
        var avatar = ""
        if (imageUri != "") {
            val uri: Uri = Uri.parse(imageUri)
            avatar = Image.toBase64(context, uri)
        }
        return UserJSON(nickname = nickname, intro = intro, avatar = avatar)
    }
}