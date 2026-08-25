package io.github.freewebmovement.zz

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import rs.zz.coin.FwmcApi

/**
 * 资料全字段增删改查（API 级 instrumented 测试，复用应用内嵌节点）。
 * 覆盖：全字段写入 → 回读校验；部分更新（其余字段保留）；帐号删除后资料不可达。
 */
@RunWith(AndroidJUnit4::class)
class ProfileCrudTest {

    private val cleaned = mutableListOf<String>()
    private var origAddr = ""

    @Before
    fun setUp() {
        origAddr = runBlocking {
            runCatching { JSONObject(FwmcApi.currentAccount()).optString("id") }.getOrDefault("")
        }
        // 无已绑定钱包时建一个保底钱包，避免测试钱包成为"唯一绑定钱包"而无法删除
        if (origAddr.isEmpty()) {
            runBlocking {
                val k = runCatching {
                    JSONObject(FwmcApi.createWallet("keeper_" + System.currentTimeMillis(), "english"))
                }.getOrDefault(JSONObject())
                val ka = k.optString("address")
                if (k.optBoolean("success") && ka.isNotEmpty()) {
                    runCatching { JSONObject(FwmcApi.login(ka, "")) }
                    origAddr = ka
                }
            }
        }
        var ready = false
        repeat(40) {
            val ok = runBlocking {
                runCatching { JSONObject(FwmcApi.getData()).optBoolean("success") }.getOrDefault(false)
            }
            if (ok) { ready = true; return@repeat }
            Thread.sleep(500)
        }
        check(ready) { "内嵌节点未能在 20s 内就绪" }
    }

    @After
    fun tearDown() {
        cleaned.forEach {
            runBlocking { runCatching { JSONObject(FwmcApi.deleteAccount(it)) } }
        }
        cleaned.clear()
    }

    private suspend fun createAccount(prefix: String): String {
        val created = JSONObject(FwmcApi.createWallet(prefix + "_" + System.currentTimeMillis(), "english"))
        assertTrue("createWallet 应成功: $created", created.optBoolean("success"))
        val addr = created.optString("address")
        assertTrue("地址应非空", addr.isNotEmpty())
        runBlocking {
            val login = JSONObject(FwmcApi.login(addr, ""))
            assertTrue("login 应成功: $login", login.optBoolean("success"))
        }
        cleaned.add(addr)
        return addr
    }

    private suspend fun save(addr: String, body: JSONObject) {
        val r = JSONObject(FwmcApi.saveProfileFor(addr, body.toString()))
        assertTrue("saveProfileFor 应成功: $r", r.optBoolean("success"))
    }

    private suspend fun load(addr: String): JSONObject =
        JSONObject(FwmcApi.getProfile(addr)).optJSONObject("profile")
            ?: throw AssertionError("getProfile 应返回 profile")

    private fun assertAllFields(p: JSONObject, d: JSONObject) {
        assertEquals(d.getString("nickname"), p.optString("nickname"))
        assertEquals(d.getString("nickname"), p.optString("name"))
        assertEquals(d.getString("notes"), p.optString("notes"))
        assertEquals(d.getString("gender"), p.optString("gender"))
        assertEquals(d.getString("blood_type"), p.optString("blood_type"))
        assertEquals(d.getInt("age"), p.optInt("age", -1))
        assertEquals(d.getInt("height_cm"), p.optInt("height_cm", -1))
        assertEquals(d.getInt("weight_kg"), p.optInt("weight_kg", -1))
        assertEquals(d.getString("ethnicity"), p.optString("ethnicity"))
        assertEquals(d.getString("education"), p.optString("education"))
        assertEquals(d.getString("country"), p.optString("country"))
        assertEquals(d.getString("province"), p.optString("province"))
        assertEquals(d.getString("city"), p.optString("city"))
        assertEquals(d.getString("county"), p.optString("county"))
        assertEquals(d.getString("town"), p.optString("town"))
        assertEquals(d.getString("village"), p.optString("village"))
        assertEquals(d.getString("home_address"), p.optString("home_address"))
        assertEquals(d.getString("occupation"), p.optString("occupation"))
        assertEquals(d.getString("zodiac"), p.optString("zodiac"))
    }

