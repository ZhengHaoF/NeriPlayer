# KuGou / QQ Music 集成补丁存档

这些是 fork（基线 `a6b30c58`）对上游共享文件的改动，用
`git diff ac7bdea46 a6b30c586 -- <path>` 导出。

上游在 `2942511`（2026-10-22）做了多模块化重构，这些文件大多被移动或拆分，
补丁不能直接套用：能干净落下的 hunk 已合入分支，落不下的那些已把冲突解为
**保留上游版本**，并把 fork 侧代码原样记在 `../fork-integrations-pending.md`。

## 每个补丁的当前处置

| 补丁 | 处置 |
|---|---|
| `PlayerManager.patch` | 部分手工移植（`kugouSearchApi` 走 `PlayerRepositoryDependencies`；音质偏好走 `PlaybackQualityOwner`）；`isKugouTrack` / 缓存键分支待重贴 |
| `PlayerManagerLifecycleExtensions.patch` | 已手工移植（初始化 + 两个设置收集器） |
| `PlayerManagerPersistenceExtensions.patch` | 已手工移植（搜索 API 分派加 `KUGOU`） |
| `AppContainer.patch` | 已手工移植（session / searchApi / CookieStore 接线） |
| `SongMetadataSearchContent.patch` | 已手工移植（`musicPlatformLabel` 加 `KUGOU`） |
| `ExploreViewModel.patch` | 已重贴：逐行核对时表面上缺 14 行，实际都以适配形式存在（`PlayerManager.kugouSession` → `AppContainer.kugouSession`、`R.string` → `CoreCommonR.string`、`loadKugouChannelContent` 改为扩展函数导入）。残留的 `client.search` 报错来自缺失的酷狗 SDK 构件，不是接线缺口 |
| `NeriApp.patch` | **不重贴**。音质档位已改走上游的 route-state（`AppSettingsRouteState.defaultAudioQuality.kugouPreferredQuality` / `qqMusicPreferredQuality`），补丁里在 `NeriAppContent` 局部 collect 两个 flow 再传参的两处 hunk 已被该结构取代 |
| `NowPlayingViewModel.patch` | 已手工移植 |
| `SettingsScreen.patch` | **已完成**：账号卡片走上游的 `SettingsAccountPlatform` + `SettingsAccountAuthController`；音质档位走上游的 route-state / bindings；首页排序与标签排序入口走 `SettingsPersonalizationHomeCard` |
| `LibraryScreen.patch` | 已重贴（`LibraryTab.KUGOU` + 排序设置接回上游文件位置 `tab/library/`；上游的 QQ 占位 `QqMusicPlaylistList` 换成 fork 的 `KugouLibraryContent` / `QQMusicLibraryContent`） |
| `LibraryHostScreen.patch` | 已重贴；KuGou/QQ 子类型改用上游的 `saveState()` + restorer 注册表写法，弃用 fork 的集中式 `when` |
| `SettingsHostScreen.patch` | 已重贴（`HomeSettings` / `TabOrderSettings` 两个子页接进上游的 `selectSettingsHostPage`） |
| `StartupOnboardingScreen.patch` | 已重贴（QQ / 酷狗扫码登录引导步骤 + 未登录告警） |
| `HomeViewModel.patch` | 已重贴（多源首页 + 酷狗兜底 + `HomeScrollKeyKugouHome`；补丁 115 条新增行逐行核对全部落地），但**尚未编译通过**：`kugouSession` 取值点和一处 `context` 参数仍指向重构前的 API |
| `PlayerLyricsProvider.patch` | **不重贴**。按设计文档改用上游的 `LyricSourcePreference` + 身份直取链路，见 `docs/歌词源体系接入KuGou与QQMusic设计.md` |

需要重新生成时用：

```bash
git diff ac7bdea46 a6b30c586 -- <文件路径> > 这个目录/<名字>.patch
```
