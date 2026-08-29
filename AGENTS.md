# AGENTS.md — Android 客户端编码规则

本文件定义 AI 编码助手在本项目（Android 手机端，ZZ App）中必须遵守的规则。

---

## 项目定位

ZZ App 是 FreeWebMovement 网络的一个**平等 P2P 节点**，与桌面/服务器地位相同。UI 为 Jetpack Compose，
Rust Core（`zz-rust-mod-crypto-currency`）通过 JNI（`rs.zz.coin.FwmcApi`）提供全部能力。

## 架构职责（Rust 仓库同步约定）

- **aex = P2P 通用底层**，**fwmc（Rust）= 应用层**。消息、聊天、连接记录等应用业务全在 fwmc 层，
  aex 只做通用 P2P 传输。Android 只通过 `FwmcApi` JNI 调用 fwmc，不得绕过后端逻辑。
- 详见 Rust 仓库 `AGENTS.md` 与 `docs/P2P_NETWORK_LAYERING.md`（`zz-rust-mod-crypto-currency/`）。

## 连接列表数据结构（`FwmcApi.getConnections()` → `peers`）

服务端 `/api/connections` 返回统一对端连接数组 `peers`，每个对端节点一条记录：

```json
{
  "node_id": "FWMC:Zz:...",
  "wallet_addr": "FWMC:Zz:...",
  "ip": "198.18.0.1:20261",
  "all_ips": ["198.18.0.1:20261", "192.168.3.56:20261", "172.17.0.1:20261"],
  "inbound":  { "remotePort": 59702, "localPort": 20260 },
  "outbound": { "remotePort": 20261, "localPort": 20261 },
  "connected": false
}
```

规则：
1. **peer 唯一性 = node_id + `ip:listen_port`**；`ip`/`all_ips` 一律是完整 `ip:port`。
   P2P 是服务对服务，不是 IP 对 IP。
2. `inbound`/`outbound` 是端口对 `{ remotePort, localPort }`，不含 IP。
3. **`connected = inbound 与 outbound 都存在`**（数据填满 = P2P 成功）。
   **数据没填满 = P2P 失败**——失败节点后期再处理，当前只记录与判定。

### Android UI 消费约定

- 「节点连接情况」界面（`ConnectionsScreen`）按 `peers` 渲染统一连接行：
  - 每行 = 一个对端节点：`wallet_addr`（短地址）、`ip`（最重要监听地址）、`connected` 状态标记。
  - 每行地址旁有**聊天按钮**：点击后切到聊天 tab（Sessions）并打开与该节点 `wallet_addr` 的正常聊天。
  - `connected=true` 显示为已连接，`connected=false` 显示为未连接/失败标记。
- **聊天入口形态（用户明确要求）**：在节点连接行地址边上添加聊天按钮，点击后进入聊天页。
- i18n 一律走 `AppStrings` / `StringsZh` / `StringsEn`（`ui/i18n/`），禁止硬编码用户可见文案。
- 依赖仅用 core material3 图标（如 `Icons.Filled.Send`），不引入 icons-extended。

## 聊天通讯前提

**聊天（手机 UI ↔ 网页 UI）的前提是 P2P 双向连接先打通**（两端互为 inbound + outbound，`connected=true`）。
当前 Rust 侧存在回环缺陷（回连目标可能算成本机自己），可能使 `connected=false`——修复在 Rust 仓库，
Android 侧按 `connected` 如实展示，不做补偿逻辑。

## 测试

- 本机多节点模拟：Rust 侧 `fwmc --daemon node --name X --port N --data-dir DIR [--seeds ip:port]`，
  需设 `AEX_ALLOW_LOOPBACK=1`。
- Android 真机：serial `RFCN205LJWM`；adb `/disk2vm/Android/Sdk/platform-tools/adb`；
  APK 用 `./gradlew :app:assembleDebug` + `adb install -r` 部署。
- 编译检查：`./gradlew :app:compileDebugKotlin`。
