package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.system.database.ZzDatabase
import io.github.freewebmovement.zz.system.database.entity.Peer
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IPInstrumentedTest {
	@Test
	fun should_test_peer() {
		val coroutineScope = CoroutineScope(Dispatchers.IO)
		coroutineScope.launch {
			val app = ApplicationProvider.getApplicationContext<MainApplication>()
			val db = ZzDatabase.getDatabase(app.applicationContext)
			app.db = db;
			val dao = app.db.peer()
			dao.clearData()
			dao.clearSequence()
			val epochTime = System.currentTimeMillis() / 1000
			val peer = Peer(createdAt = epochTime, updatedAt = epochTime)
			dao.add(peer)
			val all = app.db.peer().getAll()
			assertEquals(all.size, 1)
			val one = all[0]
			assertEquals(one.id, 1)
		}
	}
}