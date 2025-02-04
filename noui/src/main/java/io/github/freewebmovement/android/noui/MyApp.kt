package io.github.freewebmovement.android.noui

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.russhwolf.settings.Settings
import io.github.freewebmovement.peer.PeerClient
import io.github.freewebmovement.peer.PeerManager
import io.github.freewebmovement.peer.PeerServer
import io.github.freewebmovement.peer.database.AppDatabase
import io.github.freewebmovement.peer.database.entity.AccountPeer
import io.github.freewebmovement.peer.database.entity.Peer
import io.github.freewebmovement.peer.interfaces.IApp
import io.github.freewebmovement.peer.interfaces.IInstrumentedHandler
import io.github.freewebmovement.peer.interfaces.IPreference
import io.github.freewebmovement.peer.interfaces.IShare
import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.UserJSON
import io.github.freewebmovement.peer.realize.RoomHandler
import io.github.freewebmovement.peer.system.IPList
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.peer.types.IPScopeType
import io.github.freewebmovement.peer.types.IPType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MyApp(private var context: Context) : IApp {

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
    private lateinit var _settings: KVSettings
    override var settings: KVSettings
        get() = _settings
        set(value) {
            _settings = value
        }

    private lateinit var _peerManager: PeerManager
    override var peerManager: PeerManager
        get() = _peerManager
        set(value) {
            _peerManager = value
        }

    override fun setIpInfo(json: PublicKeyJSON) {
        json.ip = IPList.getIP(IPType.IPV4, IPScopeType.LOCAL)
        json.port = settings.network.port
        json.type = IPList.getPublicType()
    }

    override fun getDownloadDir(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.packageName
        )
    }

    override fun getProfile(): UserJSON {
        val nickname = settings.profile.nickname
        val intro = settings.profile.intro
        val imageUri = settings.profile.imageUri
        var avatar = ""
        if (imageUri != "") {
            val uri: Uri = Uri.parse(imageUri)
            avatar = Image.toBase64(context, uri)
        }
        return UserJSON(nickname = nickname, intro = intro, avatar = avatar)
    }

    override suspend fun getPeers(): List<AccountPeer> {
        return db.account().getPeers()
    }

    override fun startPeerManager() {
        peerManager.start()
    }

    override fun stopPeerManager() {
        peerManager.stop()
    }
    companion object {
        fun new(context: Context): MyApp {
            val app = MyApp(context)
            app.scope = CoroutineScope(Dispatchers.IO)
            app.scope.launch {
                val settings = Settings()
                app.preference = Preference(settings)
                app.crypto = Crypto.getInstance(app.preference)
                app.settings = KVSettings(app.preference)
                app.share = Share(context)
                app.db = getDatabase(context)
                app.handler = RoomHandler(app)
                app.client = PeerClient( app.handler)
                val peer = Peer("0.0.0.0", app.settings.network.port, IPType.IPV4)
                app.server = PeerServer(app, peer)
                app.peerManager = PeerManager(app)
                app.startPeerManager()
            }
            return app
        }
    }
}