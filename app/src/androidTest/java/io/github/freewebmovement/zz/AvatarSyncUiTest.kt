package io.github.freewebmovement.zz

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 头像端到端同步测试：在我的页点「＋」→ 系统相册任选一张 →
 * 上传成功后，我的页头像立即更新（getProfile 返回新的 data:image）。
 */
@RunWith(AndroidJUnit4::class)
class AvatarSyncUiTest {

    private fun profileAvatar(addr: String): String =
        runCatching {
            kotlinx.coroutines.runBlocking {
                JSONObject(rs.zz.coin.FwmcApi.getProfile(addr))
                    .optJSONObject("profile")?.optString("avatar_path") ?: ""
            }
        }.getOrDefault("")

    @Test
    fun pickAnyPhotoUpdatesMineAvatar() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // 回到桌面再拉起应用，保证处于我的页入口的干净状态
        device.pressHome()
        Thread.sleep(600)
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)!!.apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.text("我的")), 15_000)

        // 进入我的页（点击底部导航最下方的「我的」）
        val tabs = device.findObjects(By.text("我的"))
        assertTrue("找不到底部导航「我的」", tabs.isNotEmpty())
        tabs.last().click()
        assertTrue(
            "我的页未加载",
            device.wait(Until.hasObject(By.textContains("钱包地址")), 10_000),
        )

        // 当前帐号与头像基线（与 UI 同源：直接读应用会话，避免其它测试改绑后错位）
        val sess = io.github.freewebmovement.zz.ui.content.FwmcSession.current
        assertTrue("会话未就绪", sess != null)
        val addr = sess!!.first
        assertTrue(addr.isNotEmpty())
        // 基线先置为一张合成图：这样无论在相册里任选哪张真实照片，上传后必然不同
        assertTrue(
            "设置基线头像失败",
            JSONObject(kotlinx.coroutines.runBlocking {
                rs.zz.coin.FwmcApi.setAvatarFor(addr, ByteArray(64) { (it * 3 + 7).toByte() })
            }).optBoolean("success"),
        )
        val before = profileAvatar(addr)
        assertTrue(before.startsWith("data:image/"))

        // 我的页只显示头像本身，点按头像 → 系统相册
        val badge = device.findObject(By.desc("头像"))
            ?: device.findObject(By.desc("Avatar"))
        assertTrue("找不到头像", badge != null)
        badge.click()

        // 相册里任意选一张（取第一张照片）
        assertTrue(
            "相册未打开",
            device.wait(Until.hasObject(By.descStartsWith("照片拍摄于")), 15_000)
                || device.wait(Until.hasObject(By.descStartsWith("Photo taken")), 5_000),
        )
        val photo = device.findObject(By.descStartsWith("照片拍摄于"))
            ?: device.findObject(By.descStartsWith("Photo taken"))
        assertTrue("相册中没有可选照片", photo != null)
        photo.click()

        // 上传完成后 getProfile 应返回新的 data:image（与基线不同）
        var updated = false
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val now = profileAvatar(addr)
            if (now.startsWith("data:image/") && now != before) { updated = true; break }
            Thread.sleep(800)
        }
        val curNow = runCatching {
            kotlinx.coroutines.runBlocking { JSONObject(rs.zz.coin.FwmcApi.currentAccount()) }
        }.getOrNull()
        assertTrue(
            "选择照片后我的页头像未同步更新: sess=$addr before=${before.take(40)} " +
                "now=${profileAvatar(addr).take(40)} curAcc=${curNow?.optString("id")}",
            updated,
        )

        // 回到前台再走一次 ON_RESUME 刷新路径，头像仍应保持新值
        device.pressHome()
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.textContains("钱包地址")), 10_000)
        val again = profileAvatar(addr)
        assertTrue("返回后头像应保持", again.startsWith("data:image/") && again != before)
    }
}
