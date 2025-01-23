package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.peer.IPType
import io.github.freewebmovement.peer.system.Settings
import io.github.freewebmovement.peer.system.Time
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.peer.database.entity.Account
import io.github.freewebmovement.peer.database.entity.Peer
import io.github.freewebmovement.zz.system.getDatabase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomInstrumentedTest {
    @Test
    fun should_test_peer() = runTest {
        val context = ApplicationProvider.getApplicationContext<MainApplication>()
        var app = MyApp(context)
        app.preference = MainApplication.getPreference(context)
        val setting = Settings(app.preference)
        val db = getDatabase(context)
        app.db = db
        val crypto = Crypto.createCrypto()
        val address = Crypto.toAddress(crypto.publicKey)
        val account = Account(address)
        app.db.account().add(account)
        val account01 = app.db.account().getAccountByAddress(account.address)
        assert(account01!=null)
        assert(account01!!.id != 0)
        val dao = app.db.peer()
        dao.clearData()
        dao.clearSequence()
        val epochTime = Time.now()
        val peer = Peer(
            "0.0.0.0",
            setting.localServerPort,
            IPType.IPV4
        )
        peer.account = account.id;
        dao.add(peer)
        val all = app.db.peer().getAll()
        assertEquals(all.size, 1)
        val one = all[0]
        assertEquals(one.id, 1)
    }
}