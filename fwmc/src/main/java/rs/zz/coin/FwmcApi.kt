package rs.zz.coin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JNI data API against the running fwmc node.
 *
 * Every method returns the raw JSON string produced by the Rust side, e.g.
 * `{"success":true,...}` or `{"success":false,"error":"..."}`. Calls block on
 * the node's async runtime internally, so they run on [Dispatchers.IO].
 *
 * The node must have been started via [FwmcNode.start] first; otherwise every
 * call returns a "node not running" error object.
 */
object FwmcApi {

    /** Dashboard payload (tick/epoch/witness/connections/nodes/seeds/balance). */
    suspend fun getData(): String = io { apiGetData() }

    /** Raw P2P connection list with local/remote endpoints. */
    suspend fun getConnections(): String = io { apiGetConnections() }

    /** Balance + recent transactions for one address. */
    suspend fun getAddressInfo(address: String): String =
        io { apiGetAddressInfo(address) }

    /** Balance of one address (raw scaled integer). */
    suspend fun getBalance(address: String): String =
        io { apiGetBalance(address) }

    /** Contact list merged with registry nodes. */
    suspend fun getContacts(): String = io { apiGetContacts() }

    suspend fun addContact(name: String, address: String): String =
        io { apiAddContact(name, address) }

    suspend fun deleteContact(address: String): String =
        io { apiDeleteContact(address) }

    /** Conversation summaries (last message + unread count). */
    suspend fun getConversations(): String = io { apiGetConversations() }

    /** Chat history with one contact (marks as read). */
    suspend fun getChatMessages(contact: String): String =
        io { apiGetChatMessages(contact) }

    suspend fun sendChat(to: String, message: String): String =
        io { apiSendChat(to, message) }

    suspend fun getProfile(address: String = ""): String =
        io { apiGetProfile(address) }

    suspend fun saveProfile(profileJson: String): String =
        io { apiSaveProfile("", profileJson) }

    /** 保存指定帐号（或空串=当前帐号）的资料。 */
    suspend fun saveProfileFor(accountId: String, profileJson: String): String =
        io { apiSaveProfile(accountId, profileJson) }

    /** 上传指定帐号（或空串=当前帐号）的头像。 */
    suspend fun setAvatarFor(accountId: String, data: ByteArray): String =
        io { apiSetAvatarFor(accountId, data) }

    /** 生成二维码矩阵（返回 width + 0/1 位串）。 */
    suspend fun qrMatrix(data: String): String = io { apiQrMatrix(data) }


    suspend fun transfer(to: String, amount: Long): String =
        io { apiTransfer(to, amount) }

    /** Witness ring snapshot (active / locked / entries). */
    suspend fun getWitness(): String = io { apiGetWitness() }

    /** Registry nodes with IP classification. */
    suspend fun getNodes(): String = io { apiGetNodes() }

    /** Known seed nodes. */
    suspend fun getSeeds(): String = io { apiGetSeeds() }

    /** Resource weights (IP counts, witness eligibility, composite weight). */
    suspend fun getWeights(): String = io { apiGetWeights() }

    /** Genesis allocation data (total supply, initial allocations, etc.). */
    suspend fun getGenesis(): String = io { apiGetGenesis() }

    // ---- 账本浏览（与网页 `/api/ledger/*` 同一份 JSON 契约）----

    /** 账本概览：交易/发币/见证环计数、最新 epoch / 区块、本人余额。 */
    suspend fun getLedgerSummary(address: String = ""): String =
        io { apiGetLedgerSummary(address) }

    /** 交易分页；`address` 非空时只看该地址相关的交易。 */
    suspend fun getLedgerTransactions(address: String = "", page: Int = 1, perPage: Int = 20): String =
        io { apiGetLedgerTransactions(address, page, perPage) }

    /** epoch 列表（按 day 倒序）。 */
    suspend fun getLedgerEpochs(limit: Int = 50): String =
        io { apiGetLedgerEpochs(limit) }

