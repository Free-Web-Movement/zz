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

        // ---- 5. 编辑帐号：重命名 + 修改密码 ----
        val renamed = JSONObject(FwmcApi.renameAccount(id, "$name-renamed"))
        assertTrue("renameAccount 应成功: $renamed", renamed.optBoolean("success"))
        val list2 = JSONObject(FwmcApi.listAccounts()).optJSONArray("accounts")!!
        val nm = (0 until list2.length())
            .map { list2.getJSONObject(it) }
            .firstOrNull { it.optString("id") == id }
        assertTrue("改名后应能查到该帐号", nm != null)
        assertEquals("名称应为新名", "$name-renamed", nm!!.optString("name"))

        val pwOk = JSONObject(FwmcApi.changePassword(id, "pw123456", "pw654321"))
        assertTrue("changePassword 应成功: $pwOk", pwOk.optBoolean("success"))

        // 旧密码登录失败，新密码登录成功
        val badLogin = JSONObject(FwmcApi.login(id, "pw123456"))
        assertTrue("旧密码不应能登录", !badLogin.optBoolean("success"))
        val goodLogin = JSONObject(FwmcApi.login(id, "pw654321"))
        assertTrue("新密码应能登录: $goodLogin", goodLogin.optBoolean("success"))

        // ---- 6a. 一个帐号一个目录：资料按帐号隔离，删除帐号即删目录 ----
        val idA = JSONObject(FwmcApi.createAccount("dirA_$name", "")).optString("id")
        JSONObject(FwmcApi.saveProfileFor(idA, JSONObject().apply {
            put("name", "A资料")
        }.toString()))
        // 再建 B（自动成为当前帐号）
        val idB = JSONObject(FwmcApi.createAccount("dirB_$name", "")).optString("id")
        JSONObject(FwmcApi.saveProfileFor(idB, JSONObject().apply {
            put("name", "B资料")
        }.toString()))
        // 当前帐号(B)与指定帐号(A) 各自独立
        assertEquals("B 资料独立保存", "B资料",
            JSONObject(FwmcApi.getProfile()).optJSONObject("profile")!!.optString("name"))
        assertEquals("A 资料独立保存", "A资料",
            JSONObject(FwmcApi.getProfile(idA)).optJSONObject("profile")!!.optString("name"))
        assertEquals("B 资料独立保存(指定id)", "B资料",
            JSONObject(FwmcApi.getProfile(idB)).optJSONObject("profile")!!.optString("name"))
        // 帐号头像：按帐号上传并可读
        val avOk = JSONObject(FwmcApi.setAvatarFor(idB, ByteArray(64) { it.toByte() })).optBoolean("success")
        assertTrue("setAvatarFor 应成功", avOk)
        val avPath = JSONObject(FwmcApi.getProfile(idB)).optJSONObject("profile")!!.optString("avatar_path")
        assertTrue("帐号头像应可读(data uri)", avPath.startsWith("data:image/"))
        val avA = JSONObject(FwmcApi.getProfile(idA)).optJSONObject("profile")!!.optString("avatar_path")
        assertTrue("A 帐号不应有 B 的头像", !avA.startsWith("data:image/") || avA.isEmpty())

        // 删除 A → A 的目录被清，B 不受影响
        JSONObject(FwmcApi.deleteAccount(idA))
        assertEquals("删除后 A 资料目录应被清除", "",
            JSONObject(FwmcApi.getProfile(idA)).optJSONObject("profile")!!.optString("name"))
        assertEquals("B 资料不受影响", "B资料",
            JSONObject(FwmcApi.getProfile(idB)).optJSONObject("profile")!!.optString("name"))
        assertEquals("当前帐号 B 资料不受影响", "B资料",
            JSONObject(FwmcApi.getProfile()).optJSONObject("profile")!!.optString("name"))
        // 清理 B，避免测试残留帐号污染设备数据
        JSONObject(FwmcApi.deleteAccount(idB))

        // ---- 6. 无密码帐号 ----
        val id2 = JSONObject(FwmcApi.createAccount("pwless_$name", "")).optString("id")
        assertTrue("无密码帐号 ID 应存在", id2.isNotEmpty())
        // 无密码：空密码与任意密码均可登录
        assertTrue("无密码帐号空密码登录", JSONObject(FwmcApi.login(id2, "")).optBoolean("success"))
        assertTrue("无密码帐号任意密码登录", JSONObject(FwmcApi.login(id2, "whatever")).optBoolean("success"))
        // 有密码帐号仍需正确密码
        val badLogin2 = JSONObject(FwmcApi.login(id, "wrong-pw"))
        assertTrue("有密码帐号错误密码应失败", !badLogin2.optBoolean("success"))
        assertTrue("有密码帐号正确密码应成功", JSONObject(FwmcApi.login(id, "pw654321")).optBoolean("success"))
        val del2 = JSONObject(FwmcApi.deleteAccount(id2))
        assertTrue(del2.optBoolean("success"))

        // 清理：删除测试帐号（不影响钱包）
        val del = JSONObject(FwmcApi.deleteAccount(id))
        assertTrue(del.optBoolean("success"))
        assertTrue(ctx != null)
    }
}
