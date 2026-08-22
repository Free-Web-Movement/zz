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
        io { apiSaveProfile(profileJson) }

    suspend fun transfer(to: String, amount: Long): String =
        io { apiTransfer(to, amount) }

    /** Witness ring snapshot (active / locked / entries). */
    suspend fun getWitness(): String = io { apiGetWitness() }

    /** Registry nodes with IP classification. */
    suspend fun getNodes(): String = io { apiGetNodes() }

    /** Known seed nodes. */
    suspend fun getSeeds(): String = io { apiGetSeeds() }

    private suspend inline fun io(crossinline block: () -> String): String =
        withContext(Dispatchers.IO) { block().ifEmpty { """{"success":false,"error":"empty"}""" } }

    // ---- JNI ----
    private external fun apiGetData(): String
    private external fun apiGetAddressInfo(address: String): String
    private external fun apiGetBalance(address: String): String
    private external fun apiGetContacts(): String
    private external fun apiAddContact(name: String, address: String): String
    private external fun apiDeleteContact(address: String): String
    private external fun apiGetConversations(): String
    private external fun apiGetChatMessages(contact: String): String
    private external fun apiSendChat(to: String, message: String): String
    private external fun apiGetProfile(address: String): String
    private external fun apiSaveProfile(jsonBody: String): String
    private external fun apiTransfer(to: String, amount: Long): String
    private external fun apiGetWitness(): String
    private external fun apiGetNodes(): String
    private external fun apiGetSeeds(): String

    init {
        System.loadLibrary("zz_rs")
    }
}