    private fun crudAll(d: JSONObject, tag: String) {
        runBlocking {
            // ---- 增：建号 + 全字段写入 ----
            val addr = createAccount(tag)
            val body = JSONObject().apply {
                put("nickname", d.getString("nickname"))
                put("name", d.getString("nickname"))
                put("notes", d.getString("notes"))
                put("gender", d.getString("gender"))
                put("blood_type", d.getString("blood_type"))
                put("age", d.getInt("age"))
                put("height_cm", d.getInt("height_cm"))
                put("weight_kg", d.getInt("weight_kg"))
                put("ethnicity", d.getString("ethnicity"))
                put("education", d.getString("education"))
                put("country", d.getString("country"))
                put("province", d.getString("province"))
                put("city", d.getString("city"))
                put("county", d.getString("county"))
                put("town", d.getString("town"))
                put("village", d.getString("village"))
                put("home_address", d.getString("home_address"))
                put("occupation", d.getString("occupation"))
                put("zodiac", d.getString("zodiac"))
            }
            save(addr, body)

            // ---- 查：全字段回读 ----
            assertAllFields(load(addr), d)

            // ---- 改：部分更新（notes/city 变更，其余必须保留）----
            val upd = JSONObject(load(addr).toString())
            upd.put("notes", d.getString("notes") + "_upd")
            upd.put("city", d.getString("city") + "X")
            save(addr, upd)

            val p2 = load(addr)
            assertEquals(d.getString("notes") + "_upd", p2.optString("notes"))
            assertEquals(d.getString("city") + "X", p2.optString("city"))
            assertEquals(d.getString("nickname"), p2.optString("nickname"))
            assertEquals(d.getString("gender"), p2.optString("gender"))
            assertEquals(d.getString("blood_type"), p2.optString("blood_type"))
            assertEquals(d.getInt("age"), p2.optInt("age", -1))
            assertEquals(d.getInt("height_cm"), p2.optInt("height_cm", -1))
            assertEquals(d.getInt("weight_kg"), p2.optInt("weight_kg", -1))
            assertEquals(d.getString("ethnicity"), p2.optString("ethnicity"))
            assertEquals(d.getString("education"), p2.optString("education"))
            assertEquals(d.getString("country"), p2.optString("country"))
            assertEquals(d.getString("province"), p2.optString("province"))
            assertEquals(d.getString("county"), p2.optString("county"))
            assertEquals(d.getString("town"), p2.optString("town"))
            assertEquals(d.getString("village"), p2.optString("village"))
            assertEquals(d.getString("home_address"), p2.optString("home_address"))
            assertEquals(d.getString("occupation"), p2.optString("occupation"))
            assertEquals(d.getString("zodiac"), p2.optString("zodiac"))

            // ---- 删：切回原帐号解绑后删除 ----
            var dbg = "orig=$origAddr "
            if (origAddr.isNotEmpty() && origAddr != addr) {
                val lg = runCatching { JSONObject(FwmcApi.login(origAddr, "")) }.getOrDefault(JSONObject())
                dbg += "login=${lg} "
            }
            dbg += "cur=${runCatching { FwmcApi.currentAccount() }.getOrDefault("?")} "
            val del = JSONObject(FwmcApi.deleteAccount(addr))
            assertTrue("deleteAccount 应成功: $del $dbg", del.optBoolean("success"))
            cleaned.remove(addr)

            // getProfile 对已删地址可能回退到当前帐号，只要不再返回已删内容即可
            val goneP = runCatching { load(addr) }.getOrNull()
            assertTrue("删除后不应再读到已删资料",
                goneP == null || goneP.optString("nickname") != d.getString("nickname"))

            val list = FwmcApi.listAccounts()
            assertFalse("删除后列表不应包含该帐号", list.contains(addr))
        }
    }

    @Test
    fun crudAllFieldsDatasetA() {
        crudAll(
            JSONObject()
                .put("nickname", "小狐狸🦊")
                .put("notes", "签名：你好，世界 Hello 🌍")
                .put("gender", "女")
                .put("blood_type", "O")
                .put("age", 28)
                .put("height_cm", 165)
                .put("weight_kg", 50)
                .put("ethnicity", "汉族")
                .put("education", "本科")
                .put("country", "中国")
                .put("province", "浙江省")
                .put("city", "杭州市")
                .put("county", "西湖区")
                .put("town", "文新街道")
                .put("village", "文华社区")
                .put("home_address", "文一西路 1 号")
                .put("occupation", "工程师")
                .put("zodiac", "天蝎座"),
            "crudA",
        )
    }

    @Test
    fun crudAllFieldsDatasetB() {
        crudAll(
            JSONObject()
                .put("nickname", "BobTester")
                .put("notes", "long notes xxxxx")
                .put("gender", "男")
                .put("blood_type", "AB")
                .put("age", 45)
                .put("height_cm", 188)
                .put("weight_kg", 88)
                .put("ethnicity", "瑶族")
                .put("education", "硕士")
                .put("country", "美国")
                .put("province", "加利福尼亚州")
                .put("city", "旧金山")
                .put("county", "圣克拉拉")
                .put("town", "圣何塞")
                .put("village", "硅谷")
                .put("home_address", "1 Market St")
                .put("occupation", "Designer")
                .put("zodiac", "摩羯座"),
            "crudB",
        )
    }
}
