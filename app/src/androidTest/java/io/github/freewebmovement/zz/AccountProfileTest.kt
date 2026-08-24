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
 * 钱包即帐号：每个钱包 = 一个帐号（身份绑定钱包地址）。
 * 1. 创建钱包 → 即创建帐号（Peer ID = 钱包地址）
 * 2. 选择钱包（绑定）= 选择帐号
 * 3. 资料按钱包地址独立存储；删除钱包=删除帐号（目录一并删除）
 */
@RunWith(AndroidJUnit4::class)
class AccountProfileTest {

    private val cleaned = mutableListOf<String>()

    @Before
    fun setUp() {
        // 复用应用内嵌节点（MainApplication 自动启动），避免同进程起第二个节点污染共享 native 状态。
        // initDataDir 为 no-op：数据目录固定为应用目录，与 UI 行为一致。
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
    }

    @Test
    fun walletIsAccountFlow() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
        // ---- 1. 创建钱包 = 创建帐号 ----
        val name = "acct_" + System.currentTimeMillis()
        val created = JSONObject(FwmcApi.createWallet(name, "english"))
        assertTrue("createWallet 应成功: $created", created.optBoolean("success"))
        val addr = created.optString("address")
        assertTrue("钱包地址(Peer ID)应非空", addr.isNotEmpty())
        val mnemonic = created.optString("mnemonic")
        assertTrue("应返回助记词", mnemonic.split(Regex("\\s+")).size >= 12)

        // 创建钱包必须同步创建其用户 Profile 目录（一个钱包一个帐号）
        val userDir = java.io.File(ctx.filesDir, "fwmc/users/$addr")
        assertTrue("创建钱包应生成用户目录: $userDir", userDir.exists())
        assertTrue("用户目录应有 profile.json", java.io.File(userDir, "profile.json").exists())

        // ---- 2. 列表里它就是帐号 ----
        val listRaw = FwmcApi.listAccounts()
        val list = JSONObject(listRaw).optJSONArray("accounts")
        val found = list?.let { arr ->
            (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .any { it.optString("id") == addr }
        } ?: false
        assertTrue("listAccounts 应包含该钱包 as acct; raw=$listRaw addr=$addr", found)

        // ---- 3. 选择钱包 = 选择帐号（绑定后 currentAccount 指向它）----
        val bind = JSONObject(FwmcApi.login(addr, ""))
        assertTrue("选择钱包应成功: $bind", bind.optBoolean("success"))
        val cur = JSONObject(FwmcApi.currentAccount())
        assertTrue(cur.optBoolean("success"))
        assertEquals("当前帐号应为所选钱包", addr, cur.optString("id"))

        // ---- 4. 资料按钱包地址独立存储 ----
        val nickname = "昵称_$name"
        val saved = JSONObject(
            FwmcApi.saveProfileFor(addr, JSONObject().apply {
                put("nickname", nickname)
            }.toString())
        )
        assertTrue("saveProfileFor 应成功: $saved", saved.optBoolean("success"))
        assertEquals("资料应绑定钱包地址", nickname,
            JSONObject(FwmcApi.getProfile(addr)).optJSONObject("profile")!!.optString("nickname"))

        // ---- 5. 头像按钱包上传并可读 ----
        val avOk = JSONObject(FwmcApi.setAvatarFor(addr, ByteArray(64) { it.toByte() })).optBoolean("success")
        assertTrue("setAvatarFor 应成功", avOk)
        val avPath = JSONObject(FwmcApi.getProfile(addr)).optJSONObject("profile")!!.optString("avatar_path")
        assertTrue("钱包头像应可读", avPath.startsWith("data:image/"))

        // 头像修改后同步更新：第二次上传应覆盖第一次
        val av1 = JSONObject(FwmcApi.getProfile(addr)).optJSONObject("profile")!!.optString("avatar_path")
        val bytesB = ByteArray(96) { (it * 7).toByte() }
        assertTrue("第二次上传头像应成功",
            JSONObject(FwmcApi.setAvatarFor(addr, bytesB)).optBoolean("success"))
        val av2 = JSONObject(FwmcApi.getProfile(addr)).optJSONObject("profile")!!.optString("avatar_path")
        assertTrue("我的页应读到新头像", av2.startsWith("data:image/") && av2 != av1)

        // 编辑昵称（部分保存）不应丢头像 —— ProfileEditor 保存流程依赖此语义
        val keep = JSONObject(FwmcApi.saveProfileFor(addr, JSONObject().apply {
            put("nickname", "改名不改头像")
        }.toString()))
        assertTrue("部分保存应成功: $keep", keep.optBoolean("success"))
        val prof = JSONObject(FwmcApi.getProfile(addr)).optJSONObject("profile")!!
        assertEquals("部分保存后昵称更新", "改名不改头像", prof.optString("nickname"))
        assertEquals("部分保存后头像保留", av2, prof.optString("avatar_path"))

        // ---- 6. 私聊保护：钱包身份密码 ----
        val pwOk = JSONObject(FwmcApi.changePassword(addr, "", "secret"))
        assertTrue("设置身份密码应成功: $pwOk", pwOk.optBoolean("success"))
        val badLogin = JSONObject(FwmcApi.login(addr, "wrong"))
        assertTrue("错误密码不应能选择该钱包", !badLogin.optBoolean("success"))
        val goodLogin = JSONObject(FwmcApi.login(addr, "secret"))
        assertTrue("正确密码应能选择该钱包: $goodLogin", goodLogin.optBoolean("success"))

        // 删除钱包 = 删除帐号：用户目录同步删除
        val del = JSONObject(FwmcApi.deleteAccount(addr))
        assertTrue("deleteAccount 应成功: $del", del.optBoolean("success"))
        assertTrue("删除钱包后用户目录应被删除", !java.io.File(ctx.filesDir, "fwmc/users/$addr").exists())

        // 清理
        cleaned.add(addr)
        }
    }
}