    /** 发币明细；`epoch <= 0` 表示全部。 */
    suspend fun getLedgerRewards(epoch: Long = 0, limit: Int = 50): String =
        io { apiGetLedgerRewards(epoch, limit) }

    /** 分配明细（含权重与链式 hash）；`epoch <= 0` 表示全部。 */
    suspend fun getLedgerAllocations(epoch: Long = 0, limit: Int = 50): String =
        io { apiGetLedgerAllocations(epoch, limit) }

    /** 全网地址币量排名（标记本人 `is_me`）。 */
    suspend fun getLedgerBalances(limit: Int = 50): String =
        io { apiGetLedgerBalances(limit) }

    /** 区块链（一个 epoch 一个区块）。 */
    suspend fun getLedgerBlocks(limit: Int = 50): String =
        io { apiGetLedgerBlocks(limit) }

    /** tick 记录；`epoch <= 0` 表示全部。 */
    suspend fun getLedgerTicks(epoch: Long = 0, limit: Int = 100): String =
        io { apiGetLedgerTicks(epoch, limit) }

    /** 见证环：活动/锁定环 + 历史静态环 + 逐 tick 见证记录。 */
    suspend fun getLedgerRings(epoch: Long = 0, limit: Int = 50): String =
        io { apiGetLedgerRings(epoch, limit) }

    /** Get all fwmc config values. */
    suspend fun getConfig(): String = io { apiGetConfig() }

    /** Set fwmc config values (JSON body). */
    suspend fun setConfig(jsonBody: String): String = io { apiSetConfig(jsonBody) }

    /**
     * 纪元运行模式：`tickMs > 0` 启用测试模式（每 tick 的真实毫秒数），
     * `tickMs <= 0` 为生产模式（真实 15 分钟/tick）。**必须在启动/重启节点前调用**。
     */
    fun setTimeSim(tickMs: Long) {
        apiSetTimeSim(tickMs)
    }

    /** 查询当前时间仿真状态：`{ success, enabled, tick_ms }`。 */
    suspend fun getTimeSim(): String = io { apiGetTimeSim() }

    /**
     * 测试模式下 TICK 的初始值（0..96-1），达到 96 时发 EPOCH_NEW。
     * **必须在启动/重启节点前调用**；生产模式忽略。
     */
    fun setStartTick(tick: Long) {
        apiSetStartTick(tick)
    }

    suspend fun setLang(code: String): String = io { apiSetLang(code) }

    /** Add a seed server (ip, port); persists and connects immediately. */
    suspend fun addSeed(ip: String, port: Int): String =
        io { apiAddSeed(ip, port) }

    /** Delete a seed server by (ip, port). */
    suspend fun deleteSeed(ip: String, port: Int): String =
        io { apiDeleteSeed(ip, port) }

    /** Upload the local avatar image (JPEG/PNG bytes). */
    suspend fun setAvatar(image: ByteArray): String =
        io { apiSetAvatar(image) }

    /** Set local data dir (accounts/wallets root); call once at app start. */
    fun initDataDir(dir: String) {
        apiInitDataDir(dir)
    }

    /** List local accounts (id/name). */
    suspend fun listAccounts(): String = io { apiListAccounts() }

    /** Currently logged-in account, if any. */
    suspend fun currentAccount(): String = io { apiCurrentAccount() }

    /** Create a local account (temp numeric id + password); auto-login. */
    suspend fun createAccount(name: String, password: String): String =
        io { apiCreateAccount(name, password) }

    /** Login with account id + password. */
    suspend fun login(id: String, password: String): String =
        io { apiLogin(id, password) }

    suspend fun logout(): String = io { apiLogout() }

    /** Delete an account; wallets are not affected. */
    suspend fun deleteAccount(id: String): String = io { apiDeleteAccount(id) }
    suspend fun renameAccount(id: String, name: String): String = io { apiRenameAccount(id, name) }
    suspend fun changePassword(id: String, oldPw: String, newPw: String): String = io { apiChangePassword(id, oldPw, newPw) }

