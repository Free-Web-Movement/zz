package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.bussiness.Settings
import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.crypto.Crypto
import io.github.freewebmovement.zz.system.database.ZzDatabase
import io.github.freewebmovement.zz.system.database.entity.Account
import io.github.freewebmovement.zz.system.database.entity.IPType
import io.github.freewebmovement.zz.system.database.entity.Peer
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomInstrumentedTest {
    @Test
    fun should_test_peer() = runTest {
        val app = ApplicationProvider.getApplicationContext<MainApplication>()
        val preference = app.preference
        val setting = Settings(preference)
        val db = ZzDatabase.getDatabase(app.applicationContext)
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
            account.id,
            "0.0.0.0",
            setting.localServerPort,
            IPType.IPV4,
            createdAt = epochTime, updatedAt = epochTime
        )
        dao.add(peer)
        val all = app.db.peer().getAll()
        assertEquals(all.size, 1)
        val one = all[0]
        assertEquals(one.id, 1)
    }
}