# 把 KuGou / QQMusic 歌词接入上游 `LyricSourcePreference` 体系

状态：设计待评审
适用基线：`upstream/master @ 2942511`（2026-10-22），即地基同步（方案 B 第 1 步）合入之后的目录结构
作者：待评审确认

---

## 1. 问题定义

双方各自实现了一套"这首歌的歌词从哪来"，机制互不兼容：

| | 上游 | fork |
|---|---|---|
| 入口 | `LyricSourcePreference`（用户可配置） | `song.channelId` 硬分支 |
| 取词方式 | `EditableLyricsMatcher` 关键词扇出 + 打分排名 | 平台原生 ID 直取（KuGou hash / QQ songmid） |
| 位置 | `modules/platform/.../lyrics/repository/EditableLyricsMatcher.kt` | [PlayerLyricsProvider.kt:873](file:///D:/work/NeriPlayer/app/src/main/java/moe/ouom/neriplayer/core/player/metadata/PlayerLyricsProvider.kt#L873)、[:1061](file:///D:/work/NeriPlayer/app/src/main/java/moe/ouom/neriplayer/core/player/metadata/PlayerLyricsProvider.kt#L1061) |

fork 的分支在上游体系**外面**，因此每加一个源就要再改一次 `PlayerLyricsProvider`；而 fork 携带的东西上游**表达不了**：精确身份。目标是保留身份优势、删除旁路。

### fork 分支现在实际造成的 3 个问题

1. **用户手工编辑的歌词会被覆盖。** `shouldTryPreferredLyricSource`（`PlayerLyricsProvider.kt:222`）有 `song.lyricSyncEdited == true` 守卫；fork 的 `PlayerLyricsProvider.kt` 里 `lyricSyncEdited` 出现 **0 次**，上游同一文件出现 **3 次**。`return@withContext loadKugouTranslatedLyrics(song)` 在任何编辑检查之前就返回了。
2. **per-source 默认偏移失效。** 上游 `modules/lyrics/.../offset/LyricDefaultOffset.kt` 已支持 `kugouDefaultOffsetMs` / `qqMusicDefaultOffsetMs`（#436d8303），只有走标准链路才会应用。
3. **翻译/音译分层被跳过。** 上游的 `romanizedLyrics`、`translationLookupComplete`、`EditableLyricFormat` 判定都在标准结果结构里，fork 直接返回 `List<LyricEntry>`，拿不到这些元数据。

---

## 2. 决定方案的 6 个事实（均已核对代码）

**F1 — QQMusic 侧 upstream 已经有身份直取，fork 的实现是重复的。**
`modules/platform/.../platform/search/api/client/QQMusicSearchApi.kt:158`：

```kotlin
suspend fun getNativeLyrics(songMid: String): Pair<String?, String?>
```

fork 的 `loadQQMusicTranslatedLyrics` + `QQMusicLyricsCacheEntry` + `qqMusicLyricsCache` + `isQQMusicLyricTarget` + `QQ_MUSIC_ALBUM_TAG`（commit `a1cfa0f5`）**全部删除**，不需要等价替换。

**F2 — KuGou 侧上游 client 已经吃 hash，缺的只是入口。**
`KugouLyricsClient.kt` 只有 4 个公开方法：`searchSongs(keyword)`:20、`searchLyricCandidates(song)`:41、`downloadKrcLyric`:54、`downloadLrcLyric`:67。`KugouSongSearchResult` 本身带 `hash` 字段。所以"跳过 `searchSongs`、直接用 hash 要 KRC"在 client 层已经成立，只是 `EditableLyricsMatcher.searchKugou()`（:188）无论如何都先跑 `searchSongs(query)`。

**F3 — `EditableLyricMatchRequest` 没有身份字段。**
```kotlin
keyword, trackName, artistName, albumName, durationMs, preferWordTimed, sources
```
这是本次唯一需要**新增**的抽象。

**F4 — `matchedLyricSource` 以字符串持久化，加枚举值零迁移成本。**
`modules/database/.../entity/PlaybackQueueEntities.kt:60` 与 `DownloadSnapshotEntities.kt:93` 均为 `val matchedLyricSource: String?`。上游 `MusicPlatform` 只有 `CLOUD_MUSIC, QQ_MUSIC`；fork 在 [SongData.kt:28-30](file:///D:/work/NeriPlayer/app/src/main/java/moe/ouom/neriplayer/core/api/search/SongData.kt#L28) 加了 `KUGOU`。**保留 fork 的这版**，不碰 schema。

**F5 — 两个词表并存，必须只在一个地方桥接。**
`MusicPlatform {CLOUD_MUSIC, QQ_MUSIC, KUGOU}`（播放来源）与 `EditableLyricMatchSource {KUGOU, CLOUD_MUSIC, QQ_MUSIC, AMLL_TTML, LRCLIB, YOUTUBE_MUSIC}`（歌词来源）语义不同。已有先例：`LyricSourcePreference.matchSource` 就是这张映射表。不要再引入第二处 `when (platform)`。

**F6 — 所有 KuGou `SongItem` 恒带 hash，身份入口无需为非 KuGou 数据兜底。**
[KugouChannel.kt:171-178](file:///D:/work/NeriPlayer/app/src/main/java/moe/ouom/neriplayer/core/api/kugou/KugouChannel.kt#L171) 的 `parseKugouAudioEntry` 在 `hash` / `hash_128` / `hash_320` / `FileHash` 全部缺失时直接 `return null`——条目根本不入库。频道、歌单、Personal FM 都走这个解析器；云盘走 `buildCloudSongItem`，同样 `audioId = hash`（`KugouUserApi.kt:165`）。QQMusic 侧 `audioId = songMid`（`QQMusicModels.kt:44`）。因此"KuGou 频道歌曲一定可用身份直取"是成立的不变量。

---

## 3. 目标 / 非目标

**目标**
- KuGou / QQMusic 频道歌曲走 `EditableLyricsMatcher` 单一链路，`PlayerLyricsProvider` 内不出现平台分支。
- 已知原生 ID 时**不发关键词搜索请求**（省 1 次 RTT，且避免匹配到同名不同曲）。
- 用户设置 `LyricSourcePreference = Kugou` 时确实锁定 KuGou；`= Automatic` 时频道身份只做**排序加权**，不做独占。
- 净删代码。

**非目标**
- 不改 KuGou / QQMusic 的播放、登录、音质链路（那是另一个 PR）。
- 不给 `EditableLyricsMatcher` 加新源。
- 不动批量补全（`LocalCurationCoordinator`）的用户交互，只是它自动获得身份短路能力。

---

## 4. 方案

### 4.1 数据流

```
SongItem ──▶ lyricIdentityOf(song) ──▶ EditableLyricIdentity?  (hash / songmid / neteaseId)
                                    └─▶ lyricSourceHintOf(song) ─▶ EditableLyricMatchSource?

EditableLyricMatchRequest(track, artist, duration, identity, sources)
        │
        ▼
EditableLyricsMatcher
        ├─ identity != null && identity.source in sources  ──▶ 身份直取（无搜索）
        └─ 否则                                             ──▶ 既有 searchXxx 关键词路径
        ▼
rankEditableLyricMatches(sourceRank = { s -> base(s) - if (s == hint) HINT_BONUS else 0 })
        ▼
PreferredLyricSourceResult（沿用上游结构，含 translated/romanized/format/confidence）
```

关键取舍：**身份命中不豁免校验**。仍然过 `sanitizeEditableLyricMatchCandidate` 和塌缩时间轴检查，只是跳过标题/歌手/时长的相似度打分。理由：fork 的 `audioId` 来自上一版频道数据，存在 hash 过期/串号的可能，静默信任会把错歌词钉死。

### 4.2 新增：`EditableLyricIdentity`（`:model`）

`modules/model/src/main/java/moe/ouom/neriplayer/data/model/lyrics/matching/EditableLyricIdentity.kt`

```kotlin
package moe.ouom.neriplayer.data.model.lyrics.matching

/** 歌曲在来源平台上的原生标识，存在时可直接取词、跳过关键词检索。 */
sealed interface EditableLyricIdentity {
    val source: EditableLyricMatchSource
    val isUsable: Boolean

    data class Kugou(val audioHash: String, val songId: String?) : EditableLyricIdentity {
        override val source get() = EditableLyricMatchSource.KUGOU
        override val isUsable get() = audioHash.isNotBlank()
    }

    data class QqMusic(val songMid: String) : EditableLyricIdentity {
        override val source get() = EditableLyricMatchSource.QQ_MUSIC
        override val isUsable get() = songMid.isNotBlank()
    }

    data class CloudMusic(val songId: Long) : EditableLyricIdentity {
        override val source get() = EditableLyricMatchSource.CLOUD_MUSIC
        override val isUsable get() = songId > 0L
    }
}
```

`EditableLyricMatchRequest` 增一个默认参数，全部现有调用点不用改：

```kotlin
data class EditableLyricMatchRequest(
    ...,
    val sources: Set<EditableLyricMatchSource> = DEFAULT_EDITABLE_LYRIC_MATCH_SOURCES.toSet(),
    val identity: EditableLyricIdentity? = null
)
```

`EditableLyricMatchCandidate` 增 `val exactIdentity: Boolean = false`，供排名器识别。

用 sealed 而不是 `Map<String, String>`：`domain-dependencies.py` 的 `allowed_classes` 是按类名白名单校验的，松散 map 会需要 `*` 通配、并让 `EditableLyricsMatcher` 的分支复杂度直接进 CRAP 统计。

### 4.3 修改：`EditableLyricsMatcher` 身份短路（`:platform`）

`searchKugou` 开头插入（`searchQqMusic` 同构，走 `getNativeLyrics`）：

```kotlin
private suspend fun searchKugou(request: EditableLyricMatchRequest): List<EditableLyricMatchCandidate> =
    runSourceSearch("kugou") {
        kugouIdentityCandidate(request) ?: keywordKugouCandidates(request)
    }
```

`kugouIdentityCandidate` / `keywordKugouCandidates` 各自是独立私有函数——**不要**写成一个大 `when`，原因见 §6。

`QQMusic` 侧：`searchSearchApi` 里已有 `if (source == EditableLyricMatchSource.QQ_MUSIC && api is NativeLyricSearchApi)` 分支（`EditableLyricsMatcher.kt:252`），身份路径就挂在它旁边，直接 `qqMusicSearchApi.getNativeLyrics(songMid)`，产出 `exactIdentity = true` 的候选。

**两个必须处理的既有 guard，否则身份路径静默失效：**

1. **空 `keyword` 早退在源分发之前。** `matchLyrics`（`EditableLyricsMatcher.kt:72-74`）和 `matchHighConfidenceLyricsForSource`（:153-155）都在调用 `searchSource` **之前** `if (keyword.isBlank()) return emptyList()`。歌名/歌手为空但有 hash 的条目（fork 解析器允许 `rawName` 兜底成空串）会整条链路拿不到歌词。不是常见路径，但命中即彻底无词。
   → 两个入口的 guard 改为 `if (keyword.isBlank() && request.identity?.isUsable != true)`。只放宽这一处条件，不动其它校验。
2. **`runSourceSearch` 吞异常。** 它 catch 所有非取消异常并返回 `emptyList()`（:408-412）。若 `getPayloadByHash` 抛错，外层捕获后直接返回空，`?: keywordKugouCandidates(request)` **永远不会执行**。
   → 身份查询函数内部自带 try/catch 并返回 `null`，只向 `runSourceSearch` 暴露关键词路径的异常。

`KugouLyricsRepository` 需要新增一个入口（它当前只暴露 `searchSongs` / `getBestLyrics` / `getBestLyricPayload`）：

```kotlin
suspend fun getPayloadByHash(audioHash: String, songId: String?, title: String,
                             artist: String, album: String?): KugouLyricsPayload? =
    kugouLyricsClient.getBestLyricPayload(
        KugouSongSearchResult(id = songId.orEmpty(), hash = audioHash,
                              title = title, artist = artist, album = album)
    )
```

保持一行委托。这个文件在 CRAP scope 内（`config/quality/crap-scope.json:272`），失败阈值 CRAP > 9，**任何分支逻辑都不要写在这里**。

### 4.4 修改：排名器接受 hint

`rankEditableLyricMatches` 已经接收 `sourceRank: (EditableLyricMatchSource) -> Int`（调用点 `EditableLyricsMatcher.kt:136`、`EditableLyricMatchPolicy.kt:121`，两处都传 `::editableLyricMatchSourcePriority`）。因此：

```kotlin
// 纯函数，不动 editableLyricMatchSourcePriority 本体（它被 ScoringTest 断言了 6 个源的相对顺序）
fun sourceRankWithHint(source: EditableLyricMatchSource, hint: EditableLyricMatchSource?): Int =
    editableLyricMatchSourcePriority(source) - if (source == hint) HINT_RANK_BONUS else 0
```

`exactIdentity == true` 的候选直接判 `Confidence.HIGH`、`durationDeltaMs = null`，不参与 token 打分。`matchIdentityKey()`（:170）已经用 `source:title:artist:hashcode` 去重，身份候选天然兼容，不用改。

### 4.5 修改：`PlayerLyricsProvider` 删除旁路（`:playback:runtime`）

删：
- 873-877 两个 `return@withContext` 早退（KuGou + QQ）
- 1061 的 `when` 平台分支
- `loadKugouPlatformLyrics` / `loadKugouTranslatedLyrics` / `loadKugouLyricsPayload`
- `isQQMusicLyricTarget` / `loadQQMusicPlatformLyrics` / `loadQQMusicTranslatedLyrics` / `QQMusicLyricsCacheEntry` / `qqMusicLyricsCache` / `QQ_MUSIC_ALBUM_TAG`
- fork 侧 `LyricDefaultOffset.kt` 那 1 行 `MusicPlatform.KUGOU -> ...`（上游 #436d8303 已覆盖，直接取上游版本）

增：`lyricIdentityOf(song)` / `lyricSourceHintOf(song)` 两个纯映射函数。身份优先级沿用 fork 已验证过的规则（`a1cfa0f5` 注释里写明的）：

```kotlin
// KuGou
EditableLyricIdentity.Kugou(
    audioHash = song.audioId.orEmpty(),
    songId = song.matchedSongId.takeIf { song.matchedLyricSource == MusicPlatform.KUGOU }
        ?: song.subAudioId
)
// QQMusic
EditableLyricIdentity.QqMusic(
    songMid = if (song.matchedLyricSource == MusicPlatform.QQ_MUSIC) song.matchedSongId.orEmpty()
              else song.audioId.orEmpty()
)
```

放在 `:platform` 而非 `:model`：`SongItem` 在 `moe.ouom.neriplayer.data.model`，而 `platform-lyrics-provider` 规则已经白名单化了 `data.model.lyrics.matching.*`，反向依赖（model 引 SongItem 的其他域）会撞门禁。

### 4.6 拆除 `album` 打包身份

fork 现在把身份塞进用户可见字段：

```kotlin
album = listOfNotNull(PlayerManager.KUGOU_SOURCE_TAG, hash, albumAudioIdPart).joinToString("|")
```

`SongItem` 已有 `channelId` / `audioId` / `subAudioId`（上游 `SongItem.kt:33-35`），不该再用 `album` 承载 ID——它会显示在 UI，而且会污染上游 `tokenOverlapRatio` 的专辑比对。

迁移策略：**读旧写新**。`lyricIdentityOf` 保留一个 `parseLegacyAlbumTag(song.album)` 回退分支，覆盖已入库和已同步到 WebDAV 的旧行；新写入一律不再打包。这个回退分支标注 `// TODO(2027-01) 旧数据不再有存量后删除`。不做 DB 迁移——它只影响一次性解析，代价低于一次 schema bump + ZSTD 存档迁移（#475）叠在一起的回归面。

---

## 5. 决策表

| `LyricSourcePreference` | 有身份 | 行为 |
|---|---|---|
| `Kugou` | 有 | 身份直取；失败才回退到 `sources = {KUGOU}` 关键词搜索 |
| `Kugou` | 无 | `matchHighConfidenceLyricsForSource(request, KUGOU)` |
| `Automatic` | 有 | 全源扇出，但身份源候选 `exactIdentity` 置顶（hint = 频道源） |
| `Automatic` | 无 | 上游当前行为，不变 |
| 任意 | 任意 | `lyricSyncEdited == true` → 一律先返回用户编辑内容（**fork 现状缺失，本设计补回**） |

---

## 6. 门禁清单（漏了会直接红 CI）

这是本设计里最容易踩的部分，上游的质量门禁是**按类名白名单**校验的：

- [ ] `config/quality/domain-dependencies.json` 的 `platform-lyrics-provider` 规则（:3087）`allowed_classes` 逐条列出 `data.model.lyrics.matching.*`（:3197-3204）。新增 `EditableLyricIdentity` 及其 3 个嵌套类**必须同步登记**，否则 `domain_dependencies.py` 报 `forbidden package dependency`。
- [ ] `config/quality/crap-scope.json` 已包含 `KugouLyricsRepository.kt`（:272）、`QQMusicLyricsRepository.kt`（:274）、`QQMusicLyricSelection.kt`（:275）、`data/settings/lyrics/**`（:298）、`lyrics/offset/**`（:326）。CRAP > 9 直接 fail（`crap_report.py:12`）。→ 分支逻辑只写在 `EditableLyricsMatcher`（不在 scope 内）和 `PlayerLyricsProvider`（不在 scope 内），repository 保持单行委托。
- [ ] `tools_pub/quality/module_boundaries.py:27` `MAX_DIRECTORY_SOURCES = 16`。`modules/model/.../lyrics/matching/` 当前文件数需在改动前复查。
- [ ] `settings/page/SettingsSearchIndex.kt` 和 `AutoSettingsSchema` / `ConfigSettingsSanitizer` 对设置项有元数据测试。本设计**不新增设置项**（`LyricSourcePreference` 六个值已存在），若过程中发现要加，这三个文件 + i18n 三语必须一起改。

---

## 7. 落地拆分

| PR | 内容 | 依赖 | 规模 |
|---|---|---|---|
| PR-1 | `EditableLyricIdentity` + request/candidate 字段 + 门禁登记 + 单测（纯 model 层，行为不变） | 地基 merge 完成 | 小，可独立验证编译 |
| PR-2 | matcher 身份短路 + hint 排名 + `KugouLyricsRepository` 委托 + `:platform` 单测 | PR-1 | 中 |
| PR-3 | `PlayerLyricsProvider` 删旁路、接身份/hint、拆 album 打包 + 行为回归测试 | PR-2 | **净删代码**，风险最高，需真机验证 |
| PR-4 | `MusicPlatform.KUGOU` 收敛（含 fork 的 `SongData.kt` modify/delete 冲突处置）+ QQMusic 播放/搜索双实现去重 | PR-3 | 独立评审 |

PR-1 和 PR-2 合完就可以先跑一次 CI，确认门禁全绿再动 PR-3——PR-3 是唯一改运行时行为的。

---

## 8. 测试计划

**单元**
- `EditableLyricIdentityTest`：三类身份的 `isUsable` 边界（空 hash、`songId <= 0`、`matchedSongId` 与 `subAudioId` 优先级）。
- `EditableLyricsMatcherIdentityTest`：给身份 → 断言 `KugouLyricsClient.searchSongs` **零调用**（用 fake client 计数，这条是省 RTT 的验收线）；身份取词返回 null → 断言回退到关键词路径。
- `EditableLyricMatchScoringTest` 扩展：hint 只改顺序不改相对优先级；`exactIdentity` 判 HIGH 且 `durationDeltaMs == null`；token 完全不匹配的假身份**不能**胜出（防串号）。
- `LyricIdentityOfSongTest`：`channelId = "kugou"` + `album = "KUGOU|hash|id"` 旧格式仍能解析出身份。
- 两条 guard 的特性测试（对应 §4.3，PR-2 先写先红）：`keyword` 为空但身份可用 → `matchLyrics` 与 `matchHighConfidenceLyricsForSource` 仍返回候选；身份查询内部抛异常 → 回退到关键词路径且不为空。

**行为回归（PR-3 必做）**
- KuGou 频道歌，播放 → 歌词出、翻译出、per-source 偏移生效。
- 用户在歌词编辑器手工改过 → 重新播放不被覆盖。**这条在 fork 当前版本是失败的**，作为特性测试先写、先红。
- `LyricSourcePreference = CloudMusic` + KuGou 频道歌 → 尊重用户选择（fork 当前忽略）。
- QQMusic 频道歌全链路同上。
- WebDAV 同步往返：`matchedLyricSource = "KUGOU"` 写入→下载→读取不丢失（注意上游 #475 ZSTD 存档迁移在同一个 PR 窗口内，两项都动过的字段要在真机上验一次）。

---

## 9. 风险与回滚

| 风险 | 面 | 缓解 |
|---|---|---|
| 身份直取绕过相似度，遇到过期 hash → 错歌词 | 中 | §4.1 保留 sanitize + 塌缩时间轴校验；假身份胜出有单测；PR-3 前可在 `NPLogger` 打一行 `identity_hit` 观测命中率 |
| 空 `keyword` guard 与 `runSourceSearch` 吞异常导致身份路径失效 | 高 | 已在 §4.3 定为硬性要求，PR-2 必须先写这两条测试再写实现 |
| 门禁白名单漏登记 | 高（CI 立刻红） | §6 checklist，PR-1 单独跑 CI |
| `album` 打包身份拆除影响存量数据 | 低 | 读旧写新，不迁移 |

回滚：PR-3 是唯一行为变更，单文件 revert 即可回到 fork 现状（其余 PR 是纯增量、默认参数，不改变既有调用结果）。

---

## 11. 实现时的三处偏差（已按实况修正，实现优先于此文）

实现 PR-1/PR-2/PR-3 时发现本文有三处比实际情况保守，已按更简单的做法落地：

**1. 不需要给 `KugouLyricsRepository` 加 `getPayloadByHash`（§4.3、§6 门禁项）。**
它现有的 `getBestLyricPayload(song: KugouSongSearchResult)` 本来就吃 hash，直接构造
`KugouSongSearchResult` 即可。因此那个在 CRAP scope 内的文件一行没动，§6 关于
"repository 保持单行委托"的约束自动满足。QQ 侧同理，上游已有的
`NativeLyricSearchApi.getNativeSongInfo(id)` 就是直取入口。**本次零新增平台 API。**

**2. 不需要 `sourceRankWithHint`（§4.4）。**
`editableLyricMatchResultComparator` 的排序键是
`wordTiming → confidence → score → durationDelta → sourceRank`。身份候选拿到的是
`Confidence.HIGH` + 固定 `1000` 分，在 confidence 与 score 两级上已经压过关键词候选，
`sourceRank` 根本轮不到参与竞争。再引入 hint 函数去改两个调用点的 `sourceRank`
参数是多余的间接层，还会动到被 `EditableLyricMatchScoringTest` 断言相对顺序的
`editableLyricMatchSourcePriority`。§8 里"hint 只改顺序不改相对优先级"这条测试
因此作废。

**3. 不需要解析 `album` 里打包的身份（§4.6）。**
取词只需要 `SongItem.audioId`：酷狗条目所有构造路径（频道 / 歌单 / Personal FM / 云盘）
都写入 `audioId = hash`，且 `parseKugouAudioEntry` 在拿不到 hash 时直接 `return null`，
条目根本不入库；QQ 写 `audioId = songmid`。`album` 里那份 `Kugou|<hash>|<id>` 是
播放分支标记，对歌词链路是冗余信息。§4.6 提的"读旧写新"迁移因而不涉及取词正确性，
仍然值得单独做（它污染 `tokenOverlapRatio` 的专辑比对，且会显示在 UI），但不属于本设计范围。

相应地 §7 的 PR 拆分变成：PR-1 与 PR-2 合为一次 `:model` + `:platform` 改动，
PR-3 只剩派生函数与接线，PR-4 不变。

## 10. 未决问题

1. **上游 `QQMusicSearchApi` 用的是匿名接口还是带凭据？** fork 侧有 `QQMusicVkeyRateLimiter`、`QQMusicCredentialRefresh`、QIMEI 设备号。若上游 API 无鉴权，直取 `getNativeLyrics` 可能对 VIP 歌曲拿空结果，则 PR-4 要把 fork 的带凭据客户端接进 `NativeLyricSearchApi`，而不是复用上游的。
2. **AMLL / LrcLib 是否也该有身份类型？** `EditableLyricMatchSource` 六个源里还有 3 个没有身份入口。本设计只做 KuGou/QQMusic，但 sealed 结构要预留——若确定不做，就在 `EditableLyricIdentity` 的 KDoc 里写明"仅覆盖已知平台 ID 的三个源"，避免后来者往 sealed 里塞 `Unknown`。