    /** List wallets: bound primary first, then local wallets. */
    suspend fun listWallets(): String = io { apiListWallets() }

    /** Create a wallet (random keypair); language = bip39 wordlist (english/chinese_simplified/...). Returns mnemonic for backup. */
    suspend fun createWallet(name: String, language: String = "english"): String =
        io { apiCreateWallet(name, language) }

    /** Check whether a TCP port is free on 0.0.0.0. */
    suspend fun checkPort(port: Int): String = io { apiCheckPort(port) }

    /** Delete a non-bound wallet by name. */
    suspend fun deleteWallet(name: String): String = io { apiDeleteWallet(name) }

    /** Bind wallet as node primary (effective after node restart). */
    suspend fun bindWallet(name: String): String = io { apiBindWallet(name) }

    private suspend inline fun io(crossinline block: () -> String): String =
        withContext(Dispatchers.IO) { block().ifEmpty { """{"success":false,"error":"empty"}""" } }

    // ---- JNI ----
    private external fun apiGetData(): String
    private external fun apiGetConnections(): String
    private external fun apiGetAddressInfo(address: String): String
    private external fun apiGetBalance(address: String): String
    private external fun apiGetContacts(): String
    private external fun apiAddContact(name: String, address: String): String
    private external fun apiDeleteContact(address: String): String
    private external fun apiGetConversations(): String
    private external fun apiGetChatMessages(contact: String): String
    private external fun apiSendChat(to: String, message: String): String
    private external fun apiGetProfile(address: String): String
    private external fun apiSaveProfile(address: String, jsonBody: String): String
    private external fun apiTransfer(to: String, amount: Long): String
    private external fun apiGetWitness(): String
    private external fun apiGetNodes(): String
    private external fun apiGetSeeds(): String
    private external fun apiGetWeights(): String
    private external fun apiGetGenesis(): String
    private external fun apiGetLedgerSummary(address: String): String
    private external fun apiGetLedgerTransactions(address: String, page: Int, perPage: Int): String
    private external fun apiGetLedgerEpochs(limit: Int): String
    private external fun apiGetLedgerRewards(epoch: Long, limit: Int): String
    private external fun apiGetLedgerAllocations(epoch: Long, limit: Int): String
    private external fun apiGetLedgerBalances(limit: Int): String
    private external fun apiGetLedgerBlocks(limit: Int): String
    private external fun apiGetLedgerTicks(epoch: Long, limit: Int): String
    private external fun apiGetLedgerRings(epoch: Long, limit: Int): String
    private external fun apiGetConfig(): String
    private external fun apiSetConfig(jsonBody: String): String
    private external fun apiSetTimeSim(tickMs: Long): String
    private external fun apiGetTimeSim(): String
    private external fun apiSetStartTick(tick: Long): String
    private external fun apiSetLang(code: String): String
    private external fun apiAddSeed(ip: String, port: Int): String
    private external fun apiDeleteSeed(ip: String, port: Int): String
    private external fun apiSetAvatar(image: ByteArray): String
    private external fun apiSetAvatarFor(address: String, image: ByteArray): String
    private external fun apiQrMatrix(data: String): String
    private external fun apiInitDataDir(dir: String): String
    private external fun apiListAccounts(): String
    private external fun apiCurrentAccount(): String
    private external fun apiCreateAccount(name: String, password: String): String
    private external fun apiLogin(id: String, password: String): String
    private external fun apiLogout(): String
    private external fun apiDeleteAccount(id: String): String
    private external fun apiRenameAccount(id: String, name: String): String
    private external fun apiChangePassword(id: String, oldPw: String, newPw: String): String
    private external fun apiListWallets(): String
    private external fun apiCreateWallet(name: String, language: String): String
    private external fun apiCheckPort(port: Int): String
    private external fun apiDeleteWallet(name: String): String
    private external fun apiBindWallet(name: String): String

    init {
        System.loadLibrary("fwmc")
    }
}
