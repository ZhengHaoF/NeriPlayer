# NeriPlayer 接入 QQ音乐 · 实施方案（完整版）

> 状态：**仅完成评估与实测验证，尚未开始编码** · 本文最后同步 2026-09-12
> 范围：完整版（匿名播放 + 登录 + 高音质 + 用户内容）
> 落地方式（**已决策**）：**内嵌 Kotlin 适配层**，不引入 Node 中转
> 登录路线（**已决策**）：**QR 扫码登录（路线 A）** —— 连锁依赖见 §5.4 / §7.2
> 参考蓝本：[QQMusicapi](file:///D:/work/QQMusicapi)（本地 `D:\work\QQMusicapi`，Node.js ESM + Koa，系 Python 版 [L-1124/QQMusicApi](https://github.com/L-1124/QQMusicApi) 的 JS 移植）

---

## 1. 结论摘要

- **可行，难度中等偏低**。核心依据：NeriPlayer **已有** QQ音乐的搜索 / 歌曲详情 / 歌词全链路（`QQMusicSearchApi.kt`，纯 Kotlin + OkHttp，匿名可用），缺的只有**播放链路与登录态**。
- **实测确认（本次评估新增，见 §5.5）**：匿名态即可取得播放直链，且**不需要 QIMEI 设备指纹**——走 web 平台参数（`platform=20` / `ct=24`）与现有搜索链路同源，直链真实可播（HTTP 206 + magic bytes 校验通过）。
- **硬边界**：匿名只能拿 **C400(AAC 96k) / M500(MP3 128k)** 两档，12 首样本中仅 3 首可播（约 25%），其余返回 `result=104003`（无权限 / 需 VIP）。**覆盖率是本次接入最大的体验风险**，不是技术风险。
- **QQMusicapi（Node）不能作为依赖直接引入**——它是服务/库形态，与 Android 客户端跨语言。**已决策：移植算法为 Kotlin 内嵌**（与酷狗/B站/网易云架构一致），否决 HTTP 中转方案（存档见 §4.2）。
- **⚠️ 登录路线（QR 扫码 / 路线 A）的连锁依赖 —— 必须提前知晓**：QQ 扫码登录的最后一跳 `QQConnectLogin.LoginServer / QQLogin` 走 **Android 平台协议**，该协议在 QQMusicapi 中会强制触发 `_ensureSession()`，而 session 与 comm 参数都依赖 **QIMEI 设备指纹**。因此选定路线 A，等于**同时承诺移植 QIMEI 注册链路**（RSA-PKCS1 + AES-128-CBC + MD5，约 100 行，公钥与 SECRET 为固定常量）。详见 §5.4 / §6 / §7.2。
  - 利好：项目内已有 [NeteaseCrypto.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/netease/NeteaseCrypto.kt) 提供 `AES/CBC/PKCS5Padding`、`AES/ECB/PKCS5Padding`、RSA 加密、`md5Hex` 现成原语，可直接参照甚至部分复用，QIMEI 移植的实际难度低于从零开始。
- 架构改造面**低于酷狗概念版**：酷狗在**匿名阶段**就需设备注册（`dfid/mid/guid`）与完整音质降级链；QQ音乐的搜索/详情/歌词/平台枚举/歌词偏移设置项**均已存在**，匿名播放**零设备注册**，QIMEI 仅在登录阶段才引入。

---

## 2. 目标与边界

在应用中把 QQ音乐从当前的「元数据 / 歌词匹配源」提升为**独立播放源**，与网易云 / YouTube Music / B 站 / 酷狗概念版并列。

| 模块 | 说明 | 优先级 |
| --- | --- | --- |
| 播放（匿名） | vkey 取址 → 128k 档（C400 / M500），覆盖免费曲目 | P0 |
| 探索页搜索源 | `SearchSource.QQ_MUSIC`，搜索结果可直接入队播放 | P0 |
| 歌词 | 复用现有 QQ音乐歌词链路（已含 base64 解码 + 未翻译占位处理 + AMLL 优先） | P0（已完成） |
| 音质偏好设置 | `qqMusicAudioQuality`，接入设置页与 PlayerManager 音质管道 | P1 |
| 登录 | **QQ 扫码登录（QR）**，解锁高音质与受限曲库 · **含 QIMEI + session 前置** | P1 |
| 高音质 | 登录后逐档上探（M800 / C600 / O800 / F000 等） | P1 |
| 媒体库 tab | 替换现有 `QqMusicPlaylistList` 占位，接入「我的」内容 | P1 |
| 榜单 / 推荐 / 歌单 | 官方榜单、推荐歌单、相似歌曲 | P2 |
| 用户歌单 / 收藏 | 依赖登录态 | P2 |
| 云盘 | QQ音乐云盘（需确认接口可用性） | P3 |
| QRC 逐字歌词 | 需移植自定义 TripleDES（约 344 行 JS） | P3（可延后） |

**边界与说明：**
- 仅做「播放源」，不做 QQ音乐客户端完整复刻；评论、MV、直播、数字专辑购买不在本期范围。
- **不做绕过付费墙的设计**：匿名态只取平台自身下发的免费档位，登录态使用用户自己的账号权限，与官方客户端行为一致。
- 接口仅供个人学习研究，正式对外需关注版权与合规。

---

## 3. 现状分析

### 3.1 NeriPlayer 侧已有基础（重要 —— 比预期完整）

| 已有组件 | 位置 | 现状说明 |
| --- | --- | --- |
| **QQ音乐搜索 / 详情 / 歌词** | [QQMusicSearchApi.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/search/QQMusicSearchApi.kt)（398 行） | ✅ **已实现**。纯 Kotlin + OkHttp，**匿名可用**：搜索走 `c.y.qq.com/soso/fcgi-bin/client_search_cp`，详情/歌词走 `u.y.qq.com/cgi-bin/musicu.fcg`；含 base64 歌词解码、`//` 未翻译占位处理、AMLL 逐字歌词优先 |
| 平台枚举 | `core/api/search/SongData.kt` → `MusicPlatform.QQ_MUSIC` | ✅ 已存在，**仅作为元数据/歌词源**使用 |
| 搜索源注册 | [SearchManager.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/search/SearchManager.kt) L78 / L172 | ✅ 已注册（用于歌词自动匹配的候选源） |
| 手动元数据搜索界面 | [NowPlayingViewModel.kt](../../app/src/main/java/moe/ouom/neriplayer/ui/viewmodel/NowPlayingViewModel.kt) L92 | ✅ `ManualSearchState.selectedPlatform` 的**声明默认值即 `QQ_MUSIC`**（运行时初始值另有条件：网易云 cookie 缺失 → `QQ_MUSIC`，否则 `CLOUD_MUSIC`，见 L109-116） |
| 歌词偏移设置项 | `qq_music_lyric_default_offset_ms`（默认 500ms） | ✅ 已存在 |
| 媒体库 tab 骨架 | `LibraryTab.QQMUSIC` + `qqMusicListState` + `QqMusicPlaylistList` | ⚠️ 存在但为「开发中」占位（`LibraryScreen.kt:3470`，纯 `TODO`） |
| DI 容器 | `AppContainer.qqMusicSearchApi`（L405） | ✅ 已注册 |
| 播放源枚举 | `core/player/model/PlaybackAudioInfo.kt` → `PlaybackAudioSource` | ❌ **无 `QQ_MUSIC`**（现有：LOCAL / NETEASE / BILIBILI / YOUTUBE_MUSIC / KUGOU） |
| URL 解析分发 | [PlayerManagerUrlExtensions.kt](../../app/src/main/java/moe/ouom/neriplayer/core/player/url/PlayerManagerUrlExtensions.kt) L336 起 | ❌ 无 QQ音乐分支 |
| 音质偏好管道 | `PlaybackPreferenceSnapshot` / `AutoSettingsSchema` / `PlayerManagerLifecycleExtensions` | ❌ 无 QQ音乐音质项 |

**⚠️ 关键缺口（唯一的 P0）**：`PlaybackAudioSource` 无 `QQ_MUSIC`、`resolveSongUrl` 无对应分支、无取址实现 —— 即**搜得到、看得见歌词，但播不了**。

### 3.2 探索页搜索源现状

`SearchSource`（`ui/viewmodel/tab/ExploreViewModel.kt` L103）当前为：

```
YOUTUBE_MUSIC, NETEASE, BILIBILI, KUGOU, LINK_RECOGNITION
```

**无 `QQ_MUSIC`** —— 这是个独立缺口。即使播放链路做通，用户在探索页也搜不到 QQ音乐歌曲。需新增该枚举值（酷狗当时同理）。

### 3.3 可复刻的范式

| 参照 | 位置 | 可复用的内容 |
| --- | --- | --- |
| 酷狗播放 | [KugouPlayback.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/kugou/KugouPlayback.kt)（218 行） | `SongUrlResult` 构造、音质档位降级链、`audioInfo` 回填、`cacheKeyOverride` |
| 酷狗模型 | [KugouModels.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/kugou/KugouModels.kt) | `SongItem` 的 `album` 前缀标记 + `channelId` + 字符串 id 承载范式（**QQ音乐 songmid 同为字符串，可直接照搬**） |
| 酷狗会话 | `KugouSession.kt` / `KugouCookieStore` | 登录态 `loggedInFlow` 广播、cookie 加密持久化 |
| 酷狗登录 UI | `KugouQrLoginSheet`（`internal`，`ui.screen.tab` 包） | 二维码底部弹窗范式，可直接复刻为 QQ 版 |
| 网易云登录 | `NeteaseQrLoginClient.kt`（448 行）/ `NeteaseWebLoginActivity` | QR 轮询 + WebView 取 cookie 两套范式 |
| 音质设置 | `kugouAudioQuality` 全链路（6 个文件） | 直接照抄为 `qqMusicAudioQuality` |
| **密码学原语** | [NeteaseCrypto.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/netease/NeteaseCrypto.kt)（185 行） | `AES/CBC/PKCS5Padding`、`AES/ECB/PKCS5Padding`、RSA 加密、`md5Hex` —— **QIMEI 移植可直接参照/复用** |
| WebView + JS 桥范式 | [NeteaseYdDeviceTokenProvider.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/netease/NeteaseYdDeviceTokenProvider.kt)（283 行） | WebView 注入 JS / 拦截 cookie / 超时降级 —— 若未来需要设备指纹类补充可参照 |

---

## 4. 总体架构设计

### 4.1 方案：Kotlin 内嵌适配层（**已选定** · 无本地代理）

```
┌──────────────────────── NeriPlayer (Android) ────────────────────────┐
│                                                                      │
│  UI 层：探索页(新增 QQ音乐搜索源) / 媒体库 tab(替换占位) / 设置(音质)  │
│     │                                                                │
│  core/player（PlayerManager）                                        │
│     │  SongUrlResult ← PlayerUrlResolver ← PlaybackAudioSource.QQ_MUSIC
│     ▼                                                                │
│  core/api/qqmusic（新增包 · 待实现）                                  │
│   ├── QQMusicSession      （guid 持久化、登录态、musickey 存储）        │
│   ├── QQMusicDevice       （QIMEI 注册 + getSession · 仅登录阶段需要）  │
│   ├── QQMusicPlayback     （vkey 取址 + 音质降级链 → SongUrlResult）   │
│   ├── QQMusicModels       （SongItem 构建 / QQMUSIC_CHANNEL_ID）      │
│   ├── QQMusicChannel      （榜单 / 推荐 / 歌单 · P2）                 │
│   ├── QQMusicUserApi      （用户歌单 / 收藏 · P2）                    │
│   ├── QQMusicQrLoginClient（QQ 扫码登录 · P1 · 依赖 QQMusicDevice）    │
│   └── QQMusicSigner       （zzc_sign / hash33 · 仅签名接口需要）       │
│  data/auth/qqmusic/QQMusicCookieStore（musickey/uin 加密持久化）      │
│                                                                      │
│  core/api/search/QQMusicSearchApi（已存在，仅需小幅扩展）             │
│                                                                      │
└────────────── 无本地代理：App 内 OkHttp 直连 QQ音乐域名 ───────────────┘
```

**要点：**
- **匿名阶段（阶段 1–3）：无需 QIMEI、无需 session**。播放走 web 平台协议（`platform=20` / `ct=24`，见 §5.5 实测），相比酷狗（需 `dfid/mid/guid` 注册）是显著的减法。
- **登录阶段（阶段 4）：必须补 `QQMusicDevice`**。路线 A 的 `QQLogin` 走 Android 协议 → 需 session → 需 QIMEI。这是选定 QR 路线后的既定成本，不是可选项。
- **仍需持久化 `guid`**：`CgiGetVkey` 的 `guid` 参数应在安装后生成一次并持久化（建议取设备指纹派生），避免每次随机导致风控画像漂移。登录阶段的 QIMEI 与它同源复用同一身份。
- 与现有全部平台一致：App 内 OkHttp 直连，无中间服务。

### 4.2 已否决方案：Node 中转服务（存档）

> **决策：不采用**（用户 2026-09-12 确认走内嵌 Kotlin）。

原设想：把 `D:\work\QQMusicapi` 部署到已有 Lighthouse，App 侧只做 HTTP 客户端。对比表存档如下：

| 维度 | **内嵌 Kotlin（已选）** | Node 中转（已否决） |
| --- | --- | --- |
| 算法移植 | 需移植（工作量可控，见 §6） | 零 |
| 额外依赖 | 无 | **需常驻服务器** |
| 架构一致性 | 与 kugou / bili / netease 全部同源 | 孤例 |
| 隐私 | 账号凭证不出本机 | cookie 经服务器 |
| 离线 / 弱网 | 与主 App 同生命周期 | 依赖公网服务可用性 |
| 协议变更维护 | 改 App 发版 | 只改服务端 |

**否决理由**：内嵌方案在架构一致性、隐私、无额外依赖三项上全面占优，且移植量可控（匿名阶段几乎零密码学工作）。中转方案仅在「协议频繁变更时的机动性」上有优势，不足以抵消成本。若未来出现协议高频变动或需重资产能力（QRC 逐字歌词、17 档音质全量），可重新评估该方案。

---

## 5. 核心接口清单

> 域名与参数以「本次实测」为准（已验证可用），QQMusicapi 的 Node 实现作为交叉参照。

### 5.1 搜索（已实现，无需改动）

| 功能 | 接口 | 备注 |
| --- | --- | --- |
| 歌曲搜索 | `GET https://c.y.qq.com/soso/fcgi-bin/client_search_cp` | 参数 `format=json&n=20&p={page}&w={kw}&cr=1&g_tk=5381`；**匿名可用** |
| 歌曲详情 | `GET https://u.y.qq.com/cgi-bin/musicu.fcg?data={songinfo}` | `music.pf_song_detail_svr` / `get_song_detail_yqq`，param `song_mid` |
| 歌词 | 同上 `music.musichallSong.PlayLyricInfo` / `GetPlayLyricInfo` | 参数 `songMID` + `trans=1&qrc=0&crypt=0`；返回 base64 明文歌词 |

### 5.2 播放取址（P0 核心，待实现）

| 能力 | 接口 | 说明 |
| --- | --- | --- |
| **匿名取址（采用）** | `GET https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&data={...}` | module `vkey.GetVkeyServer` / method `CgiGetVkey`；param `guid` / `songmid[]` / `songtype[]` / `uin:"0"` / `loginflag:1` / `platform:"20"`；comm `{uin:0, format:"json", ct:24, cv:0}` |
| 带音质指定 | 同上，加 `filename: ["{QUALITY}{mid}{mid}.{ext}"]` | **实测用 `songmid` 即可成功**（服务端按 songmid 自行解析出正确的 media_mid 生成 purl）；QQMusicapi 注释称部分 VIP 音质需 `media_mid`，故实现取「优先 `media_mid`、缺失回退 `songmid`」（存于 `SongItem.subAudioId`）；不传 filename 时服务端默认给 C400 |
| 登录态取址 | 同上，`uin` 换成真实 musicid，并携带 `qm_keyst` / `qqmusic_key` cookie | 解锁更高档位与受限曲目（**待验证**） |
| 项目蓝本参照 | QQMusicapi `song.js` → `music.vkey.GetVkey` / `UrlGetVkey`（`platform:"23"`） | 该路径走 Android 协议，**需 QIMEI + session**，本项目**不采用** |

**响应结构（扁平在 `req_0.data` 下）：**

```
req_0.data.sip[]            → CDN 前缀数组（取 [0]，实测 http://aqqmusic.tc.qq.com/）
req_0.data.midurlinfo[0]    → { songmid, purl, vkey, result }
                              purl 非空 → 直链 = sip[0] + purl
                              purl 为空 → 看 result 判定原因

result 语义（QQMusicapi exceptions/注释已对齐）：
  0      → 成功
  104003 → 无权限 / 需要 VIP    ← 匿名态最常见的失败码
  104004 → VKey 获取失败
```

### 5.3 音质档位表（实测标注）

| 代码 | 说明 | 匿名实测 |
| --- | --- | --- |
| `F000` | SQ 无损 | ❌ 104003 |
| `O800` / `O600` / `O400` | OGG 320 / 192 / 96 | `O800` ❌ 104003 |
| `M800` | MP3 320 | ❌ 104003 |
| **`M500`** | **MP3 128** | ✅ **可取，实测可播（`ID3` 头）** |
| **`C400`** | **AAC 96** | ✅ **可取，实测可播（`ftyp` 头）** |
| `C600` | AAC 192 | ❌ 104003 |
| `C200` | AAC 48 | 未测 |
| `AI00` / `Q000` / `Q001` / `Q003` / `D004` / `DT03` / `TL01` | 臻品母带 / 全景声 / 杜比 / DTS 等 | 未测（预期全部需会员） |

**建议降级链（对齐酷狗的设计，从用户偏好档起逐档下探）：**

```kotlin
QQMUSIC_QUALITY_FALLBACK_ORDER = ["F000", "M800", "C600", "M500", "C400"]
// 未登录 → 只保留 ["M500", "C400"]，避免必然被拒的会员档请求
```

> 注：`M500` 与 `C400` 谁优先可作为偏好项。AAC 96k 与 MP3 128k 听感接近，但 QQ音乐官方对免费曲目默认下发 C400，建议**默认 `M500` 优先**（兼容性更好、体积略小）。

### 5.4 登录与用户内容（P1 / P2）

| 功能 | 接口 | 说明 |
| --- | --- | --- |
| **QIMEI 注册（前置依赖）** | `POST https://api.tencentmusic.com/tme/trpc/proxy` | **路线 A 的强制前置**。header 含 `method: GetQimei` / `service: trpc.tme_datasvr.qimeiproxy.QimeiProxy` / `appid: qimei_qq_android`；body 的 `key` = RSA-PKCS1 加密随机 `crypt_key`，`params` = AES-128-CBC 加密的 payload，`sign` = MD5(key, params, ts*1000, nonce, SECRET, extra)。响应取 `q16` / `q36`，**24 小时有效期**，需持久化 |
| **getSession（前置依赖）** | `musicu.fcg` 的 `music.getSession.session` / `GetSession` | Android 平台 comm 下换 `session.uid/sid/vkey`，同样 24 小时有效期。`QQLogin` 前必须先建立 |
| 二维码获取 | `GET https://ssl.ptlogin2.qq.com/ptqrshow` | 参数 `appid=716027609` + `pt_3rd_aid=100497308`；从 Set-Cookie 取 **`qrsig`**（后续轮询凭据） |
| 扫码状态轮询 | `GET https://ssl.ptlogin2.qq.com/ptqrlogin` | 参数含 `ptqrtoken = hash33(qrsig)`；响应是 JS 文本，需正则提取状态码；状态：0=过期 / 1=等待 / 2=待确认 / **4=成功**（返回 `sigx` + `uin`） |
| 换取凭证 | `POST https://ssl.ptlogin2.graph.qq.com/check_sig` → `POST https://graph.qq.com/oauth2.0/authorize` → `musicu.fcg` 的 `QQConnectLogin.LoginServer` / `QQLogin` | 三步链路：`check_sig` 取 `p_skey`（**cookie 名兼容 `p_skey` / `p-skey` / `pskey` / `ptsigx` / `skey`**，见 `login.js:421-426`）→ `authorize` 换 `code`（从 302 Location 正则提取）→ `QQLogin` 换 **`musickey` / `musicid` / `refreshKey`**；`comm.tmeLoginType = 2` |
| 手机验证码 | `music.login.LoginServer` / `SendPhoneAuthCode` + `Login` | P2，非必须 |
| 凭证刷新 | `music.login.LoginServer` / `Login`（`loginMode: 2`，见 `login.js:refreshCredential`） | 登录态维护。按 `loginType` 分三套 param（1 / 2 / default） |
| 用户歌单 | QQMusicapi `user.js`（`user/songlist`） | 具体 module/method 需在实现时以抓包校验 |
| 榜单 / 推荐 | 需在实现时以抓包校验（QQMusicapi 未完整覆盖） | P2，**信息缺口见 §10** |

**⚠️ 登录链路的关键约束（已决策路线 A，本节即既定成本）**：

QQMusicapi 的 `QQLogin` 与 `refreshCredential` **均走 Android 平台协议**——`requestApi` 中 `if (platform === Platform.ANDROID) await this._ensureSession()`，而 session 与 Android comm 参数都依赖 QIMEI（`buildComm` 的 `QIMEI` / `QIMEI36` 字段）。因此：

```
路线 A（QR 扫码）
  └─ 强制前置：QIMEI 注册（RSA-PKCS1 + AES-128-CBC + MD5）
       └─ 强制前置：getSession（music.getSession.session / GetSession）
            └─ 才能调用 QQLogin 换取 musickey
```

**结论**：选定路线 A ⇒ **必须移植 QIMEI 注册 + session 获取**（归入 `QQMusicDevice` 模块）。这不是可选项，也不是"可选增强"。好消息是项目内已有 `NeteaseCrypto` 提供全部所需密码学原语，且 QIMEI 的公钥/SECRET/APP_KEY 均为固定常量（见 §6），实际编码量约 100–150 行。

### 5.5 本次实测记录（匿名可行性验证，2026-09-12）

**探针方法**：独立 Node.js 脚本（Node 22.22.2 内置 `fetch`），置于系统临时目录，**未读写任何项目文件**；直接构造 `musicu.fcg` 请求，不依赖 QQMusicapi 代码。

**第一批：验证直链可取性（关键词「起风了」）**

| 歌曲 | songmid | result | purl |
| --- | --- | --- | --- |
| 起风了 (旧版) | `0004jPDk2eB2dt` | 104003 | 空 |
| 起风了 | `002w57E00BGzXn` | 104003 | 空 |

**第二批：扩大样本至 12 首（6 组关键词 × 2 首）**

| 歌曲 | result | 结论 |
| --- | --- | --- |
| 小苹果（`001zOWbJ3Q94ca`） | **0** | ✅ 有直链 |
| 好运来（`003isWhW3PMWMP`） | **0** | ✅ 有直链 |
| 友谊地久天长（`000cKb230cuG8W`） | **0** | ✅ 有直链 |
| 其余 9 首 | 104003 | ❌ 无权限 / 需 VIP |

→ **匿名覆盖率约 25%（3/12）**，可播曲目集中在版权宽松的老歌 / 儿歌 / 免费曲。

**第三批：直链可播性与音质边界验证**

| 歌曲 | C400 | C600 | M500 | M800 | O800 | F000 |
| --- | --- | --- | --- | --- | --- | --- |
| 小苹果 | ✅ 可播 | ❌ 104003 | ✅ 可播 | ❌ 104003 | ❌ 104003 | ❌ 104003 |
| 好运来 | ✅ 可播 | ❌ 104003 | ✅ 可播 | ❌ 104003 | ❌ 104003 | ❌ 104003 |
| 友谊地久天长 | ✅ 可播 | ❌ 104003 | ✅ 可播 | ❌ 104003 | ❌ 104003 | ❌ 104003 |

**可播性验证细节**（HTTP Range 请求，取前 64KB）：

| 档位 | HTTP | Content-Type | 文件头 magic | 判定 |
| --- | --- | --- | --- | --- |
| `C400` | 206 | `audio/mp4` | `00000020 66747970`（`....ftyp`） | ✅ 真实 m4a |
| `M500` | 206 | `audio/mpeg` | `49443303`（`ID3`） | ✅ 真实 mp3 |

**直链样例**（含 vkey，有时效，勿复用）：

```
http://aqqmusic.tc.qq.com/M500002tCAn43sbfuz.mp3?guid=10000&vkey=6D3F4D79...
http://aqqmusic.tc.qq.com/C400002tCAn43sbfuz.m4a?guid=10000&vkey=B1A27F48...
```

**旧版 CDN 直连（无 vkey）验证**：`ws.stream.qqmusic.qq.com` / `dl.stream.qqmusic.qq.com` 拼接均返回 **403**，确认**必须走 vkey 接口**，不存在免签直链捷径。

**QQMusicapi 项目本地状态**：

| 文件 | 状态 |
| --- | --- |
| `node_modules/` | **未安装** |
| `credential.json` | **全空**（从未登录过） |
| `device.json` | 已有 `qimei` / `qimei36` / `sessionUid` / `sessionSid`，生成时间 2026-07-13 → **匿名 Android 协议链路曾跑通**（现已过 24h 有效期） |

### 5.6 榜单 / 歌单接口（第三轮探针 2026-09-12 已全部验证匿名可用 ✅）

| 功能 | 接口 | 编码 | 要点 |
| --- | --- | --- | --- |
| 排行榜列表 | `GET c.y.qq.com/v8/fcg-bin/fcg_myqq_toplist.fcg` | UTF-8 | `data.topList[]`：`id` / `topTitle` / `picUrl` / `listenCount` / 3 首预览 |
| 榜单歌曲 | `GET c.y.qq.com/v8/fcg-bin/fcg_v8_toplist_cp.fcg?topid={id}` | UTF-8 | `songlist[].data`：老格式 `songmid` / `songname` / `singer[]` / `albummid` / `interval` / `pay.payplay` |
| 热门歌单列表 | `GET c.y.qq.com/splcloud/fcgi-bin/fcg_get_diss_by_tag.fcg` | **GBK** | `data.list[]`：`dissid` / `dissname` / `imgurl` / `listennum` / `creator.name` |
| 歌单歌曲 | `GET c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg?disstid={id}` | **GBK** | ⚠️ 必须带 **`Referer: https://c.y.qq.com/`** + **`Cookie: uin=0`**，否则报 `invalid referer` / `check privacy error`；`cdlist[0].songlist[]` |

已验证的**失败路径**（避免重蹈）：`musicToplist.ToplistInfoServer.GetOverview` → 500005；`music.sngList.getSongList` 与 `playlist.PlaylistServ.get_song_list` → 500003；qzone 接口带 `y.qq.com` referer → `invalid referer`，带 `y.qq.com/portal/player.html` → `check privacy error`。

**实现落地**：`QQMusicChannel.kt`（数据层，GBK 按 `GB18030` 解码）+ `QQMusicExploreContent.kt`（UI，对齐酷狗 tab 形态），见 commit `841843ef`。信息缺口 §10 第 1、2 条中「榜单 / 歌单列表」已补齐；**歌单详情实际走 qzone 接口而非 musicu**，第 2 条（用户歌单）仍属登录态范畴。

---

## 6. 加密与签名（需移植的部分）

**匿名阶段（阶段 1–3）不需要任何签名** —— 实测的 `CgiGetVkey` 请求**未携带 `sign` 参数**即可成功。**登录阶段（阶段 4）按已决策的路线 A，必须移植 QIMEI 相关密码学**。完整清单如下：

| 算法 | 用途 | 复杂度 | 是否必需 |
| --- | --- | --- | --- |
| **`hash33`** | QR 登录 `ptqrtoken`、Web 平台 `g_tk` | 极低（约 5 行）。`h = (h << 5) + h + charCode` 迭代后 `& 0x7FFFFFFF`；**默认种子 0，算 `g_tk` 时种子传 5381** | ✅ 登录必需 |
| **`zzcSign`** | `musics.fcg` 签名（`sign` 查询参数） | **低**（40 行 JS → 约 30 行 Kotlin）。SHA1 十六进制 → 按固定索引表取字符 → 20 字节 XOR 打散 → Base64 去 `\/+=` → 拼 `zzc` 前缀转小写 | ⚠️ 仅部分接口需要，匿名播放**不需要** |
| **RSA-PKCS1 + AES-128-CBC + MD5**（QIMEI 注册） | Android 协议设备指纹上报（`key` / `params` / `sign`） | 中（约 100–150 行）。全部是 `java.security` / `javax.crypto` 标准原语，**项目内 [NeteaseCrypto.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/netease/NeteaseCrypto.kt) 已有 AES-CBC / RSA / MD5 可参照复用**；公钥与 SECRET 为固定常量 | ✅ **登录必需**（路线 A 决策后） |
| **自定义 TripleDES（344 行 JS）** | QRC 加密歌词解密 | 中高。**注意：标准 `des-ede3-ecb` 解不出正确结果**，是 1:1 移植自 C# 的自定义实现 | ❌ 现有 base64 明文歌词已够用，可延后（P3） |
| RSA-PKCS1（登录加密） | 手机号登录加密 | 中 | P2 可选 |

**`zzcSign` 算法要点**（来自 QQMusicapi `algorithms/sign.js`，需 1:1 复刻）：

```
输入 payload 字符串
1. sha1(payload) → 大写 hex（40 字符）
2. part1 = hex[23,14,6,36,16,7,19]        // 固定索引
3. part2 = hex[16,1,32,12,19,27,8,5]      // 固定索引
4. part3[i] = SCRAMBLE_VALUES[i] XOR parseInt(hex.substr(i*2,2), 16)   // i ∈ [0,20)
   SCRAMBLE_VALUES = [89,39,179,150,218,82,58,252,177,52,
                      186,123,120,64,242,133,143,161,121,179]
5. b64 = base64(part3) 并移除 / \ + = 字符
6. 返回 ("zzc" + part1 + b64 + part2).lowercase()
```

**QIMEI 公钥 / SECRET**（路线 A 登录必需，均为固定常量，直接写入 `QQMusicDevice`）：

```
PUBLIC_KEY = MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEIxgwoutfwoJxcGQeedgP7FG9qaIuS0qzfR8gWkrkTZKM2iWHn2ajQpBRZjMSoSf6+KJGvar2ORhBfpDXyVtZCKpqLQ+FLkpncClKVIrBwv6PHyUvuCb0rIarmgDnzkfQAqVufEtR64iazGDKatvJ9y6B9NMbHddGSAUmRTCrHQIDAQAB
SECRET     = "ZdJqM15EeO2zWc08"
APP_KEY    = "0AND0HD6FE4HY80F"
CHANNEL_ID = "10003505"
QIMEI_HOST = "https://api.tencentmusic.com/tme/trpc/proxy"
```

---

## 7. 风控、登录态与「不可播」处理

### 7.1 匿名态的现实约束（务必在 UI 体现）

| 场景 | 平台返回 | 处理策略 |
| --- | --- | --- |
| 免费曲目，匿名 | `result=0` + purl | 正常播放，音质 `M500`/`C400` |
| **VIP / 数字专辑曲目，匿名** | **`result=104003`** | **不静默失败**：提示「该歌曲需 QQ音乐会员」+ 引导登录 + 可选自动切源 |
| vkey 失效（约 2 小时） | purl 请求返回 403 | 走现有 `SongUrlResolutionRetry` 重解析机制 |
| 请求过于频繁 | 疑为限流 / 空响应 | 播放取址类接口限制 QPS（参照酷狗的 3 QPS 建议） |
| 设备画像漂移 | 未知 | 持久化 `guid`，不随机 |

**⚠️ 覆盖率风险（本项目 QQ音乐独有的最大体验问题）**：匿名只能覆盖约 25% 的曲目。这意味着与其他源相比，QQ音乐**必须**具备：
1. 清晰的「需会员 / 不可播」失败文案（不能显示通用错误）；
2. **自动切源兜底**（QQ音乐 → 网易云 / B 站）。

> ⚠️ 注意：**酷狗概念版→B 站/网易云的自动切源目前同样未实现**（见酷狗方案 §10 阶段 5 缺口）。若 QQ音乐接入依赖切源能力，需先把这条基础能力补上，否则用户体验会显著低于预期。**建议作为 QQ音乐接入的前置依赖单独评估。**

### 7.2 登录路线（**已决策：路线 A · QQ 扫码登录**）

> **决策记录**：用户 2026-09-12 确认走 **路线 A（QQ 扫码登录，复刻 QQMusicapi）**，否决路线 B（WebView cookie 提取）。

方案对比（存档）：

| | **路线 A：QQ 扫码（已选）** | 路线 B：WebView cookie（已否决） |
| --- | --- | --- |
| 流程 | `ptqrshow` → `ptqrlogin` 轮询 → `check_sig` → `authorize` → `QQLogin` | WebView 打开 `y.qq.com` 登录页 → 拦截 cookie |
| 密码学依赖 | `hash33` + **QIMEI（RSA/AES/MD5）** + session | 无（浏览器自己完成） |
| 产出凭证 | `musickey` + `musicid` + `refreshKey`（协议原生，**确定可用**） | cookie 中的 `qm_keyst` + `uin`（**是否为同一凭据未验证**） |
| 链路稳定性 | 4 个接口任一变更即失效，维护成本较高 | 页面级，相对稳定 |
| 与现有实现一致性 | 与酷狗 QR 范式一致（`KugouQrLoginSheet` 可复刻） | 与网易云 Web 登录范式一致 |
| 主要风险 | QIMEI 移植 + 三步跳转 + `p_skey` cookie 名兼容 | 前提未验证：web cookie 是否被 API 接受 |

**选定路线 A 的理由**：产出的是协议原生 `musickey`，与官方客户端行为一致，**确定可用**（路线 B 存在"cookie 不被 API 接受"的根本性不确定）。代价是需移植 QIMEI + session，但项目内已有密码学原语可复用，成本可控。

#### 路线 A 的完整实施链路

```
[前置 1] QIMEI 注册          POST api.tencentmusic.com/tme/trpc/proxy
         (QQMusicDevice)      → q16 / q36（24h 有效，持久化）

[前置 2] getSession          musicu.fcg · music.getSession.session / GetSession
         (QQMusicDevice)      → session.uid / sid / vkey（24h 有效，持久化）

[步骤 1] 取二维码            GET ssl.ptlogin2.qq.com/ptqrshow
                              → 图片字节（展示）+ Set-Cookie 的 qrsig（轮询凭据）

[步骤 2] 轮询扫码状态         GET ssl.ptlogin2.qq.com/ptqrlogin
         (2s 间隔)             ptqrtoken = hash33(qrsig)
                              → 0/1/2/4；4 成功时正则提取 sigx + uin

[步骤 3] 换 p_skey + code    POST ssl.ptlogin2.graph.qq.com/check_sig
                              POST graph.qq.com/oauth2.0/authorize
                              → p_skey（cookie，名有多种兼容）→ 302 Location 提 code

[步骤 4] 换凭证              musicu.fcg · QQConnectLogin.LoginServer / QQLogin
                              comm.tmeLoginType = 2
                              → musickey / musicid / refreshKey → 落盘
```

**实施注意点**：
1. 步骤 2 的响应是 **JS 文本而非 JSON**，需按 QQMusicapi 的 `QQ_STATUS_RE` / `QQ_ARGS_RE` 双正则解析（见 `login.js:279-296`）。
2. 步骤 3 的 `check_sig` **必须 `allowRedirects: false`**，否则拿不到 302 Location。
3. 步骤 4 与 `refreshCredential` 都走 Android comm → **依赖前置 1、2 已就绪**，且 QIMEI/session 过期（24h）时需先静默刷新再重试登录。
4. `refreshKey` 用于后续凭证续期（`music.login.LoginServer / Login`，`loginMode: 2`，按 `loginType` 分三套参数）。

**仍待验证**：登录后高音质档位（`F000` / `M800` / `C600` / `O800`）是否真正解锁 —— 需真实账号做 1 次手工实验（阶段 4 开工前完成）。

### 7.3 凭证与登录态存储

参照 `KugouCookieStore`：
- `data/auth/qqmusic/QQMusicCookieStore`：`musickey` / `musicid` / `refreshKey` / `guid` 加密持久化（`EncryptedSharedPreferences`，异常回退明文）；
- **设备身份独立持久化**（`QQMusicDevice` 负责，与登录态解耦）：`guid` / `q16` / `q36` / `session.uid` / `session.sid` / `session.vkey` + 各自的时间戳；QIMEI 与 session 均 **24h 有效**，过期需静默重注册——**签发时间必须落盘**，否则每次冷启动都要重新注册，既慢又增加风控暴露面；注册应做**并发串行化**（对齐 QQMusicapi `QimeiManager._lock` 的实现）；
- `QQMusicSession.loggedInFlow` 广播登录态，驱动 UI（音质档位、媒体库空态、失败文案）；
- **退出登录保留 `guid` / `q16` / `q36`**（设备身份不重置，避免风控画像漂移）。

---

## 8. 实施计划（分阶段）

### 阶段 0：可行性验证（**已完成 2026-09-12，本文件即结论**）

- [x] 匿名取址可行性实测 —— ✅ 通过（3/12 可播，`M500`/`C400` 直链真实可播）
- [x] 匿名是否需要 QIMEI —— ✅ **不需要**（web 平台协议）
- [x] 免签 CDN 直连是否可行 —— ✅ 否（403），必须走 vkey
- [x] 音质边界测定 —— ✅ 匿名封顶 `M500` / `C400`
- [ ] **待验证**：登录后高音质解锁（需真实账号，1 次手工实验，阶段 4 开工前完成）
- [ ] **待验证**：QIMEI 注册链路在 Kotlin 侧可跑通（可用 QQMusicapi 的固化常量 + 一次性脚本先行验证，避开 Android 环境）

### 阶段 1：基础设施 + 匿名播放（P0）

- [ ] `core/api/qqmusic/QQMusicModels.kt`：`SongItem` 构建
  - `album = "QQMusic|{songmid}"`（供识别），`channelId = "qqmusic"`，`audioId = songmid`
  - **注意 `SongItem.id` 是 `Long`，而 songmid 是字符串** → 照搬酷狗范式：`id = songmid.hashCode().toLong()`，真实标识放 `audioId`
- [ ] `core/api/qqmusic/QQMusicSession.kt`：`guid` 持久化（安装后生成一次并复用）、`QQMusicQuality` 常量
  - `guid` 独立于登录态的**设备身份存储**，退出登录不重置
- [ ] `core/api/qqmusic/QQMusicPlayback.kt`：`resolveQQMusicPlaybackUrl(song)` → `CgiGetVkey` → `SongUrlResult`
  - 降级链 `["M500", "C400"]`（未登录）；`sip[0] + purl` 组装；`result` 码映射为失败原因
- [ ] `PlaybackAudioSource.QQ_MUSIC` 枚举 + 全链路 exhaustive `when` 补全（**编译器会强制列出所有遗漏点**）
- [ ] `PlayerUrlResolver`：`qualityLabelForQQMusic` / `buildQQMusicQualityOptions` / `buildQQMusicQualityCandidates` / `QQMUSIC_QUALITY_FALLBACK_ORDER`
- [ ] `PlayerManager`：`QQMUSIC_SOURCE_TAG`、`isQQMusicTrack()`、`computeCacheKey` 分支（`qqmusic-$songmid-$quality`）
- [ ] `PlayerManagerUrlExtensions.resolveSongUrl` 新增 `isQQMusicTrack(song) -> getQQMusicSongUrl(...)` 分支
- [ ] AppContainer 注册 `qqMusicSession`
- [ ] **验收：真机能搜到 QQ音乐免费歌曲（如「小苹果」「好运来」）并播出 128k**

### 阶段 2：探索页搜索源（P0，与阶段 1 合并交付）

- [ ] `SearchSource.QQ_MUSIC` 枚举 + `ExploreViewModel` 的 `searchQQMusic` / `fetchQQMusicSearchPage` / 错误文案分支
- [ ] `ExploreScreen` 搜索源 chip（注意 `youtubeEnabled` 时的顺序差异）
- [ ] `error_qqmusic_search` 等字符串（3 个语言目录）
- [ ] **验收：探索页切到 QQ音乐源 → 搜索 → 点歌入队播放**

### 阶段 3：音质偏好设置（P1）

- [ ] `qqMusicAudioQuality` 全链路（照抄 `kugouAudioQuality`，共 6 处）：
  `SettingsKeys` → `SettingsRepository` → `PlaybackPreferenceSnapshot`（含 `normalizeQQMusicQualityKey` 与 `DEFAULT_*` 常量）→ `AutoSettingsSchema` → `NeriApp.kt` 偏好 collect → `PlayerManagerLifecycleExtensions` 音质管道
- [ ] `PreferredQualityKeys.qqMusic` + `forSource()` 映射
- [ ] `SettingsAudioQualitySection` 新增 QQ音乐音质选项 + i18n
- [ ] 登录态差异：未登录时档位选项应灰显或标注「需登录」
- [ ] **验收：设置页切换音质 → 播放请求按新档位发起**

### 阶段 4：登录 + 高音质（P1）· 路线 A（QR 扫码）

**阶段 4a：QIMEI 设备身份（路线 A 的强制前置）**

- [ ] `core/api/qqmusic/QQMusicDevice.kt`：
  - QIMEI 注册（`api.tencentmusic.com/tme/trpc/proxy`）—— RSA-PKCS1 加密 `crypt_key` + AES-128-CBC 加密 payload + MD5 签名；参照/复用 `NeteaseCrypto` 的 AES/RSA/MD5 原语
  - `getSession`（`music.getSession.session / GetSession`）
  - 24h 有效期 + 签发时间落盘 + **注册并发串行化**（对齐 QQMusicapi `QimeiManager._lock`）
  - `beaconId` 生成（40 段，`k1/k2/k13...` 用月初时间戳 + 随机数，`k3` 全 0，`k4` 为 16 位非零 hex，其余随机数）
- [ ] 设备身份独立于登录态持久化；退出登录不重置
- [ ] **验收**：冷启动可复用未过期的 QIMEI + session，不重复注册

**阶段 4b：QR 登录**

- [ ] **开工前置**：完成 §10 信息缺口第 3 项验证（登录后高音质是否真解锁）
- [ ] `QQMusicSigner`：`hash33`（QR 轮询 `ptqrtoken` 需要）
- [ ] `QQMusicQrLoginClient`：`ptqrshow` → `ptqrlogin` 轮询（2s，`hash33(qrsig)`）→ `check_sig`（**必须 `allowRedirects: false`**，cookie 名多形态兼容）→ `authorize` 提 `code` → `QQLogin`（Android comm，依赖 4a）
- [ ] 扫码状态 JS 文本解析（双正则范式，见 `login.js:279-296`）
- [ ] `QQMusicCookieStore` 加密持久化 + `QQMusicSession.loggedInFlow`
- [ ] `QQMusicQrLoginSheet`（可复刻 `KugouQrLoginSheet`，`internal`）接入设置页「平台登录」
- [ ] 登录态取址：`uin` = musicid + `qm_keyst` / `qqmusic_key` = musickey → 开放 `F000` / `M800` / `C600` / `O800` 档位
- [ ] `refreshKey` 续期（`music.login.LoginServer / Login`，`loginMode: 2`），失败降级匿名态
- [ ] 音质档位回填：以响应实际下发的 `purl` 扩展名 + 文件名档位为准（对齐酷狗的「请求无损但回落 mp3」处理）
- [ ] **验收：设置页扫码登录成功 → VIP 歌曲可播 → 音质上探到无损档**

### 阶段 5：媒体库 tab 与内容面（P2）

- [ ] 替换 `QqMusicPlaylistList` 占位（`LibraryScreen.kt:3470`，删除 `TODO` 与「开发中」文案）
- [ ] `QQMusicChannel`：榜单 / 推荐歌单（**接口 module/method 需抓包确认，见 §10**）
- [ ] `QQMusicUserApi`：用户歌单 / 收藏（依赖登录）
- [ ] 未登录空态（内嵌登录入口）+ 离线空态
- [ ] 若走详情页导航：新增 `LibrarySelectedItem` 子类时**必须同步 `librarySelectedItemSaver` 的穷尽 when**，否则编译报错 + 进程重启丢导航态（酷狗接入时踩过的坑）
- [ ] **验收：媒体库 QQ音乐 tab 显示内容并可播放**

### 阶段 6：稳定性与回归（P3）

- [ ] 播放取址 QPS 限速
- [ ] vkey 失效自动重解析（复用 `SongUrlResolutionRetry`）
- [ ] 适配层单元测试（酷狗适配层目前零测试，QQ音乐不应重蹈覆辙）
- [ ] QRC 逐字歌词（移植 344 行 TripleDES，可选）
- [ ] 自动切源兜底（**取决于 §7.1 的前置依赖是否先补**）

---

## 9. 风险与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| **匿名覆盖率仅约 25%** | 用户搜到多半播不了，体验差于其他源 | 明确「需会员」文案 + 优先补自动切源；必要时把 QQ音乐定位为「补充源」而非主源 |
| **「不可播」比例高导致口碑问题** | 用户误判为 Bug | 失败原因区分 `104003`（需会员）/ 网络错误；UI 给出可操作指引（登录 / 切源） |
| 登录后高音质解锁**未验证** | 阶段 4 可能白做 | 阶段 4 开工前先做一次手工验证（1 小时成本） |
| **QIMEI 移植成本（路线 A 既定）** | 阶段 4 工作量上升 | 全为标准密码学原语，且 `NeteaseCrypto` 已有 AES/RSA/MD5 可复用；公钥/SECRET 为固定常量，估计 100–150 行 |
| **QIMEI / session 24h 过期** | 登录与高音质取址间歇性失效 | 签发时间落盘；请求前检查时效，过期静默重注册再重试；注册做并发串行化 |
| QR 登录 4 步链路脆弱 | 上游任一环节变更即失效 | 链路封装在 `QQMusicQrLoginClient` 单文件便于热修；各步骤独立可测 |
| `check_sig` 的 cookie 名多形态 | 取 `p_skey` 失败导致登录中断 | 按 QQMusicapi 实现做多候选兼容（`p_skey` / `p-skey` / `pskey` / `ptsigx` / `skey`） |
| `guid` / 设备画像漂移触发风控 | 取址失败率上升 | 安装后生成一次并持久化，跨请求复用；退出登录不重置 |
| vkey 时效约 2 小时 | 长时间播放中断 | 已有重解析机制，验证其在 QQ音乐分支生效 |
| **自动切源能力缺失（前置依赖）** | 无法兜底，风险被放大 | **建议单独评估并先行补齐**（酷狗阶段 5 同样缺口） |
| `SongItem.id` 为 `Long`、songmid 为字符串 | 潜在碰撞 / 标识丢失 | 照搬酷狗范式：`audioId` 存 songmid，`id` 用 `hashCode()` |
| 版权 / 合规 | 法律风险 | 不绕过付费墙；仅取平台下发的免费档位；登录用用户自有账号权限；README 声明 |
| 平台协议变更 | 播放失效 | 取址逻辑集中在 `QQMusicPlayback.kt` 单文件；若变异频繁，可重新评估 §4.2 的 Node 中转方案 |

---

## 10. 决策记录与遗留事项

**已决策（用户 2026-09-12 确认）：**

1. **落地方式**：**内嵌 Kotlin 适配层**，否决 Node 中转服务（存档见 §4.2）。
2. **登录路线**：**QR 扫码（路线 A）**，否决 WebView cookie（存档见 §7.2）。
   - **已知连锁代价**：路线 A ⇒ 必须同时移植 **QIMEI 注册 + getSession**（归入 `QQMusicDevice`，见 §5.4 / §7.2 / 阶段 4a）。此代价已确认接受。

**待决策：**

> 分组说明：**【阻塞开工】** = 不定就没法动阶段 1；**【条件项】** = 需要用户提供条件；**【形态项】** = 影响改动面，阶段 2/5 之前定即可。

**【阻塞开工】**

1. **交付范围**：先交付「阶段 1 + 2」的匿名最小切片（能搜能播 128k）验证手感，还是直接推进到登录 + 高音质？
2. **「不可播」（`result=104003`）的默认交互**（QQ音乐特有，因匿名覆盖率仅约 25%，此为高频路径）：
   - 选项 A：**提示「需 QQ音乐会员」+ 提供手动切源按钮** —— 不依赖前置能力，改动小
   - 选项 B：**静默自动切源**（网易云 / B 站）—— 体验最顺，但**依赖切源能力先落地**，与第 3 项耦合
   - 选项 C：提示 + 自动切源（用户可关）
3. **自动切源是否作为前置依赖先行补齐**：该能力（酷狗→B站/网易云）目前**同样缺失**，是跨平台的基础能力，不只服务 QQ音乐。若选 2-B / 2-C，则必须先做或并行做。

**【条件项】**

4. **登录后高音质解锁验证**（阶段 4 开工前置）：需**真实 QQ音乐账号**做 1 次手工实验，确认 `F000` / `M800` / `C600` / `O800` 是否真正解锁。请确认验证方式：
   - 选项 A：你提供账号，我用 QQMusicapi 跑一次扫码验证（账号凭证仅本地使用，不落盘）
   - 选项 B：你自己在浏览器/客户端确认 VIP 状态与可用音质档位
   - 选项 C：先跳过，阶段 4 开工前再补

**【形态项】**

5. **探索页 QQ音乐搜索源的位置**：`SearchSource` 当前顺序为 `YOUTUBE_MUSIC, NETEASE, BILIBILI, KUGOU, LINK_RECOGNITION`。
   - 建议：插在 `KUGOU` 之后、`LINK_RECOGNITION` 之前（与媒体库 tab 顺序一致）
6. **探索页是否做 QQ音乐默认内容**（未搜索时的发现内容：榜单 / 推荐歌单）：
   - 参照酷狗有 `KugouExploreContent`（每日推荐/排行榜/热门歌单横滑卡片）；QQ音乐是否同等对待？
   - ⚠️ 该功能依赖「榜单 / 推荐接口的 module + method」，属信息缺口（见下），需先抓包确认可行性
7. **媒体库 QQ音乐 tab 的内容范围**：
   - 选项 A：仅个人内容（我的歌单 / 收藏）+ 未登录空态 —— 与项目「媒体库 = 我的内容」分层原则一致
   - 选项 B：A + 榜单/推荐（会与探索页职责重叠）
   - 选项 C：本阶段不做，保持占位
8. **音质偏好默认档位**：匿名封顶 `M500`/`C400`，登录后可上探。默认偏好建议 `M500` 优先（文档 §5.3 已给理由），是否需要在设置页区分「未登录/已登录」两套可选档位？

**由实施方（助手）自行决定的技术细节（如有异议请提出）：**

- `channelId = "qqmusic"`、`album` 标记 `"QQMusic|{songmid}"`、缓存 key `qqmusic-$songmid-$quality`
- **QIMEI 设备画像**：拟采用**固定伪造画像**（对齐 QQMusicapi `device.json` 中的 MI 6 / Android 10 组合），而非读取本机真机信息 —— 理由：① 与本机真实设备解耦，避免画像不一致反而触发风控；② 不把真机信息外发。**涉及隐私取向，如你倾向用真实设备信息请告知。**
- QQ音乐英文名沿用官方 **"QQ Music"**（无需像酷狗那样特殊处理）
- vkey 缓存策略：单曲短时缓存 + 403/失效即重解析

**已确认的事实（本次评估产出）：**

1. 匿名取址**不需要 QIMEI**，走 web 平台（`platform=20` / `ct=24`），与现有 `QQMusicSearchApi` 同源。
2. 匿名音质封顶 `M500`(MP3 128) / `C400`(AAC 96)，直链真实可播（已做文件头校验）。
3. 无免签 CDN 直连捷径（`ws.stream` / `dl.stream` 均 403）。
4. `NeriPlayer` 已有 QQ音乐搜索 / 详情 / 歌词全链路，且 `ManualSearchState.selectedPlatform` 的声明默认值为 `QQ_MUSIC`（网易云 cookie 缺失时的兜底平台）。
5. `QQMusicapi` 本地未安装依赖、未登录过；其 `device.json` 表明匿名 Android 链路曾跑通。
6. 匿名失败码为 `104003`（无权限 / 需 VIP），是最高频的失败原因。

**信息缺口（实施时需补齐）：**

1. **榜单 / 推荐歌单接口的 module + method**（QQMusicapi 未完整覆盖，`song.js` 仅有 `getSimilar` / `getRelatedSonglist`）。
2. **用户歌单 / 收藏接口的 module + method**（QQMusicapi 有 web 路由，但底层 module 需确认）。
3. **登录后高音质解锁的实际效果**（需真实账号，阶段 4 开工前置）。
4. **`sip` 数组的稳定性**：实测返回 `http://`（非 https）CDN，需确认 Android 端 `cleartextTraffic` 策略是否放行（项目已支持其他源 http 直链，但要单独确认 QQ音乐域名）。
5. **QQ音乐云盘**是否存在可用接口（QQMusicapi 未覆盖）。
6. **QIMEI payload 的 `reserved` 字段是否需要跟随设备真实信息**（QQMusicapi 中 `brand` / `model` / `procVersion` 等取自设备文件；本项目是否需要伪造一致画像，影响风控通过率）。

---

## 附：参考资料

- **主蓝本**：[QQMusicapi](file:///D:/work/QQMusicapi)（本地 `D:\work\QQMusicapi`，Node.js ESM + Koa，约 3000 行核心代码）
  - 取址：`src/modules/song.js`（`getUrls` / `getPlayUrls`）
  - 签名：`src/algorithms/sign.js`（`zzcSign`）、`src/algorithms/tripledes.js`、`src/algorithms/qrc.js`
  - 登录：`src/modules/login.js`（`_getQQQr` / `checkQrcode` / `_authorizeQQQr`）
  - 设备指纹：`src/utils/qimei.js`、`src/utils/device.js`
  - 平台策略：`src/versioning.js`（ANDROID / DESKTOP / WEB 三套 comm 参数）
- **上游原版**：[L-1124/QQMusicApi](https://github.com/L-1124/QQMusicApi)（Python）
- **项目内参照实现**：
  - [KugouPlayback.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/kugou/KugouPlayback.kt)（播放适配层范式）
  - [KugouModels.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/kugou/KugouModels.kt)（字符串 id 承载范式）
  - [QQMusicSearchApi.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/search/QQMusicSearchApi.kt)（已存在的 QQ音乐匿名链路）
  - [NeteaseQrLoginClient.kt](../../app/src/main/java/moe/ouom/neriplayer/core/api/netease/NeteaseQrLoginClient.kt)（登录范式）
  - [接入酷狗概念版-实施方案.md](./接入酷狗概念版-实施方案.md)（阶段划分与踩坑记录可直接借鉴）
