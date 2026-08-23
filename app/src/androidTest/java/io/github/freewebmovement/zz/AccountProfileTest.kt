package io.github.freewebmovement.zz

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import rs.zz.coin.FwmcApi

/**
 * 帐号-资料对应关系测试用例（需真机/模拟器运行）：
 * 1. 创建帐号 → ID 为 24 位纯数字（创建时间+随机数），且与钱包地址算法无关
 * 2. 编辑资料（昵称+签名）→ 保存到 FWMC → 再读取应与当前帐号对应一致
 * 3. listAccounts 能查到该帐号；currentAccount 指向它
 */
@RunWith(AndroidJUnit4::class)
class AccountProfileTest {

    private var nodePtr: Long = 0

    @Before
    fun setUp() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = java.io.File(ctx.cacheDir, "fwmc_test").apply { deleteRecursively(); mkdirs() }.absolutePath
        FwmcApi.initDataDir(dir)
        // 资料保存依赖节点运行：启动测试节点
        val port = 27600 + (System.currentTimeMillis() % 100).toInt()
        nodePtr = rs.zz.coin.FwmcNode().start(port, dir)
        // 等待节点就绪
        var ready = false
        repeat(30) {
            val ok = kotlinx.coroutines.runBlocking {
                runCatching { JSONObject(FwmcApi.getData()).optBoolean("success") }.getOrDefault(false)
            }
            if (ok) { ready = true; return@repeat }
            Thread.sleep(500)
        }
        check(ready) { "测试节点未能在 15s 内就绪" }
    }

    @After
    fun tearDown() {
        if (nodePtr != 0L) rs.zz.coin.FwmcNode().stop(nodePtr)
    }

    @Test
    fun accountProfileFlow() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        // ---- 1. 创建帐号 ----
        val name = "test_" + System.currentTimeMillis()
        val created = JSONObject(FwmcApi.createAccount(name, "pw123456"))
        assertTrue("createAccount 应成功: $created", created.optBoolean("success"))
        val id = created.optString("id")
        assertTrue("ID 应为 24 位纯数字, got=$id", id.length == 24 && id.all { it.isDigit() })

        // ---- 2. currentAccount 指向新帐号 ----
        val cur = JSONObject(FwmcApi.currentAccount())
        assertTrue(cur.optBoolean("success"))
        assertEquals("当前帐号应为刚创建的帐号", id, cur.optString("id"))

        // ---- 3. 昵称/签名保存并回读一致 ----
        val nickname = "昵称_$name"
        val bio = "签名_${System.currentTimeMillis()}"
        val saved = JSONObject(
            FwmcApi.saveProfile(JSONObject().apply {
                put("name", nickname)
                put("notes", bio)
            }.toString())
        )
        assertTrue("saveProfile 应成功: $saved", saved.optBoolean("success"))

        val raw = FwmcApi.getProfile()
        val loaded = JSONObject(raw).optJSONObject("profile")!!
        assertTrue("昵称应对应当前帐号 raw=$raw", nickname == loaded.optString("name"))
        assertEquals("签名应对应当前帐号", bio, loaded.optString("notes"))

        // ---- 4. listAccounts 包含该帐号 ----
        val list = JSONObject(FwmcApi.listAccounts()).optJSONArray("accounts")!!
        val ids = (0 until list.length()).map { list.getJSONObject(it).optString("id") }
        assertTrue("listAccounts 应包含新帐号", ids.contains(id))

        // 清理：删除测试帐号（不影响钱包）
        val del = JSONObject(FwmcApi.deleteAccount(id))
        assertTrue(del.optBoolean("success"))
        assertTrue(ctx != null)
    }
}
