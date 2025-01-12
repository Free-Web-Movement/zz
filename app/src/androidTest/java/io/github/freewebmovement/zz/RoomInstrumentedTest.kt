package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.bussiness.Settings
import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.database.ZzDatabase
import io.github.freewebmovement.zz.system.database.entity.AddressType
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.PeerServer
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomInstrumentedTest {
	@Test
	fun should_test_peer() {
		val coroutineScope = CoroutineScope(Dispatchers.IO)
		coroutineScope.launch {
			val app = ApplicationProvider.getApplicationContext<MainApplication>()
			val preference = app.preference
			val setting = Settings(preference)
			val db = ZzDatabase.getDatabase(app.applicationContext)
			app.db = db;
			val dao = app.db.peer()
			dao.clearData()
			dao.clearSequence()
			val epochTime = Time.now()
			val peer = Peer(
				"0.0.0.0",
				setting.localServerPort,
				AddressType.IPV4,
				createdAt = epochTime, updatedAt = epochTime)
			dao.add(peer)
			val all = app.db.peer().getAll()
			assertEquals(all.size, 1)
			val one = all[0]
			assertEquals(one.id, 1)
		}
	}
}