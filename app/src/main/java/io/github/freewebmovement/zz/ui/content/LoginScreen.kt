package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi

/** 全局登录态：null = 未登录。 */
object FwmcSession {
    var current by mutableStateOf<Pair<String, String>?>(null)
    suspend fun refresh() {
        val o = runCatching { JSONObject(FwmcApi.currentAccount()) }.getOrNull()
        current = if (o?.optBoolean("success") == true && o.has("id")) {
            o.optString("id") to o.optString("name")
        } else null
    }
}

/** 登录/注册页：无帐号时直接进入创建流程（临时数字ID，密码可选）。 */
@Composable
fun LoginScreen() {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("create") }
    var idOrName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var accounts by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(Unit) {
        runCatching {
            val o = JSONObject(FwmcApi.listAccounts())
            accounts = o.optJSONArray("accounts")?.let { a ->
                (0 until a.length()).map { i ->
                    val e = a.getJSONObject(i); e.optString("id") to e.optString("name")
                }
            } ?: emptyList()
            if (accounts.isEmpty()) mode = "create" else mode = "login"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("FWMC", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(s.login.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                if (mode == "create") {
                    OutlinedTextField(idOrName, { idOrName = it }, label = { Text(s.login.accountName) }, modifier = Modifier.fillMaxWidth())
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = idOrName,
                            onValueChange = { idOrName = it },
                            label = { Text(s.login.selectAccount) },
                            readOnly = true,
                            trailingIcon = { TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "▲" else "▼") } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DropdownMenu(expanded, { expanded = false }) {
                            accounts.forEach { (id, name) ->
                                DropdownMenuItem(text = { Text("$name (${id.take(14)}…)") }, onClick = { idOrName = id; expanded = false })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(password, { password = it }, label = { Text(s.login.passwordHint) }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val r = runCatching {
                                JSONObject(if (mode == "create") FwmcApi.createAccount(idOrName.trim(), password) else FwmcApi.login(idOrName.trim(), password))
                            }.getOrDefault(JSONObject())
                            if (r.optBoolean("success")) {
                                FwmcSession.refresh()
                            } else {
                                msg = r.optString("error", s.login.operationFailed)
                            }
                        }
                    },
                    enabled = idOrName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (mode == "create") s.login.createAccount else s.login.submit, fontSize = 15.sp) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { mode = if (mode == "create") "login" else "create"; msg = "" }) {
                    Text(if (mode == "create") s.login.toLogin else s.login.toRegister)
                }
                if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(s.login.accountHint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
