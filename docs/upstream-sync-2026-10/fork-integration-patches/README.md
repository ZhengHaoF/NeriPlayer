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
| `ExploreViewModel.patch` | 大部分 hunk 已合入；余下待修 |
| `NeriApp.patch` | 部分 hunk 已合入 |
| `NowPlayingViewModel.patch` | 已手工移植 |
| `SettingsScreen.patch` | **整体回退到上游**，待按上游新的分页结构重贴 |
| `LibraryScreen.patch` | **整体回退到上游**，待重贴（含 `LibraryTab.KUGOU` 枚举项） |
| `LibraryHostScreen.patch` | **整体回退到上游**，待重贴 |
| `SettingsHostScreen.patch` | **整体回退到上游**，待重贴 |
| `StartupOnboardingScreen.patch` | **整体回退到上游**，待重贴（QQ / 酷狗扫码登录引导步骤） |
| `HomeViewModel.patch` | **整体回退到上游**，待重贴（多源首页 + 酷狗兜底） |
| `PlayerLyricsProvider.patch` | **不重贴**。按设计文档改用上游的 `LyricSourcePreference` + 身份直取链路，见 `docs/歌词源体系接入KuGou与QQMusic设计.md` |

需要重新生成时用：

```bash
git diff ac7bdea46 a6b30c586 -- <文件路径> > 这个目录/<名字>.patch
```
