# Upstream sync 2026-10-22: fork integrations still to re-port

Each entry is a merge conflict that was resolved to **upstream's** version because
expressing the fork's change against upstream's restructured code needs a deliberate

edit rather than a textual merge. The fork-side code is reproduced here verbatim so
nothing is lost. Base commit for the fork diff: `a6b30c58`.

Sites pending re-port: 17（媒体库 4 处与启动引导 4 处已完成）

## `app/src/main/java/moe/ouom/neriplayer/ui/NeriApp.kt`
### NeriApp.kt conflict 1

```kotlin
    var startupGlassGateReleased by rememberSaveable {
        mutableStateOf(!advancedGlassController.isBaseBlurEnabled)
    }
    val startupBackgroundGlassReady = backgroundGlassBackdrop.hasActiveBlur
    val startupContentGlassReady = contentGlassBackdrop.hasActiveBlur
    LaunchedEffect(
        advancedGlassController.isBaseBlurEnabled,
        startupBackgroundGlassReady,
        startupContentGlassReady
    ) {
        if (
            shouldReleaseStartupGlassGate(
                baseBlurEnabled = advancedGlassController.isBaseBlurEnabled,
                backgroundEffectReady = startupBackgroundGlassReady,
                contentEffectReady = startupContentGlassReady
            )
        ) {
            startupGlassGateReleased = true
        }
    }
    val preferredQuality by repo.audioQualityFlow.collectAsStateWithLifecycle(initialValue = "exhigh")
    val youtubePreferredQuality by repo.youtubeAudioQualityFlow.collectAsStateWithLifecycle(initialValue = "high")
    val biliPreferredQuality by repo.biliAudioQualityFlow.collectAsStateWithLifecycle(initialValue = "high")
    val kugouPreferredQuality by repo.kugouAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.kugouAudioQuality
    )
    val qqMusicPreferredQuality by repo.qqMusicAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.qqMusicAudioQuality
    )
    val mobileDataFollowDefaultAudioQuality by repo.mobileDataFollowDefaultAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.mobileDataFollowDefaultAudioQuality
    )
    val mobileDataNeteaseAudioQuality by repo.mobileDataNeteaseAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.mobileDataNeteaseAudioQuality
    )
    val mobileDataYouTubeAudioQuality by repo.mobileDataYouTubeAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.mobileDataYouTubeAudioQuality
    )
    val mobileDataBiliAudioQuality by repo.mobileDataBiliAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.mobileDataBiliAudioQuality
    )
```

### NeriApp.kt conflict 2

```kotlin

                    Destinations.Settings.route -> SettingsHostScreen(
                        dynamicColor = dynamicColorEnabled,
                        onDynamicColorChange = { scope.launch { repo.setDynamicColor(it) } },
                        isDarkTheme = isDark,
                        themeMode = themeMode,
                        onThemeToggleRequest = ::requestThemeToggle,
                        onThemeModeRequest = ::requestThemeModeChange,
                        preferredQuality = preferredQuality,
                        onQualityChange = { scope.launch { repo.setAudioQuality(it) } },
                        youtubePreferredQuality = youtubePreferredQuality,
                        onYouTubeQualityChange = {
                            scope.launch { repo.setYouTubeAudioQuality(it) }
                        },
                        biliPreferredQuality = biliPreferredQuality,
                        onBiliQualityChange = { scope.launch { repo.setBiliAudioQuality(it) } },
                        kugouPreferredQuality = kugouPreferredQuality,
                        onKugouQualityChange = { scope.launch { repo.setKugouAudioQuality(it) } },
                        qqMusicPreferredQuality = qqMusicPreferredQuality,
                        onQqMusicQualityChange = {
                            scope.launch { repo.setQqMusicAudioQuality(it) }
                        },
                        mobileDataFollowDefaultAudioQuality =
                            mobileDataFollowDefaultAudioQuality,
                        onMobileDataFollowDefaultAudioQualityChange = { enabled ->
                            scope.launch {
                                repo.setMobileDataFollowDefaultAudioQuality(enabled)
                            }
                        },
                        mobileDataNeteaseAudioQuality = mobileDataNeteaseAudioQuality,
                        onMobileDataNeteaseAudioQualityChange = { quality ->
                            scope.launch {
                                repo.setMobileDataNeteaseAudioQuality(quality)
                            }
                        },
                        mobileDataYouTubeAudioQuality = mobileDataYouTubeAudioQuality,
                        onMobileDataYouTubeAudioQualityChange = { quality ->
                            scope.launch {
                                repo.setMobileDataYouTubeAudioQuality(quality)
                            }
                        },
                        mobileDataBiliAudioQuality = mobileDataBiliAudioQuality,
                        onMobileDataBiliAudioQualityChange = { quality ->
                            scope.launch {
                                repo.setMobileDataBiliAudioQuality(quality)
                            }
                        },
                        seedColorHex = themeSeedColor,
                        onSeedColorChange = { hex ->
                            scope.launch { repo.setThemeSeedColor(hex) }
                        },
                        themeColorPalette = themeColorPalette,
                        onAddColorToPalette = { hex ->
                            scope.launch { repo.addThemePaletteColor(hex) }
                        },
                        onRemoveColorFromPalette = { hex ->
                            scope.launch { repo.removeThemePaletteColor(hex) }
                        },
                        themePaletteStyle = themePaletteStyleValue,
                        onThemePaletteStyleChange = { style ->
                            scope.launch { repo.setThemePaletteStyle(style) }
                        },
                        themeColorSpec = themeColorSpecValue,
                        onThemeColorSpecChange = { spec ->
                            scope.launch { repo.setThemeColorSpec(spec) }
                        },
                        devModeEnabled = devModeEnabled,
                        onDevModeChange = { enabled ->
                            scope.launch { repo.setDevModeEnabled(enabled) }
                        },
                        lyricBlurEnabled = lyricBlurEnabled,
                        onLyricBlurEnabledChange = { enabled ->
                            scope.launch { repo.setLyricBlurEnabled(enabled) }
                        },
                        lyricBlurAmount = lyricBlurAmount,
                        onLyricBlurAmountChange = { amount ->
                            scope.launch { repo.setLyricBlurAmount(amount) }
                        },
                        cloudMusicLyricDefaultOffsetMs = cloudMusicLyricDefaultOffsetMs,
                        onCloudMusicLyricDefaultOffsetMsChange = { offsetMs ->
                            scope.launch {
                                val previousOffset = cloudMusicLyricDefaultOffsetMs
                                if (previousOffset == offsetMs) {
                                    return@launch
                                }
                                PlayerManager.rebaseUserLyricOffsetsForSource(
                                    targetSource = MusicPlatform.CLOUD_MUSIC,
                                    previousDefaultOffsetMs = previousOffset,
                                    newDefaultOffsetMs = offsetMs
                                )
                                runCatching {
                                    repo.setCloudMusicLyricDefaultOffsetMs(offsetMs)
                                }.onFailure {
                                    PlayerManager.rebaseUserLyricOffsetsForSource(
                                        targetSource = MusicPlatform.CLOUD_MUSIC,
                                        previousDefaultOffsetMs = offsetMs,
                                        newDefaultOffsetMs = previousOffset
                                    )
                                }.getOrThrow()
                            }
                        },
                        qqMusicLyricDefaultOffsetMs = qqMusicLyricDefaultOffsetMs,
                        onQqMusicLyricDefaultOffsetMsChange = { offsetMs ->
                            scope.launch {
                                val previousOffset = qqMusicLyricDefaultOffsetMs
                                if (previousOffset == offsetMs) {
                                    return@launch
                                }
                                PlayerManager.rebaseUserLyricOffsetsForSource(
                                    targetSource = MusicPlatform.QQ_MUSIC,
                                    previousDefaultOffsetMs = previousOffset,
                                    newDefaultOffsetMs = offsetMs
                                )
                                runCatching {
                                    repo.setQqMusicLyricDefaultOffsetMs(offsetMs)
                                }.onFailure {
                                    PlayerManager.rebaseUserLyricOffsetsForSource(
                                        targetSource = MusicPlatform.QQ_MUSIC,
                                        previousDefaultOffsetMs = offsetMs,
                                        newDefaultOffsetMs = previousOffset
                                    )
                                }.getOrThrow()
                            }
                        },
                        floatingLyricsPreferences = floatingLyricsPreferences,
                        onFloatingLyricsPreferencesChange = { preferences ->
                            scope.launch { repo.setFloatingLyricsPreferences(preferences) }
                        },
                        advancedBlurEnabled = advancedBlurEnabled,
                        onAdvancedBlurEnabledChange = { enabled ->
                            scope.launch { repo.setAdvancedBlurEnabled(enabled) }
                        },
                        enhancedAdvancedBlurEnabled = enhancedAdvancedBlurEnabled,
                        onEnhancedAdvancedBlurEnabledChange = { enabled ->
                            scope.launch {
                                repo.setEnhancedAdvancedBlurEnabled(enabled)
                            }
                        },
                        enhancedAdvancedBlurRadiusDp = enhancedAdvancedBlurRadiusDp,
                        onEnhancedAdvancedBlurRadiusDpChange = { radiusDp ->
                            scope.launch {
                                repo.setEnhancedAdvancedBlurRadiusDp(radiusDp)
                            }
                        },
                        advancedBlurQuality = advancedBlurQuality,
                        onAdvancedBlurQualityChange = { quality ->
                            scope.launch { repo.setAdvancedBlurQuality(quality) }
                        },
                        nowPlayingAudioReactiveEnabled = nowPlayingAudioReactiveEnabled,
                        onNowPlayingAudioReactiveEnabledChange = { enabled ->
                            scope.launch { repo.setNowPlayingAudioReactiveEnabled(enabled) }
                        },
                        nowPlayingDynamicBackgroundEnabled = nowPlayingDynamicBackgroundEnabled,
                        onNowPlayingDynamicBackgroundEnabledChange = { enabled ->
                            scope.launch { repo.setNowPlayingDynamicBackgroundEnabled(enabled) }
                        },
                        nowPlayingCoverBlurBackgroundEnabled =
                            nowPlayingCoverBlurBackgroundEnabled,
                        onNowPlayingCoverBlurBackgroundEnabledChange = { enabled ->
                            scope.launch {
                                repo.setNowPlayingCoverBlurBackgroundEnabled(enabled)
                            }
                        },
                        nowPlayingCoverBlurAmount = nowPlayingCoverBlurAmount,
                        onNowPlayingCoverBlurAmountChange = { amount ->
                            scope.launch { repo.setNowPlayingCoverBlurAmount(amount) }
                        },
                        nowPlayingCoverBlurDarken = nowPlayingCoverBlurDarken,
                        onNowPlayingCoverBlurDarkenChange = { amount ->
                            scope.launch { repo.setNowPlayingCoverBlurDarken(amount) }
                        },
                        lyricFontScales = lyricFontScales,
                        onLyricFontScaleChange = { target: LyricFontScaleTarget, scale ->
                            scope.launch { repo.setLyricFontScale(target, scale) }
                        },
                        uiDensityScale = uiDensityScale,
                        onUiDensityScaleChange = { scale ->
                            scope.launch { repo.setUiDensityScale(scale) }
                        },
                        bypassProxy = bypassProxy,
                        onBypassProxyChange = { enabled ->
                            scope.launch { repo.setBypassProxy(enabled) }
                        },
                        backgroundImageUri = backgroundImageUri,
                        onBackgroundImageChange = { uri ->
                            scope.launch { repo.setBackgroundImageUri(uri?.toString()) }
                        },
                        downloadDirectoryUri = downloadDirectoryUri,
                        downloadFileNameTemplate = downloadFileNameTemplate,
                        onDownloadDirectoryUriChange = { uri, label ->
                            scope.launch {
                                repo.setDownloadDirectory(uri, label)
                                ManagedDownloadStorage.updateConfiguredTreeUri(uri)
                                ManagedDownloadStorage.updateCustomDirectoryLabel(label)
                            }
                        },
                        onDownloadFileNameTemplateChange = { template ->
                            scope.launch { repo.setDownloadFileNameTemplate(template) }
                        },
                        backgroundImageBlur = backgroundImageBlur,
                        onBackgroundImageBlurChange = {},
                        onBackgroundImageBlurChangeFinished = { blur ->
                            scope.launch { repo.setBackgroundImageBlur(blur) }
                        },
                        backgroundImageAlpha = effectiveBackgroundImageAlpha,
                        onBackgroundImageAlphaChange = { alpha ->
                            pendingBackgroundImageAlpha = alpha
                        },
                        onBackgroundImageAlphaChangeFinished = { alpha ->
                            pendingBackgroundImageAlpha = alpha
                            scope.launch { repo.setBackgroundImageAlpha(alpha) }
                        },
                        defaultStartDestination = currentDefaultStartDestination,
                        onDefaultStartDestinationChange = { route ->
                            scope.launch { repo.setDefaultStartDestination(route) }
                        },
                        showHomeContinueCard = showHomeContinueCard,
                        onShowHomeContinueCardChange = { enabled ->
                            scope.launch { repo.setHomeCardContinue(enabled) }
                        },
                        showHomeTrendingCard = showHomeTrendingCard,
                        onShowHomeTrendingCardChange = { enabled ->
                            scope.launch { repo.setHomeCardTrending(enabled) }
                        },
                        showHomeRadarCard = showHomeRadarCard,
                        onShowHomeRadarCardChange = { enabled ->
                            scope.launch { repo.setHomeCardRadar(enabled) }
                        },
                        showHomeRecommendedCard = showHomeRecommendedCard,
                        onShowHomeRecommendedCardChange = { enabled ->
                            scope.launch { repo.setHomeCardRecommended(enabled) }
                        },
                        homeHasRecentUsage = homeUsageSnapshot.entries.isNotEmpty(),
                        playbackFadeIn = playbackFadeIn,
                        onPlaybackFadeInChange = { enabled ->
                            scope.launch { repo.setPlaybackFadeIn(enabled) }
                        },
                        playbackCrossfadeNext = playbackCrossfadeNext,
                        onPlaybackCrossfadeNextChange = { enabled ->
                            scope.launch { repo.setPlaybackCrossfadeNext(enabled) }
                        },
                        sleepTimerFinishCurrentOnExpiry = sleepTimerFinishCurrentOnExpiry,
                        onSleepTimerFinishCurrentOnExpiryChange = { enabled ->
                            scope.launch {
                                repo.setSleepTimerFinishCurrentOnExpiry(enabled)
                            }
                        },
                        playbackFadeInDurationMs = playbackFadeInDurationMs,
                        onPlaybackFadeInDurationMsChange = { duration ->
                            scope.launch { repo.setPlaybackFadeInDurationMs(duration) }
                        },
                        playbackFadeOutDurationMs = playbackFadeOutDurationMs,
                        onPlaybackFadeOutDurationMsChange = { duration ->
                            scope.launch { repo.setPlaybackFadeOutDurationMs(duration) }
                        },
                        playbackCrossfadeInDurationMs = playbackCrossfadeInDurationMs,
                        onPlaybackCrossfadeInDurationMsChange = { duration ->
                            scope.launch { repo.setPlaybackCrossfadeInDurationMs(duration) }
                        },
                        playbackCrossfadeOutDurationMs = playbackCrossfadeOutDurationMs,
                        onPlaybackCrossfadeOutDurationMsChange = { duration ->
                            scope.launch { repo.setPlaybackCrossfadeOutDurationMs(duration) }
                        },
                        playbackVolumeNormalizationEnabled =
                            playbackVolumeNormalizationEnabled,
                        onPlaybackVolumeNormalizationEnabledChange = { enabled ->
                            PlayerManager.setPlaybackVolumeNormalizationEnabled(enabled)
                        },
                        playbackHighResolutionOutputEnabled =
                            playbackHighResolutionOutputEnabled,
                        onPlaybackHighResolutionOutputEnabledChange = { enabled ->
                            PlayerManager.setPlaybackHighResolutionOutputEnabled(enabled)
                            AppFeedback.show(
                                context = context,
                                message = composeResources.getString(R.string.settings_restart_hint)
                            )
                        },
                        playbackVolumeBalance = playbackVolumeBalance,
                        onPlaybackVolumeBalanceChange = { balance ->
                            PlayerManager.setPlaybackVolumeBalance(balance)
                        },
                        keepLastPlaybackProgress = keepLastPlaybackProgress,
                        onKeepLastPlaybackProgressChange = { enabled ->
                            scope.launch { repo.setKeepLastPlaybackProgress(enabled) }
                        },
                        rememberLongFormPlaybackProgress = rememberLongFormPlaybackProgress,
                        onRememberLongFormPlaybackProgressChange = { enabled ->
                            scope.launch {
                                repo.setRememberLongFormPlaybackProgress(enabled)
                            }
                        },
                        keepPlaybackModeState = keepPlaybackModeState,
                        onKeepPlaybackModeStateChange = { enabled ->
                            scope.launch { repo.setKeepPlaybackModeState(enabled) }
                        },
                        neteaseAutoSourceSwitch = neteaseAutoSourceSwitch,
                        onNeteaseAutoSourceSwitchChange = { enabled ->
                            scope.launch { repo.setNeteaseAutoSourceSwitch(enabled) }
                        },
                        neteaseLocalSourceFallback = neteaseLocalSourceFallback,
                        onNeteaseLocalSourceFallbackChange = { enabled ->
                            scope.launch { repo.setNeteaseLocalSourceFallback(enabled) }
                        },
                        stopOnBluetoothDisconnect = stopOnBluetoothDisconnect,
                        onStopOnBluetoothDisconnectChange = { enabled ->
                            scope.launch { repo.setStopOnBluetoothDisconnect(enabled) }
                        },
                        usbExclusivePlayback = usbExclusivePlayback,
                        onUsbExclusivePlaybackChange = { enabled ->
                            if (PlayerManager.beginUsbExclusiveToggleTransitionFromUi(enabled)) {
                                scope.launch { repo.setUsbExclusivePlayback(enabled) }
                            }
                        },
                        allowMixedPlayback = allowMixedPlayback,
                        onAllowMixedPlaybackChange = { enabled ->
                            scope.launch { repo.setAllowMixedPlayback(enabled) }
                        },
                        preemptAudioFocus = preemptAudioFocus,
                        onPreemptAudioFocusChange = { enabled ->
                            scope.launch { repo.setPreemptAudioFocus(enabled) }
                        },
                        maxCacheSizeBytes = maxCacheSizeBytes,
                        onMaxCacheSizeBytesChange = { size ->
                            scope.launch { repo.setMaxCacheSizeBytes(size) }
                        },
                        onClearCacheClick = { options ->
                            scope.launch {
                                val messages = mutableListOf<String>()
                                if (options.needsPlayerCacheClear) {
                                    val (_, message) = PlayerManager.clearCache(
                                        clearAudio = options.audioCache,
                                        clearImage = options.imageCache
                                    )
                                    messages += message
                                }
                                if (options.needsExtraCacheClear) {
                                    if (options.lyricsCache) {
                                        PlayerLyricsProvider.clearLyricsCaches(
                                            neteaseLyricsCache = PlayerManager.neteaseLyricsCache,
                                            ytMusicLyricsCache = PlayerManager.ytMusicLyricsCache
                                        )
                                        withContext(Dispatchers.IO) {
                                            PlayerLyricsProvider.clearPersistentLyricCache(
                                                AppContainer.applicationContext
                                            )
                                        }
                                    }
                                    val result = clearExtraStorageCaches(context, options)
                                    messages += when {
                                        !result.success -> composeResources.getString(
                                            R.string.storage_extra_cache_clear_partial
                                        )
                                        result.roomBytesMadeReusable > 0L ->
                                            composeResources.getString(
                                                R.string.storage_extra_cache_clear_room_complete,
                                                formatFileSize(result.freedBytes),
                                                formatFileSize(result.roomBytesMadeReusable)
                                            )
                                        else -> composeResources.getString(
                                            R.string.storage_extra_cache_clear_complete,
                                            formatFileSize(result.freedBytes)
                                        )
                                    }
                                }
                                snackbarHostState.showNeriSnackbar(messages.joinToString(" · "))
                            }
                        },
                        onBeforeLanguageRestart = clearThemeRevealState,
                        onLanguageChanged = onLanguageChanged,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
```


## `app/src/main/java/moe/ouom/neriplayer/ui/onboarding/StartupOnboardingScreen.kt`
### StartupOnboardingScreen.kt conflict 1

```kotlin
                    biliState = biliState.health.state,
                    hasSavedBiliCookies = biliState.hasSavedCookies,
                    neteaseState = neteaseState.health.state,
                    hasSavedNeteaseCookies = neteaseState.hasSavedCookies,
                    youTubeState = youTubeState.health.state,
                    hasSavedYouTubeAuth = youTubeState.hasSavedAuth,
                    kugouLoggedIn = kugouLoggedIn,
                    qqMusicLoggedIn = qqMusicLoggedIn,
                    onOpenBili = {
                        inlineMessage = null
                        biliSheetTab = 0
                        showBiliSheet = true
                    },
                    onManageBili = {
                        inlineMessage = null
                        showBiliSavedCookieDialog = true
                    },
                    onOpenNetease = {
                        inlineMessage = null
                        neteaseSheetTab = 0
                        showNeteaseSheet = true
                    },
                    onManageNetease = {
                        inlineMessage = null
                        showNeteaseSavedCookieDialog = true
                    },
                    onOpenYouTube = {
```

### StartupOnboardingScreen.kt conflict 2

```kotlin
                        showYouTubeSavedCookieDialog = true
                    },
                    onOpenKugou = {
                        inlineMessage = null
                        showKugouSheet = true
                    },
                    onKugouLogout = {
                        inlineMessage = null
                        showKugouLogoutDialog = true
                    },
                    onOpenQqMusic = {
                        inlineMessage = null
                        showQqMusicSheet = true
                    },
                    onQqMusicLogout = {
                        inlineMessage = null
                        showQqMusicLogoutDialog = true
```

### StartupOnboardingScreen.kt conflict 3

```kotlin
private fun PlatformContent(
    inlineMessage: String?,
    onInlineMessageChange: (String?) -> Unit,
    biliState: SavedCookieAuthState,
    hasSavedBiliCookies: Boolean,
    neteaseState: SavedCookieAuthState,
    hasSavedNeteaseCookies: Boolean,
    youTubeState: YouTubeAuthState,
    hasSavedYouTubeAuth: Boolean,
    kugouLoggedIn: Boolean,
    qqMusicLoggedIn: Boolean,
    onOpenBili: () -> Unit,
    onOpenKugou: () -> Unit,
    onKugouLogout: () -> Unit,
    onOpenQqMusic: () -> Unit,
    onQqMusicLogout: () -> Unit,
    onManageBili: () -> Unit,
    onOpenNetease: () -> Unit,
    onManageNetease: () -> Unit,
    onOpenYouTube: () -> Unit,
    onManageYouTube: () -> Unit
) {
    StepHeader(
        icon = Icons.Outlined.Tune,
        title = stringResource(R.string.onboarding_platforms_title),
        description = stringResource(R.string.onboarding_platforms_desc)
    )
    Spacer(Modifier.height(18.dp))
    inlineMessage?.let {
        InlineMessage(text = it, onClose = { onInlineMessageChange(null) })
        Spacer(Modifier.height(14.dp))
    }
    PlatformCard(
        icon = painterResource(R.drawable.ic_bilibili),
        title = stringResource(R.string.platform_bilibili),
        status = statusTextForSavedCookie(biliState),
        connected = biliState == SavedCookieAuthState.Valid,
        actionText = if (hasSavedBiliCookies) {
            stringResource(R.string.onboarding_platform_action_manage)
        } else if (biliState == SavedCookieAuthState.Valid) {
            stringResource(R.string.onboarding_platform_action_logout)
        } else {
            stringResource(R.string.onboarding_platform_action_connect)
        },
        onClick = if (hasSavedBiliCookies) onManageBili else onOpenBili
    )
    Spacer(Modifier.height(12.dp))
    PlatformCard(
        icon = painterResource(R.drawable.ic_netease_cloud_music),
        title = stringResource(R.string.platform_netease),
        status = statusTextForSavedCookie(neteaseState),
        connected = neteaseState == SavedCookieAuthState.Valid,
        actionText = if (hasSavedNeteaseCookies) {
            stringResource(R.string.onboarding_platform_action_manage)
        } else if (neteaseState == SavedCookieAuthState.Valid) {
            stringResource(R.string.onboarding_platform_action_logout)
        } else {
            stringResource(R.string.onboarding_platform_action_connect)
        },
        onClick = if (hasSavedNeteaseCookies) onManageNetease else onOpenNetease
    )
    Spacer(Modifier.height(12.dp))
    PlatformCard(
        icon = painterResource(R.drawable.ic_youtube),
        title = stringResource(R.string.common_youtube),
        status = statusTextForYouTube(youTubeState),
        connected = youTubeState == YouTubeAuthState.Valid,
        actionText = if (hasSavedYouTubeAuth) {
            stringResource(R.string.onboarding_platform_action_manage)
        } else if (youTubeState == YouTubeAuthState.Valid) {
            stringResource(R.string.onboarding_platform_action_logout)
        } else {
            stringResource(R.string.onboarding_platform_action_connect)
        },
        onClick = if (hasSavedYouTubeAuth) onManageYouTube else onOpenYouTube
    )
    Spacer(Modifier.height(12.dp))
    PlatformCard(
        icon = painterResource(R.drawable.ic_kugou),
        title = stringResource(R.string.platform_kugou),
        status = if (kugouLoggedIn) {
            stringResource(R.string.onboarding_platform_status_connected)
        } else {
            stringResource(R.string.onboarding_platform_status_not_connected)
        },
        connected = kugouLoggedIn,
        actionText = if (kugouLoggedIn) {
            stringResource(R.string.onboarding_platform_action_logout)
        } else {
            stringResource(R.string.onboarding_platform_action_connect)
        },
        onClick = if (kugouLoggedIn) onKugouLogout else onOpenKugou
    )
    Spacer(Modifier.height(12.dp))
    PlatformCard(
        icon = painterResource(R.drawable.ic_qq_music),
        title = stringResource(R.string.settings_qq_music),
        status = if (qqMusicLoggedIn) {
            stringResource(R.string.onboarding_platform_status_connected)
        } else {
            stringResource(R.string.onboarding_platform_status_not_connected)
        },
        connected = qqMusicLoggedIn,
        actionText = if (qqMusicLoggedIn) {
            stringResource(R.string.onboarding_platform_action_logout)
        } else {
            stringResource(R.string.onboarding_platform_action_connect)
        },
        onClick = if (qqMusicLoggedIn) onQqMusicLogout else onOpenQqMusic
    )
    Spacer(Modifier.height(18.dp))
    HintCard(body = stringResource(R.string.onboarding_platforms_hint))
}

@Composable
private fun BackupRestoreContent(
    gitHubState: GitHubSyncUiState,
    webDavState: WebDavSyncUiState,
    onDismissGitHubMessage: () -> Unit,
    onDismissWebDavMessage: () -> Unit,
    onOpenGitHubConfig: () -> Unit,
    onOpenClearGitHubConfig: () -> Unit,
    onToggleGitHubAutoSync: (Boolean) -> Unit,
    onGitHubSyncNow: () -> Unit,
    onOpenWebDavConfig: () -> Unit,
    onOpenClearWebDavConfig: () -> Unit,
    onToggleWebDavAutoSync: (Boolean) -> Unit,
    onWebDavSyncNow: () -> Unit
) {
    StepHeader(
        icon = Icons.Outlined.CloudSync,
        title = stringResource(R.string.onboarding_backup_restore_title),
        description = stringResource(R.string.onboarding_backup_restore_desc)
    )
    Spacer(Modifier.height(18.dp))
    gitHubState.errorMessage?.let {
        InlineMessage(text = it, onClose = onDismissGitHubMessage)
        Spacer(Modifier.height(14.dp))
    }
    gitHubState.successMessage?.let {
        InlineMessage(text = it, onClose = onDismissGitHubMessage)
        Spacer(Modifier.height(14.dp))
    }
    webDavState.errorMessage?.let {
        InlineMessage(text = it, onClose = onDismissWebDavMessage)
        Spacer(Modifier.height(14.dp))
    }
    webDavState.successMessage?.let {
        InlineMessage(text = it, onClose = onDismissWebDavMessage)
        Spacer(Modifier.height(14.dp))
    }
    GitHubSyncCard(
        state = gitHubState,
        onOpenConfig = onOpenGitHubConfig,
        onOpenClearConfig = onOpenClearGitHubConfig,
        onToggleAutoSync = onToggleGitHubAutoSync,
        onSyncNow = onGitHubSyncNow
    )
    Spacer(Modifier.height(14.dp))
    WebDavSyncCard(
        state = webDavState,
        onOpenConfig = onOpenWebDavConfig,
        onOpenClearConfig = onOpenClearWebDavConfig,
        onToggleAutoSync = onToggleWebDavAutoSync,
        onSyncNow = onWebDavSyncNow
    )
    Spacer(Modifier.height(18.dp))
    HintCard(body = stringResource(R.string.onboarding_backup_restore_hint))
}

@Composable
```

### StartupOnboardingScreen.kt conflict 4

```kotlin
private fun PlatformCard(
    icon: Painter,
    title: String,
    status: String,
    connected: Boolean,
    actionText: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    OnboardingGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OnboardingCardShape)
            .clickable(onClick = onClick),
        shape = OnboardingCardShape,
        color = if (connected) colors.secondaryContainer else colors.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = OnboardingControlShape,
                color = colors.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = icon, contentDescription = title, tint = colors.onSurface, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(6.dp))
                StatusPill(status, connected)
            }
            OnboardingActionButton(
                text = actionText,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun OnboardingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HapticOutlinedButton(
        onClick = onClick,
        modifier = modifier
            .widthIn(max = 104.dp)
            .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp),
        shape = OnboardingControlShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusPill(label: String, connected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val textStyle = when {
        label.length >= 14 -> MaterialTheme.typography.labelSmall
        label.length >= 10 -> MaterialTheme.typography.labelMedium
        else -> MaterialTheme.typography.labelLarge
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (connected) colors.primary.copy(alpha = 0.14f) else colors.outlineVariant.copy(alpha = 0.6f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = textStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = if (connected) colors.primary else colors.onSurfaceVariant
        )
    }
}

@Composable
private fun HintCard(
    title: String? = null,
    body: String,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    OnboardingGlassSurface(
        shape = OnboardingCardShape,
        color = colors.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun GitHubSyncCard(
    state: GitHubSyncUiState,
    onOpenConfig: () -> Unit,
    onOpenClearConfig: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSyncNow: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val repoFullName = listOf(state.repoOwner, state.repoName)
        .filter { it.isNotBlank() }
        .takeIf { it.size == 2 }
        ?.joinToString("/")
    val primarySupportingText = if (state.isConfigured) {
        repoFullName?.let { stringResource(R.string.onboarding_github_repo_configured, it) }
            ?: stringResource(R.string.settings_configured)
    } else {
        null
    }
    val secondarySupportingText = if (state.isConfigured) {
        if (state.lastSyncTime > 0) {
            stringResource(R.string.sync_last_time, formatSyncTime(state.lastSyncTime))
        } else {
            stringResource(R.string.sync_not_synced)
        }
    } else {
        null
    }

    OnboardingGlassSurface(
        shape = OnboardingCardShape,
        color = if (state.isConfigured) {
            colors.secondaryContainer
        } else {
            colors.surfaceContainerHigh
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = OnboardingControlShape,
                    color = colors.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = stringResource(R.string.common_github),
                            tint = colors.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_backup_restore_github_title
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    StatusPill(
                        label = if (state.isConfigured) {
                            stringResource(R.string.settings_configured)
                        } else {
                            stringResource(R.string.settings_not_configured)
                        },
                        connected = state.isConfigured
                    )
                    primarySupportingText?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    secondarySupportingText?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                OnboardingActionButton(
                    text = if (state.isConfigured) {
                        stringResource(R.string.onboarding_platform_action_manage)
                    } else {
                        stringResource(R.string.settings_configure)
                    },
                    onClick = onOpenConfig
                )
            }

            if (state.isConfigured) {
                Surface(
                    shape = OnboardingControlShape,
                    color = colors.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sync_auto),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.sync_auto_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoSyncEnabled,
                            onCheckedChange = onToggleAutoSync
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        HapticOutlinedButton(onClick = onSyncNow, shape = OnboardingControlShape) {
                            Text(stringResource(R.string.settings_sync_now))
                        }
                    }
                    HapticTextButton(onClick = onOpenClearConfig) {
                        Text(
                            text = stringResource(R.string.settings_clear_config),
                            color = colors.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebDavSyncCard(
    state: WebDavSyncUiState,
    onOpenConfig: () -> Unit,
    onOpenClearConfig: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSyncNow: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val endpoint = state.serverUrl.takeIf { it.isNotBlank() }?.let { serverUrl ->
        state.basePath.takeIf { it.isNotBlank() }?.let { basePath ->
            "$serverUrl/$basePath"
        } ?: serverUrl
    }

    OnboardingGlassSurface(
        shape = OnboardingCardShape,
        color = if (state.isConfigured) {
            colors.secondaryContainer
        } else {
            colors.surfaceContainerHigh
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = OnboardingControlShape,
                    color = colors.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CloudSync,
                            contentDescription = stringResource(
                                R.string.onboarding_backup_restore_webdav_title
                            ),
                            tint = colors.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_backup_restore_webdav_title
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    StatusPill(
                        label = if (state.isConfigured) {
                            stringResource(R.string.settings_configured)
                        } else {
                            stringResource(R.string.settings_not_configured)
                        },
                        connected = state.isConfigured
                    )
                    endpoint?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.onboarding_backup_restore_webdav_endpoint,
                                it
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (state.isConfigured) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (state.lastSyncTime > 0) {
                                stringResource(
                                    R.string.sync_last_time,
                                    formatSyncTime(state.lastSyncTime)
                                )
                            } else {
                                stringResource(R.string.sync_not_synced)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                OnboardingActionButton(
                    text = if (state.isConfigured) {
                        stringResource(R.string.onboarding_platform_action_manage)
                    } else {
                        stringResource(R.string.settings_configure)
                    },
                    onClick = onOpenConfig
                )
            }

            if (state.isConfigured) {
                Surface(
                    shape = OnboardingControlShape,
                    color = colors.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sync_auto),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.webdav_auto_sync_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoSyncEnabled,
                            onCheckedChange = onToggleAutoSync
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        HapticOutlinedButton(onClick = onSyncNow, shape = OnboardingControlShape) {
                            Text(stringResource(R.string.settings_sync_now))
                        }
                    }
                    HapticTextButton(onClick = onOpenClearConfig) {
                        Text(
                            text = stringResource(R.string.settings_clear_config),
                            color = colors.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupPlatformLogoutDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Text(message)
        },
        confirmButton = {
            HapticTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            HapticTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
```


## `app/src/main/java/moe/ouom/neriplayer/ui/screen/host/LibraryHostScreen.kt`
### LibraryHostScreen.kt conflict 1

```kotlin
    data class YouTubeMusic(val playlist: YouTubeMusicPlaylist) : LibrarySelectedItem()
    @Parcelize
    data class KugouLibrary(val kind: KugouLibraryEntryKind) : LibrarySelectedItem()
    @Parcelize
    data class KugouPlaylist(val playlist: KugouPlaylistSelection) : LibrarySelectedItem()
    @Parcelize
    data class QqMusicPlaylist(val playlist: QQMusicPlaylistSelection) : LibrarySelectedItem()
```

### LibraryHostScreen.kt conflict 2

```kotlin
private val librarySelectedItemSaver = mapSaver<LibrarySelectedItem?>(
    save = { item ->
        when (item) {
            null -> emptyMap()
            is LibrarySelectedItem.Local -> hashMapOf(
                "type" to "local",
                "playlistId" to item.playlistId
            )
            is LibrarySelectedItem.LocalArtist -> hashMapOf(
                "type" to "localArtist",
                "artistName" to item.artistName
            )
            is LibrarySelectedItem.Hot -> hashMapOf(
                "type" to "hot",
                "monthly" to item.monthly
            )
            is LibrarySelectedItem.NeteaseAlbum -> hashMapOf(
                "type" to "neteaseAlbum",
                "album" to item.album.toSaveMap()
            )
            is LibrarySelectedItem.Netease -> hashMapOf(
                "type" to "netease",
                "playlist" to item.playlist.toSaveMap()
            )
            is LibrarySelectedItem.NeteaseArtist -> hashMapOf(
                "type" to "neteaseArtist",
                "artistId" to item.artist.id,
                "artistName" to item.artist.name
            )
            is LibrarySelectedItem.NeteaseArtistAlbum -> hashMapOf(
                "type" to "neteaseArtistAlbum",
                "artistId" to item.artist.id,
                "artistName" to item.artist.name,
                "album" to item.album.toSaveMap()
            )
            is LibrarySelectedItem.Bili -> hashMapOf(
                "type" to "bili",
                "playlist" to item.playlist.toSaveMap()
            )
            is LibrarySelectedItem.YouTubeMusic -> hashMapOf(
                "type" to "ytmusic",
                "playlist" to item.playlist.toSaveMap()
            )
            is LibrarySelectedItem.KugouLibrary -> hashMapOf(
                "type" to "kugouLibrary",
                "kind" to item.kind.name
            )
            is LibrarySelectedItem.KugouPlaylist -> hashMapOf(
                "type" to "kugouPlaylist",
                "id" to item.playlist.globalCollectionId,
                "name" to item.playlist.name,
                "cover" to (item.playlist.coverUrl ?: ""),
                "creator" to (item.playlist.creator ?: "")
            )
            is LibrarySelectedItem.QqMusicPlaylist -> hashMapOf(
                "type" to "qqMusicPlaylist",
                "id" to item.playlist.playlistId,
                "name" to item.playlist.name,
                "cover" to (item.playlist.coverUrl ?: ""),
                "creator" to (item.playlist.creator ?: ""),
                "songCount" to item.playlist.songCount,
                "playCount" to item.playlist.playCount
            )
        }
    },
    restore = { saved ->
        when (saved["type"] as? String) {
            null -> null
            "local" -> (saved["playlistId"] as? Number)?.toLong()?.let { LibrarySelectedItem.Local(it) }
            "localArtist" -> (saved["artistName"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?.let { LibrarySelectedItem.LocalArtist(it) }
            "hot" -> LibrarySelectedItem.Hot(saved["monthly"] as? Boolean ?: false)
            "neteaseAlbum" -> restoreAlbumSummary(saved["album"] as? Map<*, *>)?.let { LibrarySelectedItem.NeteaseAlbum(it) }
            "netease" -> restorePlaylistSummary(saved["playlist"] as? Map<*, *>)?.let { LibrarySelectedItem.Netease(it) }
            "neteaseArtist" -> restoreNeteaseArtistSummary(
                id = (saved["artistId"] as? Number)?.toLong(),
                name = saved["artistName"] as? String
            )?.let { LibrarySelectedItem.NeteaseArtist(it) }
            "neteaseArtistAlbum" -> {
                val artist = restoreNeteaseArtistSummary(
                    id = (saved["artistId"] as? Number)?.toLong(),
                    name = saved["artistName"] as? String
                )
                val album = restoreAlbumSummary(saved["album"] as? Map<*, *>)
                if (artist != null && album != null) {
                    LibrarySelectedItem.NeteaseArtistAlbum(artist, album)
                } else {
                    null
                }
            }
            "bili" -> restoreBiliPlaylist(saved["playlist"] as? Map<*, *>)?.let { LibrarySelectedItem.Bili(it) }
            "ytmusic" -> restoreYouTubeMusicPlaylist(saved["playlist"] as? Map<*, *>)?.let { LibrarySelectedItem.YouTubeMusic(it) }
            "kugouLibrary" -> (saved["kind"] as? String)
                ?.let { stored -> KugouLibraryEntryKind.entries.firstOrNull { it.name == stored } }
                ?.let { LibrarySelectedItem.KugouLibrary(it) }
            "kugouPlaylist" -> {
                val playlistId = saved["id"] as? String
                val playlistName = saved["name"] as? String
                if (!playlistId.isNullOrBlank() && !playlistName.isNullOrBlank()) {
                    LibrarySelectedItem.KugouPlaylist(
                        KugouPlaylistSelection(
                            globalCollectionId = playlistId,
                            name = playlistName,
                            coverUrl = (saved["cover"] as? String)?.takeIf { it.isNotBlank() },
                            creator = (saved["creator"] as? String)?.takeIf { it.isNotBlank() }
                        )
                    )
                } else {
                    null
                }
            }
            "qqMusicPlaylist" -> {
                val playlistId = saved["id"] as? String
                val playlistName = saved["name"] as? String
                if (!playlistId.isNullOrBlank() && !playlistName.isNullOrBlank()) {
                    LibrarySelectedItem.QqMusicPlaylist(
                        QQMusicPlaylistSelection(
                            playlistId = playlistId,
                            name = playlistName,
                            coverUrl = (saved["cover"] as? String)?.takeIf { it.isNotBlank() },
                            creator = (saved["creator"] as? String)?.takeIf { it.isNotBlank() },
                            songCount = (saved["songCount"] as? Number)?.toInt() ?: 0,
                            playCount = (saved["playCount"] as? Number)?.toLong() ?: 0L
                        )
                    )
                } else {
                    null
                }
            }
            else -> null
        }
    }
```


## `app/src/main/java/moe/ouom/neriplayer/ui/screen/host/SettingsHostScreen.kt`
### SettingsHostScreen.kt conflict 1

```kotlin
fun SettingsHostScreen(
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    themeMode: ThemeMode,
    onThemeToggleRequest: (Offset, Float) -> Unit,
    onThemeModeRequest: (ThemeMode, Offset, Float) -> Unit,
    preferredQuality: String,
    onQualityChange: (String) -> Unit,
    youtubePreferredQuality: String,
    onYouTubeQualityChange: (String) -> Unit,
    biliPreferredQuality: String,
    onBiliQualityChange: (String) -> Unit,
    kugouPreferredQuality: String,
    onKugouQualityChange: (String) -> Unit,
    qqMusicPreferredQuality: String,
    onQqMusicQualityChange: (String) -> Unit,
    mobileDataFollowDefaultAudioQuality: Boolean,
    onMobileDataFollowDefaultAudioQualityChange: (Boolean) -> Unit,
    mobileDataNeteaseAudioQuality: String,
    onMobileDataNeteaseAudioQualityChange: (String) -> Unit,
    mobileDataYouTubeAudioQuality: String,
    onMobileDataYouTubeAudioQualityChange: (String) -> Unit,
    mobileDataBiliAudioQuality: String,
    onMobileDataBiliAudioQualityChange: (String) -> Unit,
    seedColorHex: String,
    onSeedColorChange: (String) -> Unit,
    themeColorPalette: List<String>,
    onAddColorToPalette: (String) -> Unit,
    onRemoveColorFromPalette: (String) -> Unit,
    themePaletteStyle: String,
    onThemePaletteStyleChange: (String) -> Unit,
    themeColorSpec: String,
    onThemeColorSpecChange: (String) -> Unit,
    devModeEnabled: Boolean,
    onDevModeChange: (Boolean) -> Unit,
    lyricBlurEnabled: Boolean,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    lyricBlurAmount: Float,
    onLyricBlurAmountChange: (Float) -> Unit,
    cloudMusicLyricDefaultOffsetMs: Long,
    onCloudMusicLyricDefaultOffsetMsChange: (Long) -> Unit,
    qqMusicLyricDefaultOffsetMs: Long,
    onQqMusicLyricDefaultOffsetMsChange: (Long) -> Unit,
    floatingLyricsPreferences: FloatingLyricsPreferences,
    onFloatingLyricsPreferencesChange: (FloatingLyricsPreferences) -> Unit,
    advancedBlurEnabled: Boolean,
    onAdvancedBlurEnabledChange: (Boolean) -> Unit,
    enhancedAdvancedBlurEnabled: Boolean,
    onEnhancedAdvancedBlurEnabledChange: (Boolean) -> Unit,
    enhancedAdvancedBlurRadiusDp: Float,
    onEnhancedAdvancedBlurRadiusDpChange: (Float) -> Unit,
    advancedBlurQuality: AdvancedBlurQuality,
    onAdvancedBlurQualityChange: (AdvancedBlurQuality) -> Unit,
    nowPlayingAudioReactiveEnabled: Boolean,
    onNowPlayingAudioReactiveEnabledChange: (Boolean) -> Unit,
    nowPlayingDynamicBackgroundEnabled: Boolean,
    onNowPlayingDynamicBackgroundEnabledChange: (Boolean) -> Unit,
    nowPlayingCoverBlurBackgroundEnabled: Boolean,
    onNowPlayingCoverBlurBackgroundEnabledChange: (Boolean) -> Unit,
    nowPlayingCoverBlurAmount: Float,
    onNowPlayingCoverBlurAmountChange: (Float) -> Unit,
    nowPlayingCoverBlurDarken: Float,
    onNowPlayingCoverBlurDarkenChange: (Float) -> Unit,
    lyricFontScales: LyricFontScales,
    onLyricFontScaleChange: (LyricFontScaleTarget, Float) -> Unit,
    uiDensityScale: Float,
    onUiDensityScaleChange: (Float) -> Unit,
    bypassProxy: Boolean,
    onBypassProxyChange: (Boolean) -> Unit,
    backgroundImageUri: String?,
    onBackgroundImageChange: (Uri?) -> Unit,
    downloadDirectoryUri: String?,
    downloadFileNameTemplate: String?,
    onDownloadDirectoryUriChange: (String?, String?) -> Unit,
    onDownloadFileNameTemplateChange: (String?) -> Unit,
    backgroundImageBlur: Float,
    onBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageBlurChangeFinished: (Float) -> Unit,
    backgroundImageAlpha: Float,
    onBackgroundImageAlphaChange: (Float) -> Unit,
    onBackgroundImageAlphaChangeFinished: (Float) -> Unit,
    defaultStartDestination: String,
    onDefaultStartDestinationChange: (String) -> Unit,
    showHomeContinueCard: Boolean,
    onShowHomeContinueCardChange: (Boolean) -> Unit,
    showHomeTrendingCard: Boolean,
    onShowHomeTrendingCardChange: (Boolean) -> Unit,
    showHomeRadarCard: Boolean,
    onShowHomeRadarCardChange: (Boolean) -> Unit,
    showHomeRecommendedCard: Boolean,
    onShowHomeRecommendedCardChange: (Boolean) -> Unit,
    homeHasRecentUsage: Boolean,
    playbackFadeIn: Boolean,
    onPlaybackFadeInChange: (Boolean) -> Unit,
    playbackCrossfadeNext: Boolean,
    onPlaybackCrossfadeNextChange: (Boolean) -> Unit,
    sleepTimerFinishCurrentOnExpiry: Boolean,
    onSleepTimerFinishCurrentOnExpiryChange: (Boolean) -> Unit,
    playbackFadeInDurationMs: Long,
    onPlaybackFadeInDurationMsChange: (Long) -> Unit,
    playbackFadeOutDurationMs: Long,
    onPlaybackFadeOutDurationMsChange: (Long) -> Unit,
    playbackCrossfadeInDurationMs: Long,
    onPlaybackCrossfadeInDurationMsChange: (Long) -> Unit,
    playbackCrossfadeOutDurationMs: Long,
    onPlaybackCrossfadeOutDurationMsChange: (Long) -> Unit,
    playbackVolumeNormalizationEnabled: Boolean,
    onPlaybackVolumeNormalizationEnabledChange: (Boolean) -> Unit,
    playbackHighResolutionOutputEnabled: Boolean,
    onPlaybackHighResolutionOutputEnabledChange: (Boolean) -> Unit,
    playbackVolumeBalance: Float,
    onPlaybackVolumeBalanceChange: (Float) -> Unit,
    keepLastPlaybackProgress: Boolean,
    onKeepLastPlaybackProgressChange: (Boolean) -> Unit,
    rememberLongFormPlaybackProgress: Boolean,
    onRememberLongFormPlaybackProgressChange: (Boolean) -> Unit,
    keepPlaybackModeState: Boolean,
    onKeepPlaybackModeStateChange: (Boolean) -> Unit,
    neteaseAutoSourceSwitch: Boolean,
    onNeteaseAutoSourceSwitchChange: (Boolean) -> Unit,
    neteaseLocalSourceFallback: Boolean,
    onNeteaseLocalSourceFallbackChange: (Boolean) -> Unit,
    stopOnBluetoothDisconnect: Boolean,
    onStopOnBluetoothDisconnectChange: (Boolean) -> Unit,
    usbExclusivePlayback: Boolean,
    onUsbExclusivePlaybackChange: (Boolean) -> Unit,
    allowMixedPlayback: Boolean,
    onAllowMixedPlaybackChange: (Boolean) -> Unit,
    preemptAudioFocus: Boolean,
    onPreemptAudioFocusChange: (Boolean) -> Unit,
    maxCacheSizeBytes: Long,
    onMaxCacheSizeBytesChange: (Long) -> Unit,
    onClearCacheClick: (StorageCacheClearOptions) -> Unit,
    onBeforeLanguageRestart: () -> Unit = {},
    onLanguageChanged: (LanguageManager.Language) -> Unit = {},
    coherentFeedbackEnabled: Boolean = false,
    renderScene: @Composable (
        revealTopFraction: Float,
        contentTranslationYFraction: Float,
        contentScale: Float,
        sceneDepth: Int,
        content: @Composable () -> Unit
    ) -> Unit = { _, _, _, _, content ->
        content()
    },
```

### SettingsHostScreen.kt conflict 2

```kotlin
    PredictiveBackHandler(enabled = requestedScreenState != SettingsScreenState.Settings) { progress ->
        try {
            progress.collect { }
            requestScreen(
                when (requestedScreenState) {
                SettingsScreenState.DownloadProgress -> SettingsScreenState.DownloadManager
                SettingsScreenState.DownloadManager -> SettingsScreenState.Settings
                SettingsScreenState.HomeSettings -> SettingsScreenState.Settings
                SettingsScreenState.TabOrderSettings -> SettingsScreenState.Settings
                SettingsScreenState.Settings -> SettingsScreenState.Settings
                }
            )
        } catch (_: CancellationException) {
        }
    }
```

### SettingsHostScreen.kt conflict 3

```kotlin
                        when (state) {
                            SettingsScreenState.Settings -> {
                                SettingsScreen(
                            listState = settingsListState,
                            dynamicColor = dynamicColor,
                            onDynamicColorChange = onDynamicColorChange,
                            isDarkTheme = isDarkTheme,
                            themeMode = themeMode,
                            onThemeToggleRequest = onThemeToggleRequest,
                            onThemeModeRequest = onThemeModeRequest,
                            preferredQuality = preferredQuality,
                            onQualityChange = onQualityChange,
                            youtubePreferredQuality = youtubePreferredQuality,
                            onYouTubeQualityChange = onYouTubeQualityChange,
                            biliPreferredQuality = biliPreferredQuality,
                            onBiliQualityChange = onBiliQualityChange,
                            kugouPreferredQuality = kugouPreferredQuality,
                            onKugouQualityChange = onKugouQualityChange,
                            qqMusicPreferredQuality = qqMusicPreferredQuality,
                            onQqMusicQualityChange = onQqMusicQualityChange,
                            mobileDataFollowDefaultAudioQuality =
                                mobileDataFollowDefaultAudioQuality,
                            onMobileDataFollowDefaultAudioQualityChange =
                                onMobileDataFollowDefaultAudioQualityChange,
                            mobileDataNeteaseAudioQuality = mobileDataNeteaseAudioQuality,
                            onMobileDataNeteaseAudioQualityChange =
                                onMobileDataNeteaseAudioQualityChange,
                            mobileDataYouTubeAudioQuality = mobileDataYouTubeAudioQuality,
                            onMobileDataYouTubeAudioQualityChange =
                                onMobileDataYouTubeAudioQualityChange,
                            mobileDataBiliAudioQuality = mobileDataBiliAudioQuality,
                            onMobileDataBiliAudioQualityChange =
                                onMobileDataBiliAudioQualityChange,
                            seedColorHex = seedColorHex,
                            onSeedColorChange = onSeedColorChange,
                            themeColorPalette = themeColorPalette,
                            onAddColorToPalette = onAddColorToPalette,
                            onRemoveColorFromPalette = onRemoveColorFromPalette,
                            themePaletteStyle = themePaletteStyle,
                            onThemePaletteStyleChange = onThemePaletteStyleChange,
                            themeColorSpec = themeColorSpec,
                            onThemeColorSpecChange = onThemeColorSpecChange,
                            devModeEnabled = devModeEnabled,
                            onDevModeChange = onDevModeChange,
                            lyricBlurEnabled = lyricBlurEnabled,
                            onLyricBlurEnabledChange = onLyricBlurEnabledChange,
                            lyricBlurAmount = lyricBlurAmount,
                            onLyricBlurAmountChange = onLyricBlurAmountChange,
                            cloudMusicLyricDefaultOffsetMs = cloudMusicLyricDefaultOffsetMs,
                            onCloudMusicLyricDefaultOffsetMsChange = onCloudMusicLyricDefaultOffsetMsChange,
                            qqMusicLyricDefaultOffsetMs = qqMusicLyricDefaultOffsetMs,
                            onQqMusicLyricDefaultOffsetMsChange = onQqMusicLyricDefaultOffsetMsChange,
                            floatingLyricsPreferences = floatingLyricsPreferences,
                            onFloatingLyricsPreferencesChange = onFloatingLyricsPreferencesChange,
                            advancedBlurEnabled = advancedBlurEnabled,
                            onAdvancedBlurEnabledChange = onAdvancedBlurEnabledChange,
                            enhancedAdvancedBlurEnabled = enhancedAdvancedBlurEnabled,
                            onEnhancedAdvancedBlurEnabledChange =
                                onEnhancedAdvancedBlurEnabledChange,
                            enhancedAdvancedBlurRadiusDp = enhancedAdvancedBlurRadiusDp,
                            onEnhancedAdvancedBlurRadiusDpChange =
                                onEnhancedAdvancedBlurRadiusDpChange,
                            advancedBlurQuality = advancedBlurQuality,
                            onAdvancedBlurQualityChange = onAdvancedBlurQualityChange,
                            nowPlayingAudioReactiveEnabled = nowPlayingAudioReactiveEnabled,
                            onNowPlayingAudioReactiveEnabledChange = onNowPlayingAudioReactiveEnabledChange,
                            nowPlayingDynamicBackgroundEnabled = nowPlayingDynamicBackgroundEnabled,
                            onNowPlayingDynamicBackgroundEnabledChange = onNowPlayingDynamicBackgroundEnabledChange,
                            nowPlayingCoverBlurBackgroundEnabled = nowPlayingCoverBlurBackgroundEnabled,
                            onNowPlayingCoverBlurBackgroundEnabledChange = onNowPlayingCoverBlurBackgroundEnabledChange,
                            nowPlayingCoverBlurAmount = nowPlayingCoverBlurAmount,
                            onNowPlayingCoverBlurAmountChange = onNowPlayingCoverBlurAmountChange,
                            nowPlayingCoverBlurDarken = nowPlayingCoverBlurDarken,
                            onNowPlayingCoverBlurDarkenChange = onNowPlayingCoverBlurDarkenChange,
                            lyricFontScales = lyricFontScales,
                            onLyricFontScaleChange = onLyricFontScaleChange,
                            uiDensityScale = uiDensityScale,
                            onUiDensityScaleChange = onUiDensityScaleChange,
                            bypassProxy = bypassProxy,
                            onBypassProxyChange = onBypassProxyChange,
                            backgroundImageUri = backgroundImageUri,
                            onBackgroundImageChange = onBackgroundImageChange,
                            downloadDirectoryUri = downloadDirectoryUri,
                            downloadFileNameTemplate = downloadFileNameTemplate,
                            onDownloadDirectoryUriChange = onDownloadDirectoryUriChange,
                            onDownloadFileNameTemplateChange = onDownloadFileNameTemplateChange,
                            backgroundImageBlur = backgroundImageBlur,
                            onBackgroundImageBlurChange = onBackgroundImageBlurChange,
                            onBackgroundImageBlurChangeFinished = onBackgroundImageBlurChangeFinished,
                            backgroundImageAlpha = backgroundImageAlpha,
                            onBackgroundImageAlphaChange = onBackgroundImageAlphaChange,
                            onBackgroundImageAlphaChangeFinished = onBackgroundImageAlphaChangeFinished,
                            defaultStartDestination = defaultStartDestination,
                            onDefaultStartDestinationChange = onDefaultStartDestinationChange,
                            showHomeContinueCard = showHomeContinueCard,
                            onShowHomeContinueCardChange = onShowHomeContinueCardChange,
                            showHomeTrendingCard = showHomeTrendingCard,
                            onShowHomeTrendingCardChange = onShowHomeTrendingCardChange,
                            showHomeRadarCard = showHomeRadarCard,
                            onShowHomeRadarCardChange = onShowHomeRadarCardChange,
                            showHomeRecommendedCard = showHomeRecommendedCard,
                            onShowHomeRecommendedCardChange = onShowHomeRecommendedCardChange,
                            homeHasRecentUsage = homeHasRecentUsage,
                            playbackFadeIn = playbackFadeIn,
                            onPlaybackFadeInChange = onPlaybackFadeInChange,
                            playbackCrossfadeNext = playbackCrossfadeNext,
                            onPlaybackCrossfadeNextChange = onPlaybackCrossfadeNextChange,
                            sleepTimerFinishCurrentOnExpiry = sleepTimerFinishCurrentOnExpiry,
                            onSleepTimerFinishCurrentOnExpiryChange =
                                onSleepTimerFinishCurrentOnExpiryChange,
                            playbackFadeInDurationMs = playbackFadeInDurationMs,
                            onPlaybackFadeInDurationMsChange = onPlaybackFadeInDurationMsChange,
                            playbackFadeOutDurationMs = playbackFadeOutDurationMs,
                            onPlaybackFadeOutDurationMsChange = onPlaybackFadeOutDurationMsChange,
                            playbackCrossfadeInDurationMs = playbackCrossfadeInDurationMs,
                            onPlaybackCrossfadeInDurationMsChange = onPlaybackCrossfadeInDurationMsChange,
                            playbackCrossfadeOutDurationMs = playbackCrossfadeOutDurationMs,
                            onPlaybackCrossfadeOutDurationMsChange = onPlaybackCrossfadeOutDurationMsChange,
                            playbackVolumeNormalizationEnabled = playbackVolumeNormalizationEnabled,
                            onPlaybackVolumeNormalizationEnabledChange =
                                onPlaybackVolumeNormalizationEnabledChange,
                            playbackHighResolutionOutputEnabled =
                                playbackHighResolutionOutputEnabled,
                            onPlaybackHighResolutionOutputEnabledChange =
                                onPlaybackHighResolutionOutputEnabledChange,
                            playbackVolumeBalance = playbackVolumeBalance,
                            onPlaybackVolumeBalanceChange = onPlaybackVolumeBalanceChange,
                            keepLastPlaybackProgress = keepLastPlaybackProgress,
                            onKeepLastPlaybackProgressChange = onKeepLastPlaybackProgressChange,
                            rememberLongFormPlaybackProgress = rememberLongFormPlaybackProgress,
                            onRememberLongFormPlaybackProgressChange =
                                onRememberLongFormPlaybackProgressChange,
                            keepPlaybackModeState = keepPlaybackModeState,
                            onKeepPlaybackModeStateChange = onKeepPlaybackModeStateChange,
                            neteaseAutoSourceSwitch = neteaseAutoSourceSwitch,
                            onNeteaseAutoSourceSwitchChange = onNeteaseAutoSourceSwitchChange,
                            neteaseLocalSourceFallback = neteaseLocalSourceFallback,
                            onNeteaseLocalSourceFallbackChange = onNeteaseLocalSourceFallbackChange,
                            stopOnBluetoothDisconnect = stopOnBluetoothDisconnect,
                            onStopOnBluetoothDisconnectChange = onStopOnBluetoothDisconnectChange,
                            usbExclusivePlayback = usbExclusivePlayback,
                            onUsbExclusivePlaybackChange = onUsbExclusivePlaybackChange,
                            allowMixedPlayback = allowMixedPlayback,
                            onAllowMixedPlaybackChange = onAllowMixedPlaybackChange,
                            preemptAudioFocus = preemptAudioFocus,
                            onPreemptAudioFocusChange = onPreemptAudioFocusChange,
                            onNavigateToDownloadManager = {
                                requestScreen(SettingsScreenState.DownloadManager)
                            },
                            onNavigateToHomeSettings = {
                                requestScreen(SettingsScreenState.HomeSettings)
                            },
                            onNavigateToTabOrderSettings = {
                                requestScreen(SettingsScreenState.TabOrderSettings)
                            },
                            maxCacheSizeBytes = maxCacheSizeBytes,
                            onMaxCacheSizeBytesChange = onMaxCacheSizeBytesChange,
                            onClearCacheClick = onClearCacheClick,
                            onBeforeLanguageRestart = onBeforeLanguageRestart,
                            onLanguageChanged = onLanguageChanged
                                )
                            }

                            SettingsScreenState.DownloadManager -> {
                                DownloadManagerScreen(
                                    onBack = { requestScreen(SettingsScreenState.Settings) },
                                    onOpenDownloadProgress = {
                                        requestScreen(SettingsScreenState.DownloadProgress)
                                    },
                                    listState = downloadManagerListState
                                )
                            }

                            SettingsScreenState.DownloadProgress -> {
                                DownloadProgressScreen(
                                    onBack = {
                                        requestScreen(SettingsScreenState.DownloadManager)
                                    },
                                    listState = downloadProgressListState
                                )
                            }

                            SettingsScreenState.HomeSettings -> {
                                HomeSettingsScreen(
                                    onBack = { requestScreen(SettingsScreenState.Settings) }
                                )
                            }

                            SettingsScreenState.TabOrderSettings -> {
                                TabOrderSettingsScreen(
                                    onBack = { requestScreen(SettingsScreenState.Settings) }
                                )
                            }
```


## `app/src/main/java/moe/ouom/neriplayer/ui/screen/tab/library/LibraryScreen.kt`
### LibraryScreen.kt conflict 1

```kotlin
    LOCAL(R.string.library_tab_local),
    FAVORITE(R.string.library_tab_favorite),
    YTMUSIC(R.string.library_tab_youtube_music),
    NETEASE(R.string.library_tab_netease),
    NETEASEALBUM(R.string.library_tab_netease_album),
    BILI(R.string.library_tab_bilibili),
    QQMUSIC(R.string.library_tab_qqmusic),
    KUGOU(R.string.library_tab_kugou)
}

private const val NETEASE_CATEGORY_PLAYLIST = 0
private const val NETEASE_CATEGORY_ALBUM = 1
private const val FAVORITE_CATEGORY_PLAYLIST = 0
private const val FAVORITE_CATEGORY_ARTIST = 1
private const val FAVORITE_CATEGORY_HOT = 2
private const val LOCAL_CATEGORY_PLAYLIST = 0
private const val LOCAL_CATEGORY_ARTIST = 1
private const val LIBRARY_UI_PREFS = "library_ui_preferences"
private const val KEY_LOCAL_ARTIST_SORT_MODE = "local_artist_sort_mode"
private val LibraryPrimaryTabShape = RoundedCornerShape(20.dp)
private val LibrarySearchFieldShape = RoundedCornerShape(16.dp)

private val HotPlaylistPeriods = listOf(
```

### LibraryScreen.kt conflict 2

```kotlin
}

@Composable
private fun LibraryMainTabs(
    tabs: List<LibraryTab>,
    selectedTabIndex: Int,
    refreshEnabled: Boolean,
    onTabSelected: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.ScreenTopTab,
            modifier = Modifier
                .weight(1f)
                .clip(LibraryPrimaryTabShape),
            shape = LibraryPrimaryTabShape
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { onTabSelected(index) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(stringResource(tab.labelResId)) }
                    )
                }
            }
        }

        HapticIconButton(
            onClick = onRefresh,
            enabled = refreshEnabled
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.action_refresh)
            )
        }
    }
}

@Composable
private fun YouTubeMusicPlaylistList(
    playlists: List<YouTubeMusicPlaylist>,
    error: String?,
    listState: LazyListState,
    onClick: (YouTubeMusicPlaylist) -> Unit,
    onRetry: () -> Unit,
    offlineMode: Boolean
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val context = LocalContext.current
    val composeResources = LocalResources.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val favorites by favoriteRepo.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var menuPlaylist by remember { mutableStateOf<YouTubeMusicPlaylist?>(null) }

    fun copyToClipboard(label: String, text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
        AppFeedback.show(
            context = context,
            message = composeResources.getString(R.string.toast_copied)
        )
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 8.dp + miniPlayerHeight
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val cardShape = RoundedCornerShape(12.dp)
        if (playlists.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = error ?: stringResource(R.string.library_youtube_music_empty),
                                color = if (error != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color.Unspecified
                                }
                            )
                        },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.library_youtube_music_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (error != null) {
                                    HapticTextButton(onClick = onRetry) {
                                        Text(text = stringResource(R.string.action_retry))
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_youtube),
                                contentDescription = stringResource(R.string.common_youtube),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        }
        items(
            items = playlists,
            key = { it.browseId }
        ) { playlist ->
            val playlistFavoriteId = remember(playlist.playlistId, playlist.browseId) {
                playlist.favoriteId()
            }
            val isFavorite = remember(favorites, playlistFavoriteId) {
                favoriteRepo.isFavorite(playlistFavoriteId, "youtubeMusic")
            }
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .animateItem()
                    .clip(cardShape)
                    .combinedClickable(
                        onClick = { onClick(playlist) },
                        onLongClick = { menuPlaylist = playlist }
                    )
            ) {
                ListItem(
                    headlineContent = { Text(playlist.title) },
                    supportingContent = {
                        val trackCountText = playlist.trackCount
                            .takeIf { it > 0 }
                            ?.let { count ->
                                pluralStringResource(
                                    R.plurals.library_song_count,
                                    count,
                                    count
                                )
                            }
                        val subtitleText = playlist.subtitle.ifBlank {
                            stringResource(R.string.library_youtube_music_hint)
                        }
                        Text(
                            text = listOfNotNull(subtitleText, trackCountText)
                                .distinct()
                                .joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    leadingContent = {
                        if (playlist.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = offlineCachedImageRequest(
                                    context = context,
                                    data = playlist.coverUrl,
                                    sizePx = 192,
                                    allowHardware = false,
                                    offlineMode = offlineMode
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = menuPlaylist?.browseId == playlist.browseId,
                    onDismissRequest = { menuPlaylist = null }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_youtube_music_open_playlist)) },
                        onClick = {
                            menuPlaylist = null
                            onClick(playlist)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isFavorite) {
                                    stringResource(R.string.home_unfavorite_playlist)
                                } else {
                                    stringResource(R.string.home_favorite_playlist)
                                }
                            )
                        },
                        onClick = {
                            menuPlaylist = null
                            val toastMessage = if (isFavorite) {
                                composeResources.getString(R.string.home_unfavorited)
                            } else {
                                composeResources.getString(R.string.favorite_success)
                            }
                            scope.launch {
                                if (isFavorite) {
                                    favoriteRepo.removeFavorite(playlistFavoriteId, "youtubeMusic")
                                } else {
                                    favoriteRepo.addFavorite(
                                        id = playlistFavoriteId,
                                        name = playlist.title,
                                        coverUrl = playlist.coverUrl,
                                        trackCount = playlist.trackCount,
                                        source = "youtubeMusic",
                                        browseId = playlist.browseId,
                                        playlistId = playlist.playlistId,
                                        subtitle = playlist.subtitle,
                                        songs = emptyList()
                                    )
                                }
                                AppFeedback.show(
                                    context = context,
                                    message = toastMessage
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_youtube_music_copy_browse_id)) },
                        onClick = {
                            copyToClipboard("ytmusic_browse_id", playlist.browseId)
                            menuPlaylist = null
                        }
                    )
                    if (playlist.playlistId.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.library_youtube_music_copy_playlist_id)) },
                            onClick = {
                                copyToClipboard("ytmusic_playlist_id", playlist.playlistId)
                                menuPlaylist = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BiliPlaylistList(
    playlists: List<BiliPlaylist>,
    error: String?,
    listState: LazyListState,
    onClick: (BiliPlaylist) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val createdLabel = stringResource(R.string.library_bili_created_favorite)
    val collectedLabel = stringResource(R.string.library_bili_collected_favorite)
    val collectionLabel = stringResource(R.string.library_bili_collection)
    val seriesLabel = stringResource(R.string.library_bili_series)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredPlaylists = remember(
        playlists,
        searchQuery,
        createdLabel,
        collectedLabel,
        collectionLabel,
        seriesLabel
    ) {
        filterBiliPlaylists(
            playlists = playlists,
            query = searchQuery,
            createdLabel = createdLabel,
            collectedLabel = collectedLabel,
            collectionLabel = collectionLabel,
            seriesLabel = seriesLabel
        )
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + miniPlayerHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val cardShape = RoundedCornerShape(12.dp)
        item(key = "bili_playlist_search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.library_bili_search_hint)) },
                singleLine = true,
                shape = LibrarySearchFieldShape
            )
        }
        if (playlists.isNotEmpty() && filteredPlaylists.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.library_bili_search_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.library_bili_search_empty_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        } else if (filteredPlaylists.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = error ?: stringResource(R.string.library_bili_empty),
                                color = if (error != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color.Unspecified
                                }
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.library_bili_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        }
        items(
            items = filteredPlaylists,
            key = { "${it.kind}:${it.mediaId}" }
        ) { pl ->
            val kindLabel = when (pl.kind) {
                BiliPlaylistKind.CREATED_FAVORITE -> stringResource(R.string.library_bili_created_favorite)
                BiliPlaylistKind.COLLECTED_FAVORITE -> stringResource(R.string.library_bili_collected_favorite)
                BiliPlaylistKind.COLLECTION -> stringResource(R.string.library_bili_collection)
                BiliPlaylistKind.SERIES -> stringResource(R.string.library_bili_series)
            }
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .animateItem()
                    .clip(cardShape)
                    .clickable { onClick(pl) }
            ) {
                ListItem(
                    headlineContent = { Text(pl.title) },
                    supportingContent = {
                        val countText = pluralStringResource(R.plurals.library_video_count, pl.count, pl.count)
                        Text(
                            listOf(kindLabel, pl.subtitle, countText)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    leadingContent = {
                        if (pl.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = offlineCachedImageRequest(
                                    context = context,
                                    data = pl.coverUrl,
                                    sizePx = 192,
                                    allowHardware = false,
                                    offlineMode = offlineMode
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LocalPlaylistList(
    playlists: List<LocalPlaylist>,
    listState: LazyListState,
    onCreate: (String) -> Unit,
    onClick: (LocalPlaylist) -> Unit,
    onArtistClick: (LocalArtistSummary) -> Unit,
    onRename: (Long, String) -> Unit = { _, _ -> },
    onDelete: (List<Long>) -> Unit = {},
    onReorder: (List<Long>) -> Unit = {},
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    var selectedLocalCategory by rememberSaveable {
        mutableIntStateOf(LOCAL_CATEGORY_PLAYLIST)
    }
    var localSearchQuery by rememberSaveable { mutableStateOf("") }
    var localArtistSortMode by rememberSaveable {
        mutableStateOf(readLocalArtistSortMode(context))
    }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val defaultPlaylistName = composeResources.getString(R.string.library_create_playlist_default)
    val maxNameLength = LocalPlaylistRepository.MAX_PLAYLIST_NAME_LENGTH
    val autoShowKeyboard by AppContainer.settingsRepo.autoShowKeyboardFlow.collectAsStateWithLifecycle(
        initialValue = false
    )
    val localArtists = remember(playlists, context) {
        buildLocalArtistSummaries(playlists, context)
    }
    val filteredLocalArtists = remember(localArtists, localSearchQuery) {
        filterLocalArtists(localArtists, localSearchQuery)
    }
    val displayedLocalArtists = remember(filteredLocalArtists, localArtistSortMode) {
        sortLocalArtists(filteredLocalArtists, localArtistSortMode)
    }
    val editablePlaylists = remember(playlists, context) {
        playlists.filterNot { SystemLocalPlaylists.isSystemPlaylist(it, context) }
    }
    val reorderablePlaylists = remember(editablePlaylists) {
        mutableStateListOf<LocalPlaylist>().apply {
            addAll(editablePlaylists)
        }
    }

    LaunchedEffect(showDialog) {
        if (showDialog && autoShowKeyboard) focusRequester.requestFocus()
    }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
        showDeleteSelectedConfirm = false
    }

    fun toggleSelection(playlistId: Long) {
        selectedIds =
            if (selectedIds.contains(playlistId)) selectedIds - playlistId else selectedIds + playlistId
    }

    fun deleteSelected() {
        if (selectedIds.isEmpty()) return
        showDeleteSelectedConfirm = true
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    LaunchedEffect(editablePlaylists) {
        val validIds = editablePlaylists.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(validIds)
        if (selectionMode && editablePlaylists.isEmpty()) {
            exitSelection()
        }
    }

    fun tryCreate(): Boolean {
        val trimmedInput = newName.trim().take(maxNameLength)
        val finalName = trimmedInput.ifBlank { defaultPlaylistName }.take(maxNameLength)

        val favoritesName = composeResources.getString(R.string.favorite_my_music)
        val localFilesName = composeResources.getString(R.string.local_files)
        if (FavoritesPlaylist.matches(finalName, context)) {
            nameError = composeResources.getString(R.string.library_name_reserved, favoritesName)
            return false
        }
        if (LocalFilesPlaylist.matches(finalName, context)) {
            nameError = composeResources.getString(R.string.library_name_reserved, localFilesName)
            return false
        }
        if (playlists.any { it.name.equals(finalName, ignoreCase = true) }) {
            nameError = composeResources.getString(R.string.library_name_exists)
            return false
        }

        onCreate(finalName)
        showDialog = false
        newName = ""
        nameError = null
        return true
    }

    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val favoritesPlaylist = playlists.firstOrNull { FavoritesPlaylist.isSystemPlaylist(it, context) }
    val localFilesPlaylist = playlists.firstOrNull { LocalFilesPlaylist.isSystemPlaylist(it, context) }
    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from: ItemPosition, to: ItemPosition ->
            if (!selectionMode) return@rememberReorderableLazyListState
            val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
            val toId = to.key as? Long ?: return@rememberReorderableLazyListState
            val fromIdx = reorderablePlaylists.indexOfFirst { it.id == fromId }
            val toIdx = reorderablePlaylists.indexOfFirst { it.id == toId }
            if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                reorderablePlaylists.add(toIdx, reorderablePlaylists.removeAt(fromIdx))
            }
        },
        canDragOver = { _, over ->
            selectionMode && over.key is Long
        },
        onDragEnd = { _, _ ->
            if (selectionMode) {
                onReorder(reorderablePlaylists.map { it.id })
            }
        }
    )

    val displayedFavoritesPlaylist = favoritesPlaylist
        ?.takeIf { playlist -> playlist.matchesLocalPlaylistSearch(localSearchQuery, context) }
    val displayedLocalFilesPlaylist = localFilesPlaylist
        ?.takeIf { playlist -> playlist.matchesLocalPlaylistSearch(localSearchQuery, context) }
    val displayedPlaylists = reorderablePlaylists
        .filter { playlist -> playlist.matchesLocalPlaylistSearch(localSearchQuery, context) }
    val hasPlaylistSearchMatches =
        displayedFavoritesPlaylist != null ||
            displayedPlaylists.isNotEmpty() ||
            displayedLocalFilesPlaylist != null
    val windowWidthDp = currentWindowWidthDp()
    val localArtistColumnCount = remember(windowWidthDp) {
        ((windowWidthDp.value - 16f + 10f) / 130f).toInt().coerceAtLeast(1)
    }
    val localArtistRows = remember(displayedLocalArtists, localArtistColumnCount) {
        displayedLocalArtists.chunked(localArtistColumnCount)
    }

    LazyColumn(
        state = reorderState.listState,
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 8.dp + miniPlayerHeight
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderState)
    ) {
        val cardShape = RoundedCornerShape(12.dp)
        item(key = "local_library_header") {
            LocalLibraryHeaderContent(
                selectedLocalCategory = selectedLocalCategory,
                selectionMode = selectionMode,
                searchQuery = localSearchQuery,
                onSearchQueryChange = { localSearchQuery = it },
                artistSortMode = localArtistSortMode,
                onArtistSortModeChange = { sortMode ->
                    localArtistSortMode = sortMode
                    persistLocalArtistSortMode(context, sortMode)
                },
                onPlaylistSelected = {
                    if (selectedLocalCategory != LOCAL_CATEGORY_PLAYLIST) {
                        exitSelection()
                        selectedLocalCategory = LOCAL_CATEGORY_PLAYLIST
                    }
                },
                onArtistSelected = {
                    if (selectedLocalCategory != LOCAL_CATEGORY_ARTIST) {
                        exitSelection()
                        selectedLocalCategory = LOCAL_CATEGORY_ARTIST
                    }
                }
            )
        }

        if (selectedLocalCategory == LOCAL_CATEGORY_ARTIST) {
            if (displayedLocalArtists.isEmpty()) {
                item(key = "local_artist_empty") {
                    LibrarySearchEmptyCard(
                        titleResId = if (localSearchQuery.isBlank()) {
                            R.string.library_local_artist_empty
                        } else {
                            R.string.library_local_search_empty
                        },
                        hintResId = if (localSearchQuery.isBlank()) {
                            R.string.library_local_artist_hint
                        } else {
                            R.string.library_local_search_empty_hint
                        },
                        iconIsArtist = true
                    )
                }
            }
            items(
                items = localArtistRows,
                key = { row -> row.joinToString(separator = "|") { artist -> artist.stableKey } }
            ) { rowArtists ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    rowArtists.forEach { artist ->
                        Box(modifier = Modifier.weight(1f)) {
                            LocalArtistGridCard(
                                artist = artist,
                                onClick = { onArtistClick(artist) },
                                offlineMode = offlineMode
                            )
                        }
                    }
                    repeat(localArtistColumnCount - rowArtists.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
                if (selectionMode) {
                    item(key = "local_playlist_selection_header") {
                        val allSelected =
                            selectedIds.size == displayedPlaylists.size && displayedPlaylists.isNotEmpty()
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                pluralStringResource(
                                    R.plurals.common_selected_count,
                                    selectedIds.size,
                                    selectedIds.size
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            HapticIconButton(onClick = { exitSelection() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_exit_multi_select)
                                )
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HapticTextButton(
                                    onClick = {
                                        selectedIds = if (allSelected) {
                                            emptySet()
                                        } else {
                                            displayedPlaylists.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        if (allSelected) {
                                            stringResource(R.string.action_deselect_all)
                                        } else {
                                            stringResource(R.string.action_select_all)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                HapticTextButton(
                                    enabled = selectedIds.isNotEmpty(),
                                    onClick = { deleteSelected() }
                                ) {
                                    Text(stringResource(R.string.common_delete_selected))
                                }
                            }
                        }
                    )
                }
            }
        }
        if (localSearchQuery.isBlank()) {
            item(key = "local_playlist_create") {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .animateItem()
                    .clip(cardShape)
                    .clickable(enabled = !selectionMode) { showDialog = true }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.library_create_new)) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }

            if (showDialog) {
                MiuixSettingsDialog(
                    onDismissRequest = {
                        showDialog = false
                        newName = ""
                        nameError = null
                    },
                    title = { Text(stringResource(R.string.playlist_create)) },
                    text = {
                        MiuixSettingsDialogContent(verticalSpacing = 12.dp) {
                            MiuixSettingsTextField(
                                value = newName,
                                onValueChange = {
                                    newName = it.take(maxNameLength)
                                    if (nameError != null) nameError = null
                                },
                                placeholder = { Text(stringResource(R.string.playlist_enter_name)) },
                                singleLine = true,
                                modifier = Modifier.focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { tryCreate() })
                            )
                            if (nameError != null) {
                                Text(
                                    text = nameError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        MiuixSettingsButton(
                            onClick = { tryCreate() },
                            enabled = newName.trim().isNotBlank()
                        ) {
                            Text(stringResource(R.string.action_create))
                        }
                    },
                    dismissButton = {
                        MiuixSettingsTextButton(
                            onClick = {
                                showDialog = false
                                newName = ""
                                nameError = null
                            }
                        ) { Text(stringResource(R.string.action_cancel)) }
                    }
                )
            }

            if (showDeleteSelectedConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteSelectedConfirm = false },
                    title = { Text(stringResource(R.string.dialog_confirm_delete)) },
                    text = {
                        Text(
                            pluralStringResource(
                                R.plurals.library_delete_selected_confirm,
                                selectedIds.size,
                                selectedIds.size
                            )
                        )
                    },
                    confirmButton = {
                        HapticTextButton(
                            onClick = {
                                val idsToDelete = selectedIds.toList()
                                exitSelection()
                                onDelete(idsToDelete)
                            }
                        ) { Text(stringResource(R.string.action_delete)) }
                    },
                    dismissButton = {
                        HapticTextButton(
                            onClick = { showDeleteSelectedConfirm = false }
                        ) { Text(stringResource(R.string.action_cancel)) }
                    }
                )
            }
            }
        }

        if (localSearchQuery.isNotBlank() && !hasPlaylistSearchMatches) {
            item(key = "local_playlist_search_empty") {
                LibrarySearchEmptyCard(
                    titleResId = R.string.library_local_search_empty,
                    hintResId = R.string.library_local_search_empty_hint,
                    iconIsArtist = false
                )
            }
        }

        displayedFavoritesPlaylist?.let { system ->
            item(key = "local_playlist_favorites") {
                val displayName = SystemLocalPlaylists.resolve(system.id, system.name, context)?.currentName ?: system.name
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                        .combinedClickable(
                            onClick = {
                                if (!selectionMode) onClick(system)
                            }
                        )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                pluralStringResource(R.plurals.library_song_count, system.songs.size, system.songs.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Spacer(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val cover = rememberPlaylistDisplayCoverUrl(
                                    playlist = system,
                                    resolveLocalFallback = true
                                )
                                if (!cover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = fastScrollableImageRequest(
                                            context = context,
                                            data = cover,
                                            sizePx = 160,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

            items(
                items = displayedPlaylists,
                key = { it.id }
            ) { pl ->
            ReorderableItem(state = reorderState, key = pl.id) { _ ->
                val systemPlaylist = SystemLocalPlaylists.resolve(pl.id, pl.name, context)
                val displayName = systemPlaylist?.currentName ?: pl.name
                val isSystemPlaylist = systemPlaylist != null
                val isSelected = selectionMode && selectedIds.contains(pl.id)
                val rowContainerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                }

                var showMenu by remember { mutableStateOf(false) }
                var showRenameDialog by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var renameText by remember { mutableStateOf(pl.name.take(maxNameLength)) }

                if (selectionMode && showMenu) showMenu = false

                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = rowContainerColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .animateItem()
                        .clip(cardShape)
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    toggleSelection(pl.id)
                                } else {
                                    onClick(pl)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode && !isSystemPlaylist) {
                                    selectionMode = true
                                    selectedIds = setOf(pl.id)
                                }
                            }
                        )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                pluralStringResource(R.plurals.library_song_count, pl.songs.size, pl.songs.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (!isSystemPlaylist) toggleSelection(pl.id)
                                        },
                                        enabled = !isSystemPlaylist
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val cover = rememberPlaylistDisplayCoverUrl(
                                    playlist = pl,
                                    resolveLocalFallback = true
                                )
                                if (!cover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = fastScrollableImageRequest(
                                            context = context,
                                            data = cover,
                                            sizePx = 160,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            if (selectionMode && !isSystemPlaylist) {
                                Box(
                                    modifier = Modifier
                                        .detectReorder(reorderState)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DragHandle,
                                        contentDescription = stringResource(R.string.common_drag_handle),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else if (!selectionMode && !isSystemPlaylist) {
                                Box {
                                    HapticIconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = stringResource(R.string.common_more_options)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_rename)) },
                                            onClick = {
                                                showMenu = false
                                                renameText = pl.name.take(maxNameLength)
                                                showRenameDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_delete)) },
                                            onClick = {
                                                showMenu = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                if (showRenameDialog) {
                    MiuixSettingsDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text(stringResource(R.string.action_rename)) },
                        text = {
                            MiuixSettingsDialogContent(verticalSpacing = 12.dp) {
                                MiuixSettingsTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it.take(maxNameLength) },
                                    placeholder = { Text(displayName) },
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            MiuixSettingsButton(
                                onClick = {
                                    val trimmed = renameText.trim().take(maxNameLength)
                                    if (trimmed.isNotBlank()) {
                                        onRename(pl.id, trimmed)
                                        showRenameDialog = false
                                    }
                                },
                                enabled = renameText.trim().isNotBlank()
                            ) { Text(stringResource(R.string.action_confirm)) }
                        },
                        dismissButton = {
                            MiuixSettingsTextButton(
                                onClick = { showRenameDialog = false }
                            ) { Text(stringResource(R.string.action_cancel)) }
                        }
                    )
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text(stringResource(R.string.action_delete)) },
                        text = {
                            Text(stringResource(R.string.library_delete_playlist_confirm, displayName))
                        },
                        confirmButton = {
                            HapticTextButton(
                                onClick = {
                                    val playlistId = pl.id
                                    showDeleteDialog = false
                                    onDelete(listOf(playlistId))
                                }
                            ) { Text(stringResource(R.string.action_delete)) }
                        },
                        dismissButton = {
                            HapticTextButton(
                                onClick = { showDeleteDialog = false }
                            ) { Text(stringResource(R.string.action_cancel)) }
                        }
                    )
                }
            }
        }

        displayedLocalFilesPlaylist?.let { system ->
            item(key = "local_playlist_local_files") {
                val displayName = SystemLocalPlaylists.resolve(system.id, system.name, context)?.currentName ?: system.name
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                        .combinedClickable(
                            onClick = {
                                if (!selectionMode) onClick(system)
                            }
                        )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                pluralStringResource(R.plurals.library_song_count, system.songs.size, system.songs.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Spacer(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val cover = rememberPlaylistDisplayCoverUrl(
                                    playlist = system,
                                    resolveLocalFallback = true
                                )
                                if (!cover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = fastScrollableImageRequest(
                                            context = context,
                                            data = cover,
                                            sizePx = 160,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun LocalLibraryHeaderContent(
    selectedLocalCategory: Int,
    selectionMode: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    artistSortMode: LocalArtistSortMode,
    onArtistSortModeChange: (LocalArtistSortMode) -> Unit,
    onPlaylistSelected: () -> Unit,
    onArtistSelected: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LocalCategoryTabs(
            selectedCategory = selectedLocalCategory,
            onPlaylistSelected = onPlaylistSelected,
            onArtistSelected = onArtistSelected
        )
        if (!selectionMode) {
            if (selectedLocalCategory == LOCAL_CATEGORY_ARTIST) {
                LocalArtistSearchAndSortRow(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    sortMode = artistSortMode,
                    onSortModeChange = onArtistSortModeChange
                )
            } else {
                LibraryInlineSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholderResId = R.string.library_local_playlist_search_hint
                )
            }
        }
    }
}

@Composable
private fun LocalArtistSearchAndSortRow(
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: LocalArtistSortMode,
    onSortModeChange: (LocalArtistSortMode) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.library_local_artist_search_hint)) },
            singleLine = true,
            shape = LibrarySearchFieldShape,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    HapticIconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_clear)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Box {
            HapticIconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.library_local_artist_sort)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.SONG_COUNT,
                    text = stringResource(R.string.library_local_artist_sort_count),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.SONG_COUNT)
                        menuExpanded = false
                    }
                )
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.RECENT_ADDED,
                    text = stringResource(R.string.library_local_artist_sort_recent),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.RECENT_ADDED)
                        menuExpanded = false
                    }
                )
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.NAME,
                    text = stringResource(R.string.library_local_artist_sort_name),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.NAME)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LocalArtistSortMenuItem(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text)
        },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.common_selected)
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        },
        onClick = onClick
    )
}

@Composable
private fun LocalCategoryTabs(
    selectedCategory: Int,
    onPlaylistSelected: () -> Unit,
    onArtistSelected: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.ScreenTopTab,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            tintColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedCategory,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedCategory == LOCAL_CATEGORY_PLAYLIST,
                    onClick = onPlaylistSelected,
                    text = { Text(stringResource(R.string.library_favorite_tab_playlists)) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null
                        )
                    }
                )
                Tab(
                    selected = selectedCategory == LOCAL_CATEGORY_ARTIST,
                    onClick = onArtistSelected,
                    text = { Text(stringResource(R.string.library_favorite_tab_artists)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryInlineSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderResId: Int
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text(stringResource(placeholderResId)) },
        singleLine = true,
        shape = LibrarySearchFieldShape,
        trailingIcon = {
            if (query.isNotEmpty()) {
                HapticIconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_clear)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun LibrarySearchEmptyCard(
    titleResId: Int,
    hintResId: Int,
    iconIsArtist: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        ListItem(
            headlineContent = { Text(stringResource(titleResId)) },
            supportingContent = {
                Text(
                    stringResource(hintResId),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Icon(
                    imageVector = if (iconIsArtist) {
                        Icons.Filled.AccountCircle
                    } else {
                        Icons.AutoMirrored.Filled.QueueMusic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
            }
        )
    }
}

private fun filterLocalArtists(
    artists: List<LocalArtistSummary>,
    query: String
): List<LocalArtistSummary> {
    if (query.isBlank()) return artists
    return artists.filter { artist -> artist.matchesLocalArtistSearch(query) }
}

internal fun sortLocalArtists(
    artists: List<LocalArtistSummary>,
    sortMode: LocalArtistSortMode
): List<LocalArtistSummary> {
    return when (sortMode) {
        LocalArtistSortMode.SONG_COUNT -> artists.sortedWith(
            compareByDescending<LocalArtistSummary> { artist ->
                artist.songs.size
            }.thenBy { artist ->
                artist.name.lowercase()
            }
        )
        LocalArtistSortMode.RECENT_ADDED -> artists.sortedWith(
            compareByDescending<LocalArtistSummary> { artist ->
                artist.coverSong?.addedAt ?: 0L
            }.thenBy { artist ->
                artist.name.lowercase()
            }
        )
        LocalArtistSortMode.NAME -> artists.sortedBy { artist ->
            artist.name.lowercase()
        }
    }
}

private fun LocalArtistSummary.matchesLocalArtistSearch(query: String): Boolean {
    return queryMatches(query, name) ||
        songs.any { song ->
            queryMatches(
                query,
                song.displayName(),
                song.displayArtist(),
                song.album,
                song.localFileName
            )
        }
}

private fun LocalPlaylist.matchesLocalPlaylistSearch(query: String, context: Context): Boolean {
    if (query.isBlank()) return true
    val displayName = SystemLocalPlaylists.resolve(id, name, context)?.currentName ?: name
    return queryMatches(query, id, name, displayName, songs.size) ||
        songs.any { song ->
            queryMatches(
                query,
                song.displayName(),
                song.displayArtist(),
                song.album,
                song.localFileName
            )
        }
}

private fun filterFavoritePlaylists(
    favorites: List<FavoritePlaylist>,
    query: String
): List<FavoritePlaylist> {
    if (query.isBlank()) return favorites
    return favorites.filter { favorite -> favorite.matchesFavoriteSearch(query) }
}

private fun FavoritePlaylist.matchesFavoriteSearch(query: String): Boolean {
    return queryMatches(
        query,
        id,
        name,
        subtitle,
        source,
        browseId,
        playlistId,
        trackCount,
        favoriteSourceSearchAliases(source)
    ) || songs.any { song ->
        queryMatches(
            query,
            song.displayName(),
            song.displayArtist(),
            song.album,
            song.localFileName
        )
    }
}

private fun favoriteSourceSearchAliases(source: String): List<String> {
    return when (source) {
        "youtubeMusic" -> listOf("YouTube", "YouTube Music")
        "neteaseAlbum" -> listOf("Netease Album", "网易云专辑", "专辑")
        "netease" -> listOf("Netease", "网易云", "歌单")
        "bili" -> listOf("Bilibili", "哔哩哔哩", "B站")
        FAVORITE_SOURCE_NETEASE_ARTIST -> listOf("Artist", "歌手")
        else -> listOf(source)
    }
}

private fun queryMatches(query: String, vararg values: Any?): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return true
    return values.any { value ->
        when (value) {
            null -> false
            is Iterable<*> -> value.any { item ->
                item?.toString()?.contains(normalizedQuery, ignoreCase = true) == true
            }
            else -> value.toString().contains(normalizedQuery, ignoreCase = true)
        }
    }
}

@Composable
private fun NeteaseLibraryList(
    playlists: List<PlaylistSummary>,
    albums: List<AlbumSummary>,
    playlistListState: LazyListState,
    albumListState: LazyListState,
    selectedCategory: Int,
    onCategoryChange: (Int) -> Unit,
    onPlaylistClick: (PlaylistSummary) -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
    offlineMode: Boolean
) {
    var playlistSearchQuery by rememberSaveable { mutableStateOf("") }
    var albumSearchQuery by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val isAlbumCategory = selectedCategory == NETEASE_CATEGORY_ALBUM
    val listState = if (isAlbumCategory) albumListState else playlistListState
    val searchQuery = if (isAlbumCategory) albumSearchQuery else playlistSearchQuery
    val filteredPlaylists = remember(playlists, playlistSearchQuery) {
        filterNeteasePlaylists(playlists, playlistSearchQuery)
    }
    val filteredAlbums = remember(albums, albumSearchQuery) {
        filterNeteaseAlbums(albums, albumSearchQuery)
    }

    fun updateSearchQuery(category: Int, value: String) {
        if (category == NETEASE_CATEGORY_ALBUM) {
            albumSearchQuery = value
        } else {
            playlistSearchQuery = value
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 8.dp + miniPlayerHeight
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val cardShape = RoundedCornerShape(12.dp)
        item(key = "netease_library_header") {
            NeteaseLibraryHeaderContent(
                selectedCategory = selectedCategory,
                onCategoryChange = { category ->
                    if (selectedCategory != category) {
                        onCategoryChange(category)
                    }
                }
            )
            LibraryInlineSearchField(
                query = searchQuery,
                onQueryChange = { value ->
                    updateSearchQuery(selectedCategory, value)
                },
                placeholderResId = R.string.library_netease_search_hint
            )
        }

        if (isAlbumCategory) {
            if (albums.isNotEmpty() && filteredAlbums.isEmpty()) {
                item(key = "netease_album_search_empty") {
                    NeteaseLibraryEmptyCard(
                        cardShape = cardShape,
                        title = stringResource(R.string.library_netease_search_empty),
                        hint = stringResource(R.string.library_netease_search_empty_hint),
                        iconIsAlbum = true
                    )
                }
            } else if (filteredAlbums.isEmpty()) {
                item(key = "netease_album_empty") {
                    NeteaseLibraryEmptyCard(
                        cardShape = cardShape,
                        title = stringResource(R.string.library_netease_album_empty),
                        hint = stringResource(R.string.library_netease_search_hint),
                        iconIsAlbum = true
                    )
                }
            }
            items(
                items = filteredAlbums,
                key = { album -> "album:${album.id}" }
            ) { album ->
                NeteaseAlbumRow(
                    album = album,
                    cardShape = cardShape,
                    onClick = { onAlbumClick(album) },
                    offlineMode = offlineMode
                )
            }
        } else {
            if (playlists.isNotEmpty() && filteredPlaylists.isEmpty()) {
                item(key = "netease_playlist_search_empty") {
                    NeteaseLibraryEmptyCard(
                        cardShape = cardShape,
                        title = stringResource(R.string.library_netease_search_empty),
                        hint = stringResource(R.string.library_netease_search_empty_hint),
                        iconIsAlbum = false
                    )
                }
            } else if (filteredPlaylists.isEmpty()) {
                item(key = "netease_playlist_empty") {
                    NeteaseLibraryEmptyCard(
                        cardShape = cardShape,
                        title = stringResource(R.string.library_netease_playlist_empty),
                        hint = stringResource(R.string.library_netease_search_hint),
                        iconIsAlbum = false
                    )
                }
            }
            items(
                items = filteredPlaylists,
                key = { playlist -> "playlist:${playlist.id}" }
            ) { playlist ->
                NeteasePlaylistRow(
                    playlist = playlist,
                    cardShape = cardShape,
                    context = context,
                    onClick = { onPlaylistClick(playlist) },
                    offlineMode = offlineMode
                )
            }
        }
    }
}

@Composable
private fun NeteaseLibraryHeaderContent(
    selectedCategory: Int,
    onCategoryChange: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        NeteaseCategoryTabs(
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange
        )
    }
}

@Composable
private fun NeteaseCategoryTabs(
    selectedCategory: Int,
    onCategoryChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.ScreenTopTab,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            tintColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedCategory,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedCategory == NETEASE_CATEGORY_PLAYLIST,
                    onClick = { onCategoryChange(NETEASE_CATEGORY_PLAYLIST) },
                    text = { Text(stringResource(R.string.library_netease_tab_playlists)) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null
                        )
                    }
                )
                Tab(
                    selected = selectedCategory == NETEASE_CATEGORY_ALBUM,
                    onClick = { onCategoryChange(NETEASE_CATEGORY_ALBUM) },
                    text = { Text(stringResource(R.string.library_netease_tab_albums)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Album,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun NeteaseLibraryEmptyCard(
    cardShape: RoundedCornerShape,
    title: String,
    hint: String,
    iconIsAlbum: Boolean
) {
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(cardShape)
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = {
                Text(
                    text = hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Icon(
                    imageVector = if (iconIsAlbum) {
                        Icons.Filled.Album
                    } else {
                        Icons.AutoMirrored.Filled.QueueMusic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
            }
        )
    }
}

@Composable
private fun NeteasePlaylistRow(
    playlist: PlaylistSummary,
    cardShape: RoundedCornerShape,
    context: Context,
    onClick: () -> Unit,
    offlineMode: Boolean
) {
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(cardShape)
            .clickable(onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(playlist.name) },
            supportingContent = {
                Text(
                    text = stringResource(
                        R.string.home_play_count_format,
                        formatPlayCount(context, playlist.playCount),
                        playlist.trackCount
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                AsyncImage(
                    model = offlineCachedImageRequest(
                        context = context,
                        data = playlist.picUrl,
                        sizePx = 192,
                        allowHardware = false,
                        offlineMode = offlineMode
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        )
    }
}

@Composable
private fun NeteaseAlbumRow(
    album: AlbumSummary,
    cardShape: RoundedCornerShape,
    onClick: () -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(cardShape)
            .clickable(onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(album.name) },
            supportingContent = {
                Text(
                    text = pluralStringResource(
                        R.plurals.library_song_count,
                        album.size,
                        album.size
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                AsyncImage(
                    model = offlineCachedImageRequest(
                        context = context,
                        data = album.picUrl,
                        sizePx = 192,
                        allowHardware = false,
                        offlineMode = offlineMode
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        )
    }
}

@Composable
private fun NeteasePlaylistList(
    playlists: List<PlaylistSummary>,
    listState: LazyListState,
    selectedCategory: Int,
    onCategoryChange: (Int) -> Unit,
    onClick: (PlaylistSummary) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredPlaylists = remember(playlists, searchQuery) {
        filterNeteasePlaylists(playlists, searchQuery)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + miniPlayerHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val cardShape = RoundedCornerShape(12.dp)
        item(key = "netease_library_header") {
            NeteaseLibraryHeaderContent(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange
            )
        }
        item(key = "netease_playlist_search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.library_netease_search_hint)) },
                singleLine = true,
                shape = LibrarySearchFieldShape
            )
        }
        if (playlists.isNotEmpty() && filteredPlaylists.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.library_netease_search_empty))
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.library_netease_search_empty_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        } else if (filteredPlaylists.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.library_netease_playlist_empty))
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.library_netease_search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        }
        items(
            items = filteredPlaylists,
            key = { it.id }
        ) { pl ->
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .animateItem()
                    .clip(cardShape)
                    .clickable { onClick(pl) }
            ) {
                ListItem(
                    headlineContent = { Text(pl.name) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.home_play_count_format,
                                formatPlayCount(context, pl.playCount),
                                pl.trackCount
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    leadingContent = {
                        AsyncImage(
                            model = offlineCachedImageRequest(
                                context = context,
                                data = pl.picUrl,
                                sizePx = 192,
                                allowHardware = false,
                                offlineMode = offlineMode
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun NeteaseAlbumList(
    playlists: List<AlbumSummary>,
    listState: LazyListState,
    selectedCategory: Int,
    onCategoryChange: (Int) -> Unit,
    onClick: (AlbumSummary) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredAlbums = remember(playlists, searchQuery) {
        filterNeteaseAlbums(playlists, searchQuery)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + miniPlayerHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val cardShape = RoundedCornerShape(12.dp)
        item(key = "netease_library_header") {
            NeteaseLibraryHeaderContent(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange
            )
        }
        item(key = "netease_album_search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.library_netease_search_hint)) },
                singleLine = true,
                shape = LibrarySearchFieldShape
            )
        }
        if (playlists.isNotEmpty() && filteredAlbums.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.library_netease_search_empty))
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.library_netease_search_empty_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        } else if (filteredAlbums.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.library_netease_album_empty))
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.library_netease_search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        }
        items(
            items = filteredAlbums,
            key = { it.id }
        ) { pl ->
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .animateItem()
                    .clip(cardShape)
                    .clickable { onClick(pl) }
            ) {
                ListItem(
                    headlineContent = { Text(pl.name) },
                    supportingContent = {
                        Text(
                            pluralStringResource(R.plurals.library_song_count, pl.size, pl.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    leadingContent = {
                        AsyncImage(
                            model = offlineCachedImageRequest(
                                context = context,
                                data = pl.picUrl,
                                sizePx = 192,
                                allowHardware = false,
                                offlineMode = offlineMode
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FavoritePlaylistList(
    listState: LazyListState,
    onHotPlaylistClick: (PlaybackStatsPeriod) -> Unit,
    onNeteasePlaylistClick: (PlaylistSummary) -> Unit,
    onNeteaseAlbumClick: (AlbumSummary) -> Unit,
    onNeteaseArtistClick: (NeteaseArtistSummary) -> Unit,
    onBiliPlaylistClick: (BiliPlaylist) -> Unit,
    onYouTubeMusicPlaylistClick: (YouTubeMusicPlaylist) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val favorites by favoriteRepo.favorites.collectAsStateWithLifecycle()
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val scope = rememberCoroutineScope()
    var sortMode by rememberSaveable { mutableStateOf(false) }
    var selectedFavoriteCategory by rememberSaveable {
        mutableIntStateOf(FAVORITE_CATEGORY_PLAYLIST)
    }
    var favoriteSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }
    val reorderableFavorites = remember { mutableStateListOf<FavoritePlaylist>() }
    val playlistFavorites = remember(favorites) {
        favorites.filterNot { it.source == FAVORITE_SOURCE_NETEASE_ARTIST }
    }
    val artistFavorites = remember(favorites) {
        favorites.filter { it.source == FAVORITE_SOURCE_NETEASE_ARTIST }
    }
    val isHotCategory = selectedFavoriteCategory == FAVORITE_CATEGORY_HOT
    val visibleFavorites = remember(playlistFavorites, artistFavorites, selectedFavoriteCategory) {
        when (selectedFavoriteCategory) {
            FAVORITE_CATEGORY_ARTIST -> artistFavorites
            FAVORITE_CATEGORY_PLAYLIST -> playlistFavorites
            else -> emptyList()
        }
    }
    val hotPlaylists = if (isHotCategory) rememberHotPlaylists() else null

    fun favoriteKey(favorite: FavoritePlaylist): String {
        return "${favorite.source}:${favorite.id}"
    }

    fun exitEditMode() {
        sortMode = false
        selectedKeys = emptySet()
        showDeleteSelectedConfirm = false
    }

    fun toggleSelection(key: String) {
        selectedKeys = if (selectedKeys.contains(key)) {
            selectedKeys - key
        } else {
            selectedKeys + key
        }
    }

    BackHandler(enabled = sortMode) { exitEditMode() }

    LaunchedEffect(visibleFavorites) {
        reorderableFavorites.clear()
        reorderableFavorites.addAll(visibleFavorites)
        val validKeys = visibleFavorites.map(::favoriteKey).toSet()
        selectedKeys = selectedKeys.intersect(validKeys)
        if (sortMode && visibleFavorites.isEmpty()) {
            exitEditMode()
        }
    }

    LaunchedEffect(sortMode, visibleFavorites) {
        if (sortMode && visibleFavorites.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from: ItemPosition, to: ItemPosition ->
            if (!sortMode) return@rememberReorderableLazyListState
            val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
            val toKey = to.key as? String ?: return@rememberReorderableLazyListState
            val fromIndex = reorderableFavorites.indexOfFirst { favoriteKey(it) == fromKey }
            val toIndex = reorderableFavorites.indexOfFirst { favoriteKey(it) == toKey }
            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                reorderableFavorites.add(toIndex, reorderableFavorites.removeAt(fromIndex))
            }
        },
        canDragOver = { _, over -> sortMode && over.key is String },
        onDragEnd = { _, _ ->
            if (sortMode) {
                scope.launch {
                    favoriteRepo.reorderFavorites(
                        reorderableFavorites.map { "${it.source}:${it.id}" }
                    )
                }
            }
        }
    )

    LazyColumn(
        state = reorderState.listState,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + miniPlayerHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderState)
    ) {
        val cardShape = RoundedCornerShape(12.dp)
        val displayedFavorites = filterFavoritePlaylists(reorderableFavorites, favoriteSearchQuery)
        item(key = "favorite_category_tabs") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                AdvancedGlassSurface(
                    role = AdvancedGlassRole.ScreenTopTab,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    tintColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedFavoriteCategory,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedFavoriteCategory == FAVORITE_CATEGORY_PLAYLIST,
                            onClick = {
                                if (selectedFavoriteCategory != FAVORITE_CATEGORY_PLAYLIST) {
                                    selectedFavoriteCategory = FAVORITE_CATEGORY_PLAYLIST
                                    exitEditMode()
                                }
                            },
                            text = { Text(stringResource(R.string.library_favorite_tab_playlists)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null
                                )
                            }
                        )
                        Tab(
                            selected = selectedFavoriteCategory == FAVORITE_CATEGORY_ARTIST,
                            onClick = {
                                if (selectedFavoriteCategory != FAVORITE_CATEGORY_ARTIST) {
                                    selectedFavoriteCategory = FAVORITE_CATEGORY_ARTIST
                                    exitEditMode()
                                }
                            },
                            text = { Text(stringResource(R.string.library_favorite_tab_artists)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null
                                )
                            }
                        )
                        Tab(
                            selected = selectedFavoriteCategory == FAVORITE_CATEGORY_HOT,
                            onClick = {
                                if (selectedFavoriteCategory != FAVORITE_CATEGORY_HOT) {
                                    selectedFavoriteCategory = FAVORITE_CATEGORY_HOT
                                    exitEditMode()
                                }
                            },
                            text = { Text(stringResource(R.string.library_favorite_tab_hot)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Bolt,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
        if (!sortMode && !isHotCategory) {
            item(key = "favorite_search") {
                LibraryInlineSearchField(
                    query = favoriteSearchQuery,
                    onQueryChange = { favoriteSearchQuery = it },
                    placeholderResId = R.string.library_favorite_search_hint
                )
            }
        }
        if (sortMode && !isHotCategory) {
            item(key = "favorite_sort_mode_header") {
                val allSelected =
                    selectedKeys.size == displayedFavorites.size && displayedFavorites.isNotEmpty()
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                pluralStringResource(
                                    R.plurals.common_selected_count,
                                    selectedKeys.size,
                                    selectedKeys.size
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            HapticIconButton(onClick = { exitEditMode() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_exit_multi_select)
                                )
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HapticTextButton(
                                    onClick = {
                                        selectedKeys = if (allSelected) {
                                            emptySet()
                                        } else {
                                            displayedFavorites.map(::favoriteKey).toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        if (allSelected) {
                                            stringResource(R.string.action_deselect_all)
                                        } else {
                                            stringResource(R.string.action_select_all)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                HapticTextButton(
                                    enabled = selectedKeys.isNotEmpty(),
                                    onClick = { showDeleteSelectedConfirm = true }
                                ) {
                                    Text(stringResource(R.string.common_delete_selected))
                                }
                            }
                        }
                    )
                }
            }
        }
        if (isHotCategory) {
            if (hotPlaylists == null) {
                item(key = "favorite_hot_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(
                    items = hotPlaylists,
                    key = { playlist -> "hot_playlist_${playlist.period.name}" }
                ) { playlist ->
                    val titleResId = hotPlaylistTitleResId(playlist.period)
                    val subtitle = if (playlist.tracks.isEmpty()) {
                        stringResource(R.string.library_hot_empty_hint)
                    } else {
                        stringResource(
                            R.string.library_hot_playlist_summary,
                            playlist.tracks.size,
                            formatPlayCount(context, playlist.totalPlayCount)
                        )
                    }
                    Card(
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .animateItem()
                            .clip(cardShape)
                            .clickable { onHotPlaylistClick(playlist.period) }
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(titleResId))
                            },
                            supportingContent = {
                                Text(
                                    text = subtitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        )
                    }
                }
            }
        } else if (displayedFavorites.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    val isArtistCategory = selectedFavoriteCategory == FAVORITE_CATEGORY_ARTIST
                    val isSearchEmpty = favoriteSearchQuery.isNotBlank() && visibleFavorites.isNotEmpty()
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(
                                    if (isSearchEmpty) {
                                        R.string.library_favorite_search_empty
                                    } else if (isArtistCategory) {
                                        R.string.library_no_favorite_artist
                                    } else {
                                        R.string.playlist_no_favorite
                                    }
                                )
                            )
                        },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (isSearchEmpty) {
                                        R.string.library_favorite_search_empty_hint
                                    } else if (isArtistCategory) {
                                        R.string.library_favorite_artist_hint
                                    } else {
                                        R.string.playlist_favorite_hint
                                    }
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Icon(
                                imageVector = if (isArtistCategory) {
                                    Icons.Filled.AccountCircle
                                } else {
                                    Icons.AutoMirrored.Filled.QueueMusic
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        } else {
            items(
                items = displayedFavorites,
                key = { favoriteKey(it) }
            ) { favorite ->
                val itemKey = favoriteKey(favorite)
                val isSelected = sortMode && selectedKeys.contains(itemKey)
                ReorderableItem(state = reorderState, key = itemKey) {
                    Card(
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                            } else if (sortMode) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .animateItem()
                            .clip(cardShape)
                            .combinedClickable(
                                onClick = {
                                    if (sortMode) {
                                        toggleSelection(itemKey)
                                        return@combinedClickable
                                    }
                                    when (favorite.source) {
                                        "netease" -> {
                                            onNeteasePlaylistClick(
                                                PlaylistSummary(
                                                    id = favorite.id,
                                                    name = favorite.name,
                                                    picUrl = favorite.coverUrl ?: "",
                                                    playCount = 0,
                                                    trackCount = favorite.trackCount
                                                )
                                            )
                                        }
                                        "neteaseAlbum" -> {
                                            onNeteaseAlbumClick(
                                                AlbumSummary(
                                                    id = favorite.id,
                                                    name = favorite.name,
                                                    picUrl = favorite.coverUrl.orEmpty(),
                                                    size = favorite.trackCount
                                                )
                                            )
                                        }
                                        FAVORITE_SOURCE_NETEASE_ARTIST -> {
                                            onNeteaseArtistClick(
                                                NeteaseArtistSummary(
                                                    id = favorite.id,
                                                    name = favorite.name
                                                )
                                            )
                                        }
                                        "youtubeMusic" -> {
                                            val resolvedBrowseId = favorite.browseId
                                                ?.takeIf { it.isNotBlank() }
                                                ?: favorite.playlistId
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?.let { "VL$it" }
                                            val resolvedPlaylistId = favorite.playlistId
                                                ?.takeIf { it.isNotBlank() }
                                                ?: resolvedBrowseId?.removePrefix("VL")
                                            if (
                                                !resolvedBrowseId.isNullOrBlank() &&
                                                !resolvedPlaylistId.isNullOrBlank()
                                            ) {
                                                onYouTubeMusicPlaylistClick(
                                                    YouTubeMusicPlaylist(
                                                        browseId = resolvedBrowseId,
                                                        playlistId = resolvedPlaylistId,
                                                        title = favorite.name,
                                                        subtitle = favorite.subtitle.orEmpty(),
                                                        coverUrl = favorite.coverUrl.orEmpty(),
                                                        trackCount = favorite.trackCount
                                                    )
                                                )
                                            }
                                        }
                                        "bili" -> {
                                            onBiliPlaylistClick(favorite.toBiliPlaylist())
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!sortMode) sortMode = true
                                    toggleSelection(itemKey)
                                }
                            )
                    ) {
                        ListItem(
                            headlineContent = { Text(favorite.name) },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        R.string.library_favorite_source_format,
                                        favorite.trackCount,
                                        favoriteSourceLabel(favorite.source)
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            leadingContent = {
                                if (!favorite.coverUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = offlineCachedImageRequest(
                                            context = context,
                                            data = favorite.coverUrl,
                                            sizePx = 192,
                                            allowHardware = false,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = when (favorite.source) {
                                            FAVORITE_SOURCE_NETEASE_ARTIST -> Icons.Filled.AccountCircle
                                            "neteaseAlbum" -> Icons.Filled.Album
                                            else -> Icons.AutoMirrored.Filled.QueueMusic
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                if (sortMode) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { toggleSelection(itemKey) }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .detectReorder(reorderState)
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DragHandle,
                                                contentDescription = stringResource(R.string.common_drag_handle),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = { Text(stringResource(R.string.dialog_confirm_delete)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.library_delete_selected_confirm,
                        selectedKeys.size,
                        selectedKeys.size
                    )
                )
            },
            confirmButton = {
                HapticTextButton(
                    onClick = {
                        val targets = reorderableFavorites.filter { favoriteKey(it) in selectedKeys }
                        scope.launch {
                            targets.forEach { favoriteRepo.removeFavorite(it.id, it.source) }
                            exitEditMode()
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                HapticTextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun favoriteSourceLabel(source: String): String {
    return when (source) {
        "youtubeMusic" -> "YouTube"
        "neteaseAlbum" -> "Netease Album"
        "netease" -> "Netease"
        "bili" -> "Bilibili"
        FAVORITE_SOURCE_NETEASE_ARTIST -> stringResource(R.string.library_favorite_source_artist)
        else -> source
```


## `app/src/main/java/moe/ouom/neriplayer/ui/screen/tab/settings/SettingsScreen.kt`
### SettingsScreen.kt conflict 1

```kotlin
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import moe.ouom.neriplayer.ui.viewmodel.BackupRestoreViewModel
import moe.ouom.neriplayer.ui.viewmodel.ConfigTransferViewModel
import moe.ouom.neriplayer.ui.viewmodel.auth.BiliAuthEvent
import moe.ouom.neriplayer.ui.viewmodel.auth.BiliAuthViewModel
import moe.ouom.neriplayer.ui.viewmodel.auth.YouTubeAuthEvent
import moe.ouom.neriplayer.ui.viewmodel.auth.YouTubeAuthViewModel
import moe.ouom.neriplayer.ui.viewmodel.debug.NeteaseAuthEvent
import moe.ouom.neriplayer.ui.viewmodel.debug.NeteaseAuthViewModel
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private data class PendingDownloadDirectoryChange(
    val previousUri: String?,
    val targetUri: String?,
    val targetSummary: String,
    val releaseTargetPermissionOnCancel: Boolean
) {
    val shouldReleasePreviousPermission: Boolean
        get() = !previousUri.isNullOrBlank() &&
            !ManagedDownloadStorage.areEquivalentDirectoryUris(previousUri, targetUri)
}

private data class PendingSettingsSearchNavigation(
    val page: SettingsPage,
    val targetId: String,
    val requestId: Int
)

private fun isForwardSettingsPageTransition(
    initialPage: SettingsPage?,
    targetPage: SettingsPage?
): Boolean {
    if (targetPage == null) return false
    if (initialPage == null) return true
    if (targetPage.backTargetPage() == initialPage) return true
    if (initialPage.backTargetPage() == targetPage) return false
    return targetPage.ordinal >= initialPage.ordinal
}

@Composable
internal fun SettingsPageHost(
    activePage: SettingsPage?,
    splitLayout: Boolean,
    isolateAdvancedGlassTransitions: Boolean,
    content: @Composable (SettingsPage?) -> Unit
) {
    if (splitLayout) {
        AdvancedGlassScene(active = true) {
            content(activePage)
        }
        return
    }

    AnimatedContent(
        targetState = activePage,
        modifier = Modifier.fillMaxSize(),
        label = "settings_page_switch",
        transitionSpec = {
            isolatedAdvancedGlassHorizontalTransition(
                forward = isForwardSettingsPageTransition(initialState, targetState)
            ).using(SizeTransform(clip = true))
        }
    ) { selectedPage ->
        AdvancedGlassNavigationHandoff(
            enabled = isolateAdvancedGlassTransitions && transition.isRunning
        ) {
            AdvancedGlassScene(
                active = isolateAdvancedGlassTransitions || selectedPage == activePage
            ) {
                content(selectedPage)
            }
        }
    }
}

private fun Context.neteaseQualityLabel(value: String): String {
    return when (value) {
        "standard" -> getString(R.string.settings_audio_quality_standard)
        "higher" -> getString(R.string.settings_audio_quality_higher)
        "exhigh" -> getString(R.string.settings_audio_quality_exhigh)
        "lossless" -> getString(R.string.settings_audio_quality_lossless)
        "hires" -> getString(R.string.quality_hires)
        "jyeffect" -> getString(R.string.settings_audio_quality_jyeffect)
        "sky" -> getString(R.string.settings_audio_quality_sky)
        "jymaster" -> getString(R.string.settings_audio_quality_jymaster)
        else -> value
    }
}

private fun Context.youtubeQualityLabel(value: String): String {
    return when (value) {
        "low" -> getString(R.string.settings_audio_quality_standard)
        "medium" -> getString(R.string.settings_audio_quality_medium)
        "high" -> getString(R.string.settings_audio_quality_high)
        "very_high" -> getString(R.string.quality_very_high)
        else -> value
    }
}

private fun Context.biliQualityLabel(value: String): String {
    return when (value) {
        "dolby" -> getString(R.string.settings_audio_quality_dolby)
        "hires" -> getString(R.string.quality_hires)
        "lossless" -> getString(R.string.settings_audio_quality_lossless)
        "high" -> getString(R.string.settings_audio_quality_high)
        "medium" -> getString(R.string.settings_audio_quality_medium)
        "low" -> getString(R.string.settings_audio_quality_low)
        else -> value
    }
}

private fun Context.kugouQualityLabel(value: String): String {
    return when (value) {
        "128" -> getString(R.string.quality_standard)
        "320" -> getString(R.string.settings_audio_quality_high)
        "flac" -> getString(R.string.quality_lossless)
        "high" -> getString(R.string.quality_hires)
        "viper_tape" -> getString(R.string.quality_viper_tape)
        else -> value
    }
}

@Composable
private fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(16.dp)

    AdvancedGlassSurface(
        role = AdvancedGlassRole.SettingsSection,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = shape,
        fallbackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.62f),
        tintColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
                            Text(
                                text = stringResource(R.string.settings_search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSearchResultsCard(
    query: String,
    results: List<SettingsSearchEntry>,
    onResultClick: (SettingsSearchEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (query.isBlank()) {
        return
    }

    MiuixSettingsSectionCard(modifier = modifier) {
        if (results.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_search_empty),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            results.forEach { entry ->
                SettingsSearchResultRow(
                    entry = entry,
                    onClick = { onResultClick(entry) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSearchResultRow(
    entry: SettingsSearchEntry,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.settingsItemClickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = entry.page.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(entry.title) },
        supportingContent = {
            Text(stringResource(entry.page.titleRes))
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
```

### SettingsScreen.kt conflict 2

```kotlin
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    themeMode: ThemeMode,
    onThemeToggleRequest: (Offset, Float) -> Unit,
    onThemeModeRequest: (ThemeMode, Offset, Float) -> Unit,
    preferredQuality: String,
    onQualityChange: (String) -> Unit,
    youtubePreferredQuality: String,
    onYouTubeQualityChange: (String) -> Unit,
    biliPreferredQuality: String,
    onBiliQualityChange: (String) -> Unit,
    kugouPreferredQuality: String,
    onKugouQualityChange: (String) -> Unit,
    qqMusicPreferredQuality: String,
    onQqMusicQualityChange: (String) -> Unit,
    mobileDataFollowDefaultAudioQuality: Boolean,
    onMobileDataFollowDefaultAudioQualityChange: (Boolean) -> Unit,
    mobileDataNeteaseAudioQuality: String,
    onMobileDataNeteaseAudioQualityChange: (String) -> Unit,
    mobileDataYouTubeAudioQuality: String,
    onMobileDataYouTubeAudioQualityChange: (String) -> Unit,
    mobileDataBiliAudioQuality: String,
    onMobileDataBiliAudioQualityChange: (String) -> Unit,
    devModeEnabled: Boolean,
    onDevModeChange: (Boolean) -> Unit,
    seedColorHex: String,
    onSeedColorChange: (String) -> Unit,
    themeColorPalette: List<String>,
    onAddColorToPalette: (String) -> Unit,
    onRemoveColorFromPalette: (String) -> Unit,
    themePaletteStyle: String,
    onThemePaletteStyleChange: (String) -> Unit,
    themeColorSpec: String,
    onThemeColorSpecChange: (String) -> Unit,
    lyricBlurEnabled: Boolean,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    lyricBlurAmount: Float,
    onLyricBlurAmountChange: (Float) -> Unit,
    cloudMusicLyricDefaultOffsetMs: Long,
    onCloudMusicLyricDefaultOffsetMsChange: (Long) -> Unit,
    qqMusicLyricDefaultOffsetMs: Long,
    onQqMusicLyricDefaultOffsetMsChange: (Long) -> Unit,
    floatingLyricsPreferences: FloatingLyricsPreferences,
    onFloatingLyricsPreferencesChange: (FloatingLyricsPreferences) -> Unit,
    advancedBlurEnabled: Boolean,
    onAdvancedBlurEnabledChange: (Boolean) -> Unit,
    enhancedAdvancedBlurEnabled: Boolean,
    onEnhancedAdvancedBlurEnabledChange: (Boolean) -> Unit,
    enhancedAdvancedBlurRadiusDp: Float,
    onEnhancedAdvancedBlurRadiusDpChange: (Float) -> Unit,
    advancedBlurQuality: AdvancedBlurQuality,
    onAdvancedBlurQualityChange: (AdvancedBlurQuality) -> Unit,
    nowPlayingAudioReactiveEnabled: Boolean,
    onNowPlayingAudioReactiveEnabledChange: (Boolean) -> Unit,
    nowPlayingDynamicBackgroundEnabled: Boolean,
    onNowPlayingDynamicBackgroundEnabledChange: (Boolean) -> Unit,
    nowPlayingCoverBlurBackgroundEnabled: Boolean,
    onNowPlayingCoverBlurBackgroundEnabledChange: (Boolean) -> Unit,
    nowPlayingCoverBlurAmount: Float,
    onNowPlayingCoverBlurAmountChange: (Float) -> Unit,
    nowPlayingCoverBlurDarken: Float,
    onNowPlayingCoverBlurDarkenChange: (Float) -> Unit,
    lyricFontScales: LyricFontScales,
    onLyricFontScaleChange: (LyricFontScaleTarget, Float) -> Unit,
    uiDensityScale: Float,
    onUiDensityScaleChange: (Float) -> Unit,
    bypassProxy: Boolean,
    onBypassProxyChange: (Boolean) -> Unit,
    backgroundImageUri: String?,
    onBackgroundImageChange: (Uri?) -> Unit,
    downloadDirectoryUri: String?,
    downloadFileNameTemplate: String?,
    onDownloadDirectoryUriChange: (String?, String?) -> Unit,
    onDownloadFileNameTemplateChange: (String?) -> Unit,
    backgroundImageBlur: Float,
    onBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageBlurChangeFinished: (Float) -> Unit,
    backgroundImageAlpha: Float,
    onBackgroundImageAlphaChange: (Float) -> Unit,
    onBackgroundImageAlphaChangeFinished: (Float) -> Unit,
    defaultStartDestination: String,
    onDefaultStartDestinationChange: (String) -> Unit,
    showHomeContinueCard: Boolean,
    onShowHomeContinueCardChange: (Boolean) -> Unit,
    showHomeTrendingCard: Boolean,
    onShowHomeTrendingCardChange: (Boolean) -> Unit,
    showHomeRadarCard: Boolean,
    onShowHomeRadarCardChange: (Boolean) -> Unit,
    showHomeRecommendedCard: Boolean,
    onShowHomeRecommendedCardChange: (Boolean) -> Unit,
    homeHasRecentUsage: Boolean,
    playbackFadeIn: Boolean,
    onPlaybackFadeInChange: (Boolean) -> Unit,
    playbackCrossfadeNext: Boolean,
    onPlaybackCrossfadeNextChange: (Boolean) -> Unit,
    sleepTimerFinishCurrentOnExpiry: Boolean,
    onSleepTimerFinishCurrentOnExpiryChange: (Boolean) -> Unit,
    playbackFadeInDurationMs: Long,
    onPlaybackFadeInDurationMsChange: (Long) -> Unit,
    playbackFadeOutDurationMs: Long,
    onPlaybackFadeOutDurationMsChange: (Long) -> Unit,
    playbackCrossfadeInDurationMs: Long,
    onPlaybackCrossfadeInDurationMsChange: (Long) -> Unit,
    playbackCrossfadeOutDurationMs: Long,
    onPlaybackCrossfadeOutDurationMsChange: (Long) -> Unit,
    playbackVolumeNormalizationEnabled: Boolean,
    onPlaybackVolumeNormalizationEnabledChange: (Boolean) -> Unit,
    playbackHighResolutionOutputEnabled: Boolean,
    onPlaybackHighResolutionOutputEnabledChange: (Boolean) -> Unit,
    playbackVolumeBalance: Float,
    onPlaybackVolumeBalanceChange: (Float) -> Unit,
    keepLastPlaybackProgress: Boolean,
    onKeepLastPlaybackProgressChange: (Boolean) -> Unit,
    rememberLongFormPlaybackProgress: Boolean,
    onRememberLongFormPlaybackProgressChange: (Boolean) -> Unit,
    keepPlaybackModeState: Boolean,
    onKeepPlaybackModeStateChange: (Boolean) -> Unit,
    neteaseAutoSourceSwitch: Boolean,
    onNeteaseAutoSourceSwitchChange: (Boolean) -> Unit,
    neteaseLocalSourceFallback: Boolean,
    onNeteaseLocalSourceFallbackChange: (Boolean) -> Unit,
    stopOnBluetoothDisconnect: Boolean,
    onStopOnBluetoothDisconnectChange: (Boolean) -> Unit,
    usbExclusivePlayback: Boolean,
    onUsbExclusivePlaybackChange: (Boolean) -> Unit,
    allowMixedPlayback: Boolean,
    onAllowMixedPlaybackChange: (Boolean) -> Unit,
    preemptAudioFocus: Boolean,
    onPreemptAudioFocusChange: (Boolean) -> Unit,
    onNavigateToDownloadManager: () -> Unit = {},
    onNavigateToHomeSettings: () -> Unit = {},
    onNavigateToTabOrderSettings: () -> Unit = {},
    maxCacheSizeBytes: Long,
    onMaxCacheSizeBytesChange: (Long) -> Unit,
    onClearCacheClick: (StorageCacheClearOptions) -> Unit,
    onBeforeLanguageRestart: () -> Unit = {},
    onLanguageChanged: (LanguageManager.Language) -> Unit = {},
```

### SettingsScreen.kt conflict 3

```kotlin
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showNeteaseSavedCookieDialog by remember { mutableStateOf(false) }
    var showBiliSheet by remember { mutableStateOf(false) }
    var showBiliSavedCookieDialog by remember { mutableStateOf(false) }
    var showYouTubeSheet by remember { mutableStateOf(false) }
    var showYouTubeSavedCookieDialog by remember { mutableStateOf(false) }
    var showKugouSheet by remember { mutableStateOf(false) }
    var showKugouLogoutDialog by remember { mutableStateOf(false) }
    var showQqMusicQualityDialog by remember { mutableStateOf(false) }
    var showQqMusicSheet by remember { mutableStateOf(false) }
    var showQqMusicLogoutDialog by remember { mutableStateOf(false) }
```

### SettingsScreen.kt conflict 4

```kotlin
    val downloadDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        if (guardDownloadDirectoryChange()) {
            return@rememberLauncherForActivityResult
        }
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            val targetUri = uri.toString()
            scope.launch {
                val targetSummary = withContext(Dispatchers.IO) {
                    ManagedDownloadStorage.describeConfiguredDirectory(context, targetUri)
                }
                prepareDownloadDirectoryChange(
                    targetUri = targetUri,
                    targetSummary = targetSummary,
                    releaseTargetPermissionOnCancel = true
                )
            }
        } catch (e: SecurityException) {
            inlineMsg = composeResources.getString(
                R.string.settings_download_directory_pick_failed,
                e.message ?: ""
            )
        }
    }

    LaunchedEffect(listenTogetherWorkerBaseUrlInput) {
        if (listenTogetherServerInput != listenTogetherWorkerBaseUrlInput) {
            listenTogetherServerInput = listenTogetherWorkerBaseUrlInput
        }
    }
    LaunchedEffect(listenTogetherNickname, showListenTogetherNicknameDialog) {
        if (!showListenTogetherNicknameDialog) {
            listenTogetherNicknameInput = listenTogetherNickname
            listenTogetherNicknameError = null
        }
    }

    var downloadDirectorySummary by remember(downloadDirectoryUri, defaultDownloadDirectorySummary) {
        mutableStateOf(defaultDownloadDirectorySummary)
    }
    LaunchedEffect(downloadDirectoryUri, defaultDownloadDirectorySummary) {
        downloadDirectorySummary = if (downloadDirectoryUri.isNullOrBlank()) {
            defaultDownloadDirectorySummary
        } else {
            withContext(Dispatchers.IO) {
                ManagedDownloadStorage.describeConfiguredDirectory(context, downloadDirectoryUri)
            }
        }
    }
    val resetDownloadDirectory: () -> Unit = {
        if (!guardDownloadDirectoryChange()) {
            scope.launch {
                prepareDownloadDirectoryChange(
                    targetUri = null,
                    targetSummary = defaultDownloadDirectorySummary,
                    releaseTargetPermissionOnCancel = false
                )
            }
        }
    }

    // 备份与恢复的SAF启动器
    val exportPlaylistLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupRestoreVm.initialize(context)
            backupRestoreVm.exportPlaylists(uri)
        }
    }

    val importPlaylistLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument()
    ) { uri ->
        if (uri != null) {
            backupRestoreVm.initialize(context)
            backupRestoreVm.importPlaylists(uri)
        }
    }

    val exportConfigLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            configTransferVm.initialize(context)
            configTransferVm.exportConfig(uri)
        }
    }

    val importConfigLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument()
    ) { uri ->
        if (uri != null) {
            configTransferVm.initialize(context)
            configTransferVm.importConfig(uri)
        }
    }

    LaunchedEffect(configTransferUiState.importRequiresActivityRecreate) {
        if (!configTransferUiState.importRequiresActivityRecreate) {
            return@LaunchedEffect
        }
        onBeforeLanguageRestart()
        configTransferVm.consumeImportRecreateRequest()
        (context as? android.app.Activity)?.recreate()
    }

    val qualityLabel = remember(preferredQuality) {
        context.neteaseQualityLabel(preferredQuality)
    }

    val biliQualityLabel = remember(biliPreferredQuality) {
        context.biliQualityLabel(biliPreferredQuality)
    }

    val kugouQualityLabel = remember(kugouPreferredQuality) {
        context.kugouQualityLabel(kugouPreferredQuality)
    }

    val qqMusicQualityLabel = remember(qqMusicPreferredQuality) {
        resolveQQMusicQualityLabel(qqMusicPreferredQuality) { context.getString(it) }
    }

    val youtubeQualityLabel = remember(youtubePreferredQuality) {
        context.youtubeQualityLabel(youtubePreferredQuality)
    }

    val normalizedMobileDataNeteaseAudioQuality = remember(mobileDataNeteaseAudioQuality) {
        normalizeMobileDataNeteaseAudioQuality(mobileDataNeteaseAudioQuality)
    }
    val normalizedMobileDataYouTubeAudioQuality = remember(mobileDataYouTubeAudioQuality) {
        normalizeMobileDataYouTubeAudioQuality(mobileDataYouTubeAudioQuality)
    }
    val normalizedMobileDataBiliAudioQuality = remember(mobileDataBiliAudioQuality) {
        normalizeMobileDataBiliAudioQuality(mobileDataBiliAudioQuality)
    }
    val mobileDataNeteaseQualityLabel = remember(normalizedMobileDataNeteaseAudioQuality) {
        context.neteaseQualityLabel(normalizedMobileDataNeteaseAudioQuality)
    }
    val mobileDataYouTubeQualityLabel = remember(normalizedMobileDataYouTubeAudioQuality) {
        context.youtubeQualityLabel(normalizedMobileDataYouTubeAudioQuality)
    }
    val mobileDataBiliQualityLabel = remember(normalizedMobileDataBiliAudioQuality) {
        context.biliQualityLabel(normalizedMobileDataBiliAudioQuality)
    }
    val density = LocalDensity.current

    val homeStartAvailable =
        showHomeTrendingCard ||
            showHomeRadarCard ||
            showHomeRecommendedCard ||
            (showHomeContinueCard && homeHasRecentUsage)
    val homeTrendingLabelRes = if (internationalEnabled) {
        R.string.home_ytmusic_guess_you_like
    } else {
        R.string.settings_home_card_netease_trending
    }
    val homeRadarLabelRes = if (internationalEnabled) {
        R.string.home_ytmusic_daily_discover
    } else {
        R.string.settings_home_card_netease_radar
    }
    val homeRecommendedLabelRes = if (internationalEnabled) {
        R.string.home_ytmusic_more_recommendations
    } else {
        R.string.settings_home_card_netease_recommended
    }
    val homeTrendingSupportingRes = if (internationalEnabled) {
        R.string.settings_home_card_ytmusic_guess_you_like_desc
    } else {
        R.string.settings_home_card_netease_trending_desc
    }
    val homeRadarSupportingRes = if (internationalEnabled) {
        R.string.settings_home_card_ytmusic_daily_discover_desc
    } else {
        R.string.settings_home_card_netease_radar_desc
    }
    val homeRecommendedSupportingRes = if (internationalEnabled) {
        R.string.settings_home_card_ytmusic_more_recommendations_desc
    } else {
        R.string.settings_home_card_netease_recommended_desc
    }
    val effectiveDefaultStartDestination = remember(defaultStartDestination, homeStartAvailable) {
        if (!homeStartAvailable && defaultStartDestination == "home") {
            "explore"
        } else {
            defaultStartDestination
        }
    }
    val defaultStartDestinationLabel = remember(effectiveDefaultStartDestination, context) {
        when (effectiveDefaultStartDestination) {
            "explore" -> composeResources.getString(R.string.nav_explore)
            "library" -> composeResources.getString(R.string.nav_library)
            "settings" -> composeResources.getString(R.string.nav_settings)
            else -> composeResources.getString(R.string.nav_home)
        }
    }
    LaunchedEffect(neteaseVm) {
        neteaseVm.events.collect { e ->
            when (e) {
                is NeteaseAuthEvent.ShowSnack -> {
                    inlineMsg = e.message
                }
                is NeteaseAuthEvent.AskConfirmSend -> {
                    confirmPhoneMasked = e.masked
                    showConfirmDialog = true
                }
                NeteaseAuthEvent.LoginSuccess -> {
                    showNeteaseSavedCookieDialog = false
                    inlineMsg = null
                    showNeteaseSheet = false
                    loginSuccessTitle = composeResources.getString(
                        R.string.settings_netease_login_success
                    )
                    neteaseVm.refreshAuthHealth()
                }
            }
        }
    }

    LaunchedEffect(biliVm) {
        biliVm.events.collect { e ->
            when (e) {
                is BiliAuthEvent.ShowSnack -> inlineMsg = e.message
                BiliAuthEvent.LoginSuccess -> {
                    showBiliSavedCookieDialog = false
                    inlineMsg = null
                    showBiliSheet = false
                    loginSuccessTitle = composeResources.getString(
                        R.string.settings_bili_login_success
                    )
                    biliVm.refreshAuthHealth()
                }
            }
        }
    }

    LaunchedEffect(youtubeVm) {
        youtubeVm.events.collect { e ->
            when (e) {
                is YouTubeAuthEvent.ShowSnack -> inlineMsg = e.message
                YouTubeAuthEvent.LoginSuccess -> {
                    showYouTubeSavedCookieDialog = false
                    inlineMsg = null
                    showYouTubeSheet = false
                    loginSuccessTitle = composeResources.getString(
                        R.string.settings_youtube_login_success
                    )
                    youtubeVm.refreshAuthHealth()
                }

            }
        }
    }

    val isSettingsSplitLayout = currentWindowWidthDp() >= 840.dp
    var activeSettingsPage by rememberSaveable {
        mutableStateOf(if (isSettingsSplitLayout) SettingsPage.General else null)
    }
    fun refreshStorageDetails() {
        if (storageDetailsLoading) return
        storageScanRequest++
    }

    LaunchedEffect(storageScanRequest) {
        if (storageScanRequest == 0) return@LaunchedEffect
        storageDetailsLoading = true
        yield()
        try {
            storageDetails = analyzeStorageUsage(context)
        } finally {
            storageDetailsLoading = false
        }
    }
```

### SettingsScreen.kt conflict 5

```kotlin
                SettingsPage.Accounts -> {
                    miuixSettingsSectionCardItem("${selectedPage.name}:content") {
                        SettingsLoginExpandedContent(
                            biliVm = biliVm,
                            youtubeVm = youtubeVm,
                            neteaseVm = neteaseVm,
                            onOpenBiliSheet = { tab ->
                                inlineMsg = null
                                biliSheetInitialTab = tab
                                showBiliSheet = true
                            },
                            onOpenBiliSavedCookieDialog = {
                                inlineMsg = null
                                showBiliSavedCookieDialog = true
                            },
                            onOpenYouTubeSavedCookieDialog = {
                                inlineMsg = null
                                showYouTubeSavedCookieDialog = true
                            },
                            onOpenNeteaseSavedCookieDialog = {
                                inlineMsg = null
                                showNeteaseSavedCookieDialog = true
                            },
                            onOpenYouTubeSheet = {
                                inlineMsg = null
                                youtubeSheetInitialTab = 0
                                showYouTubeSheet = true
                            },
                            onOpenNeteaseSheet = {
                                inlineMsg = null
                                neteaseSheetInitialTab = 0
                                showNeteaseSheet = true
                            },
                            onOpenKugouSheet = {
                                inlineMsg = null
                                showKugouSheet = true
                            },
                            onKugouLogout = {
                                inlineMsg = null
                                showKugouLogoutDialog = true
                            },
                            onOpenQqMusicSheet = {
                                inlineMsg = null
                                showQqMusicSheet = true
                            },
                            onQqMusicLogout = {
                                inlineMsg = null
                                showQqMusicLogoutDialog = true
```

### SettingsScreen.kt conflict 6

```kotlin
                SettingsPage.Personalization -> {
                    for (cardIndex in 0..4) {
                        item(key = "${selectedPage.name}:card:$cardIndex") {
                            SettingsPersonalizationPageContent(
                                autoSettingsRepository = autoSettingsRepository,
                                scope = scope,
                                defaultStartDestinationLabel = defaultStartDestinationLabel,
                                onOpenDefaultStartDestination = { showDefaultStartDestinationDialog = true },
                                internationalEnabled = internationalEnabled,
                                homeTrendingLabelRes = homeTrendingLabelRes,
                                homeRadarLabelRes = homeRadarLabelRes,
                                homeRecommendedLabelRes = homeRecommendedLabelRes,
                                homeTrendingSupportingRes = homeTrendingSupportingRes,
                                homeRadarSupportingRes = homeRadarSupportingRes,
                                homeRecommendedSupportingRes = homeRecommendedSupportingRes,
                                homeStartAvailable = homeStartAvailable,
                                showHomeContinueCard = showHomeContinueCard,
                                onShowHomeContinueCardChange = onShowHomeContinueCardChange,
                                showHomeTrendingCard = showHomeTrendingCard,
                                onShowHomeTrendingCardChange = onShowHomeTrendingCardChange,
                                showHomeRadarCard = showHomeRadarCard,
                                onShowHomeRadarCardChange = onShowHomeRadarCardChange,
                                showHomeRecommendedCard = showHomeRecommendedCard,
                                onShowHomeRecommendedCardChange = onShowHomeRecommendedCardChange,
                                onNavigateToHomeSettings = onNavigateToHomeSettings,
                                onNavigateToTabOrderSettings = onNavigateToTabOrderSettings,
                                backgroundImageUri = backgroundImageUri,
                                onPickBackgroundImage = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onClearBackgroundImage = {
                                    scope.launch {
                                        BackgroundImageStorage.deleteManagedBackground(
                                            context = context,
                                            uriString = backgroundImageUri
```

### SettingsScreen.kt conflict 7

```kotlin
                SettingsPage.AudioQuality -> {
                    miuixSettingsSectionCardItem("${selectedPage.name}:content") {
                        val qqMusicLoggedIn by AppContainer.qqMusicSession.loggedInFlow
                            .collectAsState()
                        SettingsAudioQualitySection(
                            expanded = true,
                            arrowRotation = 0f,
                            onExpandedChange = {},
                            showHeader = false,
                            qualityLabel = qualityLabel,
                            preferredQuality = preferredQuality,
                            onQualityChange = onQualityChange,
                            youtubeQualityLabel = youtubeQualityLabel,
                            youtubePreferredQuality = youtubePreferredQuality,
                            onYouTubeQualityChange = onYouTubeQualityChange,
                            biliQualityLabel = biliQualityLabel,
                            biliPreferredQuality = biliPreferredQuality,
                            onBiliQualityChange = onBiliQualityChange,
                            kugouQualityLabel = kugouQualityLabel,
                            kugouPreferredQuality = kugouPreferredQuality,
                            onKugouQualityChange = onKugouQualityChange,
                            qqMusicQualityLabel = qqMusicQualityLabel,
                            qqMusicPreferredQuality = qqMusicPreferredQuality,
                            onQqMusicQualityChange = onQqMusicQualityChange,
                            qqMusicLoggedIn = qqMusicLoggedIn,
                            mobileDataFollowDefaultAudioQuality = mobileDataFollowDefaultAudioQuality,
                            onMobileDataFollowDefaultAudioQualityChange =
                                onMobileDataFollowDefaultAudioQualityChange,
                            mobileDataNeteaseQualityLabel = mobileDataNeteaseQualityLabel,
                            mobileDataNeteaseAudioQuality = normalizedMobileDataNeteaseAudioQuality,
                            onMobileDataNeteaseAudioQualityChange =
                                onMobileDataNeteaseAudioQualityChange,
                            mobileDataYouTubeQualityLabel = mobileDataYouTubeQualityLabel,
                            mobileDataYouTubeAudioQuality = normalizedMobileDataYouTubeAudioQuality,
                            onMobileDataYouTubeAudioQualityChange =
                                onMobileDataYouTubeAudioQualityChange,
                            mobileDataBiliQualityLabel = mobileDataBiliQualityLabel,
                            mobileDataBiliAudioQuality = normalizedMobileDataBiliAudioQuality,
                            onMobileDataBiliAudioQualityChange = onMobileDataBiliAudioQualityChange,
                            showQualityDialog = showQualityDialog,
                            onShowQualityDialogChange = { showQualityDialog = it },
                            showYouTubeQualityDialog = showYouTubeQualityDialog,
                            onShowYouTubeQualityDialogChange = { showYouTubeQualityDialog = it },
                            showBiliQualityDialog = showBiliQualityDialog,
                            onShowBiliQualityDialogChange = { showBiliQualityDialog = it },
                            showKugouQualityDialog = showKugouQualityDialog,
                            onShowKugouQualityDialogChange = { showKugouQualityDialog = it },
                            showQqMusicQualityDialog = showQqMusicQualityDialog,
                            onShowQqMusicQualityDialogChange = {
                                showQqMusicQualityDialog = it
                            },
                            showMobileDataNeteaseQualityDialog = showMobileDataNeteaseQualityDialog,
                            onShowMobileDataNeteaseQualityDialogChange = {
                                showMobileDataNeteaseQualityDialog = it
                            },
                            showMobileDataYouTubeQualityDialog = showMobileDataYouTubeQualityDialog,
                            onShowMobileDataYouTubeQualityDialogChange = {
                                showMobileDataYouTubeQualityDialog = it
                            },
                            showMobileDataBiliQualityDialog = showMobileDataBiliQualityDialog,
                            onShowMobileDataBiliQualityDialogChange = {
                                showMobileDataBiliQualityDialog = it
                            },
                            highlightTargetId = settingsHighlightTargetId,
                            highlightPulse = settingsHighlightPulse,
                            onHighlightFinished = onSettingsHighlightFinished
                        )
                    }
                }
```

### SettingsScreen.kt conflict 8

```kotlin
    SettingsNeteaseAuthDialogs(
        showSheet = showNeteaseSheet,
        initialTab = neteaseSheetInitialTab,
        onDismissSheet = { showNeteaseSheet = false },
        inlineMsg = inlineMsg,
        onInlineMsgChange = { inlineMsg = it },
        showConfirmDialog = showConfirmDialog,
        confirmPhoneMasked = confirmPhoneMasked,
        onDismissConfirmDialog = { showConfirmDialog = false },
        vm = neteaseVm,
        showSavedCookieDialog = showNeteaseSavedCookieDialog,
        onDismissSavedCookieDialog = { showNeteaseSavedCookieDialog = false },
        onOpenSheetAtTab = { tab ->
            inlineMsg = null
            neteaseSheetInitialTab = tab
            showNeteaseSheet = true
        },
        onLogout = {
            showNeteaseSavedCookieDialog = false
            neteaseVm.clearCookies()
        },
        onBrowserLogin = null
    )

    SettingsBiliAuthDialogs(
        showSheet = showBiliSheet,
        initialTab = biliSheetInitialTab,
        onDismissSheet = { showBiliSheet = false },
        inlineMsg = inlineMsg,
        onInlineMsgChange = { inlineMsg = it },
        vm = biliVm,
        showSavedCookieDialog = showBiliSavedCookieDialog,
        onDismissSavedCookieDialog = { showBiliSavedCookieDialog = false },
        onOpenSheetAtTab = { tab ->
            inlineMsg = null
            biliSheetInitialTab = tab
            showBiliSheet = true
        },
        onLogout = {
            showBiliSavedCookieDialog = false
            biliVm.clearCookies()
        },
        onBrowserLogin = null
    )

    SettingsYouTubeAuthDialogs(
        showSheet = showYouTubeSheet,
        initialTab = youtubeSheetInitialTab,
        onDismissSheet = { showYouTubeSheet = false },
        inlineMsg = inlineMsg,
        onInlineMsgChange = { inlineMsg = it },
        vm = youtubeVm,
        showSavedCookieDialog = showYouTubeSavedCookieDialog,
        onDismissSavedCookieDialog = { showYouTubeSavedCookieDialog = false },
        onOpenSheetAtTab = { tab ->
            inlineMsg = null
            youtubeSheetInitialTab = tab
            showYouTubeSheet = true
        },
        onLogout = {
            showYouTubeSavedCookieDialog = false
            youtubeVm.clearAuth()
        }
    )

    if (showKugouSheet) {
        KugouQrLoginSheet(
            onDismiss = { showKugouSheet = false },
            onLoggedIn = {
                showKugouSheet = false
                loginSuccessTitle = composeResources.getString(
                    R.string.settings_kugou_login_success
                )
            }
        )
    }

    if (showQqMusicSheet) {
        QQMusicQrLoginSheet(
            onDismiss = { showQqMusicSheet = false },
            onLoggedIn = {
                showQqMusicSheet = false
                loginSuccessTitle = composeResources.getString(
                    R.string.settings_qqmusic_login_success
                )
            }
        )
    }

    if (showQqMusicLogoutDialog) {
        MiuixSettingsDialog(
            onDismissRequest = { showQqMusicLogoutDialog = false },
            title = { Text(stringResource(R.string.settings_qq_music)) },
            text = { Text(stringResource(R.string.settings_qqmusic_logout_confirm)) },
            confirmButton = {
                MiuixSettingsTextButton(
                    onClick = {
                        showQqMusicLogoutDialog = false
                        AppContainer.qqMusicSession.logout()
                    }
                ) {
                    Text(stringResource(R.string.settings_saved_cookie_logout))
                }
            },
            dismissButton = {
                MiuixSettingsTextButton(onClick = { showQqMusicLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showKugouLogoutDialog) {
        MiuixSettingsDialog(
            onDismissRequest = { showKugouLogoutDialog = false },
            title = { Text(stringResource(R.string.platform_kugou)) },
            text = { Text(stringResource(R.string.settings_kugou_logout_confirm)) },
            confirmButton = {
                MiuixSettingsTextButton(
                    onClick = {
                        showKugouLogoutDialog = false
                        AppContainer.kugouSession.logout()
                    }
                ) {
                    Text(stringResource(R.string.settings_saved_cookie_logout))
                }
            },
            dismissButton = {
                MiuixSettingsTextButton(onClick = { showKugouLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    loginSuccessTitle?.let { title ->
        LoginSuccessDialog(
            title = title,
            onDismiss = { loginSuccessTitle = null }
        )
    }
```

### SettingsScreen.kt conflict 9

```kotlin
    if (showDownloadDirectorySwitchWarningDialog) {
        MiuixSettingsDialog(
            onDismissRequest = { showDownloadDirectorySwitchWarningDialog = false },
            title = {
                Text(stringResource(R.string.settings_download_directory_switch_warning_title))
            },
            text = {
                Text(stringResource(R.string.settings_download_directory_switch_warning_message))
            },
            confirmButton = {
                MiuixSettingsTextButton(
                    enabled = downloadDirectoryChangeEnabled,
                    onClick = {
                        if (guardDownloadDirectoryChange()) {
                            showDownloadDirectorySwitchWarningDialog = false
                            return@MiuixSettingsTextButton
                        }
                        showDownloadDirectorySwitchWarningDialog = false
                        downloadDirectoryLauncher.launch(null)
                    }
                ) {
                    Text(stringResource(R.string.settings_download_directory_switch_warning_confirm))
                }
            },
            dismissButton = {
                MiuixSettingsTextButton(
                    onClick = { showDownloadDirectorySwitchWarningDialog = false }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    pendingDownloadDirectoryChange?.let { pendingChange ->
        MiuixSettingsDialog(
            onDismissRequest = {
                pendingDownloadDirectoryChange = null
                if (pendingChange.releaseTargetPermissionOnCancel) {
                    ManagedDownloadStorage.releasePersistedDirectoryPermission(
                        context,
                        pendingChange.targetUri
                    )
                }
            },
            title = { Text(stringResource(R.string.settings_download_directory_migrate_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_download_directory_migrate_message,
                        pendingChange.targetSummary
                    )
                )
            },
            confirmButton = {
                MiuixSettingsTextButton(
                    enabled = downloadDirectoryChangeEnabled,
                    onClick = {
                        if (
                            guardDownloadDirectoryChange(
                                targetUri = pendingChange.targetUri,
                                releaseTargetPermissionOnBlock = pendingChange.releaseTargetPermissionOnCancel
                            )
                        ) {
                            pendingDownloadDirectoryChange = null
                            return@MiuixSettingsTextButton
                        }
                        pendingDownloadDirectoryChange = null
                        scope.launch {
                            isMigratingDownloadDirectory = true
                            try {
                                runCatching {
                                    val migrationResult = ManagedDownloadStorage.migrateManagedDownloads(
                                        context = context,
                                        fromDirectoryUri = pendingChange.previousUri,
                                        toDirectoryUri = pendingChange.targetUri
                                    )
                                    if (!migrationResult.canSwitchDirectory) {
                                        if (pendingChange.releaseTargetPermissionOnCancel) {
                                            ManagedDownloadStorage.releasePersistedDirectoryPermission(
                                                context,
                                                pendingChange.targetUri
                                            )
                                        }
                                        inlineMsg = resources.getQuantityString(
                                            R.plurals.settings_download_directory_migrate_failed,
                                            migrationResult.skippedFiles,
                                            migrationResult.skippedFiles
                                        )
                                    } else {
                                        applyDownloadDirectoryChange(
                                            targetUri = pendingChange.targetUri,
                                            targetSummary = pendingChange.targetSummary,
                                            previousUri = pendingChange.previousUri,
                                            shouldReleasePreviousPermission = pendingChange.shouldReleasePreviousPermission,
                                            migrationResult = migrationResult
                                        )
                                    }
                                }.onFailure {
                                    if (pendingChange.releaseTargetPermissionOnCancel) {
                                        ManagedDownloadStorage.releasePersistedDirectoryPermission(
                                            context,
                                            pendingChange.targetUri
                                        )
                                    }
                                    inlineMsg = composeResources.getString(
                                        R.string.settings_download_directory_pick_failed,
                                        it.message ?: ""
                                    )
                                }
                            } finally {
                                isMigratingDownloadDirectory = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_download_directory_migrate_confirm))
                }
            },
            dismissButton = {
                MiuixSettingsTextButton(
                    enabled = downloadDirectoryChangeEnabled,
                    onClick = {
                        if (
                            guardDownloadDirectoryChange(
                                targetUri = pendingChange.targetUri,
                                releaseTargetPermissionOnBlock = pendingChange.releaseTargetPermissionOnCancel
                            )
                        ) {
                            pendingDownloadDirectoryChange = null
                            return@MiuixSettingsTextButton
                        }
                        pendingDownloadDirectoryChange = null
                        scope.launch {
                            runCatching {
                                applyDownloadDirectoryChange(
                                    targetUri = pendingChange.targetUri,
                                    targetSummary = pendingChange.targetSummary,
                                    previousUri = pendingChange.previousUri,
                                    shouldReleasePreviousPermission = pendingChange.shouldReleasePreviousPermission
                                )
                            }.onFailure {
                                if (pendingChange.releaseTargetPermissionOnCancel) {
                                    ManagedDownloadStorage.releasePersistedDirectoryPermission(
                                        context,
                                        pendingChange.targetUri
                                    )
                                }
                                inlineMsg = composeResources.getString(
                                    R.string.settings_download_directory_pick_failed,
                                    it.message ?: ""
                                )
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_download_directory_migrate_skip))
                }
            }
        )
    }

    if (isMigratingDownloadDirectory) {
        val activeMigrationProgress = migrationProgress
        val stageText = when (activeMigrationProgress?.stage) {
            ManagedDownloadStorage.MigrationStage.PREPARING ->
                stringResource(R.string.settings_download_directory_migrating_stage_preparing)
            ManagedDownloadStorage.MigrationStage.COPYING ->
                stringResource(R.string.settings_download_directory_migrating_stage_copying)
            ManagedDownloadStorage.MigrationStage.REWRITING_METADATA ->
                stringResource(R.string.settings_download_directory_migrating_stage_rewriting)
            ManagedDownloadStorage.MigrationStage.CLEANING_UP ->
                stringResource(R.string.settings_download_directory_migrating_stage_cleanup)
            ManagedDownloadStorage.MigrationStage.FINALIZING ->
                stringResource(R.string.settings_download_directory_migrating)
            null -> stringResource(R.string.settings_download_directory_migrating_desc)
        }
        val progressFraction = activeMigrationProgress?.fraction?.coerceIn(0f, 1f) ?: 0f
        val processedSummary = activeMigrationProgress?.let { progress ->
            resources.getQuantityString(
                R.plurals.settings_download_directory_migrating_progress_files,
                progress.stageTotal.coerceAtLeast(0),
                progress.stageProcessed.coerceAtLeast(0),
                progress.stageTotal.coerceAtLeast(0)
            )
        }
        val copiedBytesSummary = activeMigrationProgress
            ?.takeIf { it.totalBytes > 0L }
            ?.let { progress ->
                composeResources.getString(
                    R.string.settings_download_directory_migrating_progress_bytes,
                    Formatter.formatShortFileSize(context, progress.copiedBytes.coerceAtLeast(0L)),
                    Formatter.formatShortFileSize(context, progress.totalBytes)
                )
            }
        val currentFileSummary = activeMigrationProgress?.currentFileName
            ?.takeIf(String::isNotBlank)
            ?.let { fileName ->
                composeResources.getString(R.string.settings_download_directory_migrating_current, fileName)
            }
        MiuixSettingsDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.settings_download_directory_migrating)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(stageText)
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    processedSummary?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    copiedBytesSummary?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    currentFileSummary?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

}

private data class ThemeOption(
    val value: String,
    val labelRes: Int,
    val descriptionRes: Int
)

@Composable
private fun ThemeModeSelectorListItem(
    isDarkTheme: Boolean,
    themeMode: ThemeMode,
    onThemeModeRequest: (ThemeMode, Offset, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var tabsTopLeftInWindow by remember { mutableStateOf(Offset.Zero) }
    var tabsWidthPx by remember { mutableFloatStateOf(0f) }
    var tabsHeightPx by remember { mutableFloatStateOf(0f) }
    val selectedIndex = if (isDarkTheme) 1 else 0

    ListItem(
        modifier = modifier,
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Brightness4,
                contentDescription = stringResource(R.string.settings_theme_mode),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(stringResource(R.string.settings_theme_mode)) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_theme_mode_desc))
                MiuixSettingsSegmentedTabs(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        tabsTopLeftInWindow = coordinates.positionInWindow()
                        tabsWidthPx = coordinates.size.width.toFloat()
                        tabsHeightPx = coordinates.size.height.toFloat()
                    },
                    labels = listOf(
                        stringResource(R.string.settings_theme_mode_light),
                        stringResource(R.string.settings_theme_mode_dark)
                    ),
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { index ->
                        val targetMode = if (index == 0) {
                            ThemeMode.LIGHT
                        } else {
                            ThemeMode.DARK
                        }
                        if (targetMode != themeMode) {
                            val tabWidth = tabsWidthPx / 2f
                            val origin = if (tabWidth > 0f && tabsHeightPx > 0f) {
                                tabsTopLeftInWindow + Offset(
                                    x = tabWidth * (index + 0.5f),
                                    y = tabsHeightPx / 2f
                                )
                            } else {
                                Offset.Zero
                            }
                            onThemeModeRequest(targetMode, origin, 1f)
                        }
                    }
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ThemeAutoModeListItem(
    themeMode: ThemeMode,
    isDarkTheme: Boolean,
    onThemeModeRequest: (ThemeMode, Offset, Float) -> Unit
) {
    val autoEnabled = themeMode == ThemeMode.AUTO
    var switchCenterInWindow by remember { mutableStateOf<Offset?>(null) }
    var revealStartRadiusPx by remember { mutableFloatStateOf(18f) }

    fun requestAutoMode(enabled: Boolean) {
        if (enabled == autoEnabled) {
            return
        }
        val targetMode = when {
            enabled -> ThemeMode.AUTO
            isDarkTheme -> ThemeMode.DARK
            else -> ThemeMode.LIGHT
        }
        onThemeModeRequest(
            targetMode,
            switchCenterInWindow ?: Offset.Zero,
            revealStartRadiusPx
        )
    }

    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.BrightnessAuto,
                contentDescription = stringResource(R.string.settings_theme_mode_auto),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(stringResource(R.string.settings_theme_mode_auto)) },
        supportingContent = {
            Text(stringResource(R.string.settings_theme_mode_auto_desc))
        },
        trailingContent = {
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    revealStartRadiusPx = maxOf(coordinates.size.width, coordinates.size.height) / 2f
                    switchCenterInWindow = coordinates.positionInWindow() + Offset(
                        x = coordinates.size.width / 2f,
                        y = coordinates.size.height / 2f
                    )
                }
                    .size(width = 56.dp, height = 40.dp)
                    .settingsItemClickable(onClick = {
                        requestAutoMode(!autoEnabled)
                    }),
                contentAlignment = Alignment.Center
            ) {
                MiuixSettingsSwitch(
                    checked = autoEnabled,
                    onCheckedChange = null
                )
            }
        },
        modifier = Modifier.settingsItemClickable(onClick = {
            requestAutoMode(!autoEnabled)
        }),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ThemePaletteStyleSelector(
    selectedStyle: String,
    onStyleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedStyle = ThemeDefaults.normalizePaletteStyle(selectedStyle)
    val options = listOf(
        ThemeOption("TonalSpot", R.string.settings_theme_style_tonal_spot, R.string.settings_theme_style_tonal_spot_desc),
        ThemeOption("Neutral", R.string.settings_theme_style_neutral, R.string.settings_theme_style_neutral_desc),
        ThemeOption("Vibrant", R.string.settings_theme_style_vibrant, R.string.settings_theme_style_vibrant_desc),
        ThemeOption("Expressive", R.string.settings_theme_style_expressive, R.string.settings_theme_style_expressive_desc),
        ThemeOption("Monochrome", R.string.settings_theme_style_monochrome, R.string.settings_theme_style_monochrome_desc),
        ThemeOption("Fidelity", R.string.settings_theme_style_fidelity, R.string.settings_theme_style_fidelity_desc)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_theme_palette_style),
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_theme_palette_style_desc),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        options.forEach { option ->
            MiuixSettingsChoiceRow(
                title = stringResource(option.labelRes),
                subtitle = stringResource(option.descriptionRes),
                selected = normalizedStyle == option.value,
                onClick = { onStyleChange(option.value) }
            )
        }
    }
}

@Composable
private fun ThemeColorSpecSelector(
    selectedSpec: String,
    onSpecChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedSpec = ThemeDefaults.normalizeColorSpec(selectedSpec)
    val options = listOf(
        ThemeDefaults.COLOR_SPECS[0] to stringResource(R.string.settings_theme_color_spec_2021),
        ThemeDefaults.COLOR_SPECS[1] to stringResource(R.string.settings_theme_color_spec_2025)
    )

    ListItem(
        modifier = modifier,
        headlineContent = { Text(stringResource(R.string.settings_theme_color_spec)) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_theme_color_spec_desc))
                MiuixSettingsSegmentedTabs(
                    labels = options.map { it.second },
                    selectedIndex = options.indexOfFirst { it.first == normalizedSpec }.coerceAtLeast(0),
                    onSelectedIndexChange = { index -> onSpecChange(options[index].first) }
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsPersonalizationPageContent(
    autoSettingsRepository: AutoSettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    defaultStartDestinationLabel: String,
    onOpenDefaultStartDestination: () -> Unit,
    internationalEnabled: Boolean,
    homeTrendingLabelRes: Int,
    homeRadarLabelRes: Int,
    homeRecommendedLabelRes: Int,
    homeTrendingSupportingRes: Int?,
    homeRadarSupportingRes: Int?,
    homeRecommendedSupportingRes: Int?,
    homeStartAvailable: Boolean,
    showHomeContinueCard: Boolean,
    onShowHomeContinueCardChange: (Boolean) -> Unit,
    showHomeTrendingCard: Boolean,
    onShowHomeTrendingCardChange: (Boolean) -> Unit,
    showHomeRadarCard: Boolean,
    onShowHomeRadarCardChange: (Boolean) -> Unit,
    showHomeRecommendedCard: Boolean,
    onShowHomeRecommendedCardChange: (Boolean) -> Unit,
    onNavigateToHomeSettings: () -> Unit = {},
    onNavigateToTabOrderSettings: () -> Unit = {},
    backgroundImageUri: String?,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    pendingBackgroundImageBlur: Float,
    onPendingBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageBlurCommit: () -> Unit,
    pendingBackgroundImageAlpha: Float,
    onPendingBackgroundImageAlphaChange: (Float) -> Unit,
    onBackgroundImageAlphaCommit: () -> Unit,
    cardIndex: Int? = null,
    highlightTargetId: String? = null,
    highlightPulse: Int = 0,
    onHighlightFinished: (() -> Unit)? = null
) {
    fun shouldShowCard(index: Int): Boolean = cardIndex == null || cardIndex == index

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val autoShowKeyboard by autoSettingsRepository.autoShowKeyboardFlow.collectAsState(initial = false)
        val showCoverSourceBadge by autoSettingsRepository.showCoverSourceBadgeFlow.collectAsState(initial = true)
        val alwaysUseNewTabStyle by autoSettingsRepository.alwaysUseNewTabStyleFlow.collectAsState(initial = true)
        val nowPlayingShowTitle by autoSettingsRepository.nowPlayingShowTitleFlow.collectAsState(initial = true)
        val nowPlayingSongTitleMarqueeEnabled by autoSettingsRepository
            .nowPlayingSongTitleMarqueeEnabledFlow
            .collectAsState(initial = true)
        val nowPlayingKeepScreenOn by autoSettingsRepository.nowPlayingKeepScreenOnFlow.collectAsState(initial = true)
        val nowPlayingToolbarDockEnabled by autoSettingsRepository.nowPlayingToolbarDockEnabledFlow.collectAsState(
            initial = true
        )
        val playbackControlLayoutPreferences by AppContainer.settingsRepo
            .playbackControlLayoutPreferencesFlow
            .collectAsState(initial = PlaybackControlLayoutPreferences())
        val nowPlayingCoverLyricsEnabled by autoSettingsRepository.nowPlayingCoverLyricsEnabledFlow.collectAsState(
            initial = true
        )
        val nowPlayingProgressShowQualitySwitch by autoSettingsRepository
            .nowPlayingProgressShowQualitySwitchFlow
            .collectAsState(initial = true)
        val nowPlayingProgressShowAudioCodec by autoSettingsRepository
            .nowPlayingProgressShowAudioCodecFlow
            .collectAsState(initial = true)
        val nowPlayingProgressShowAudioSpec by autoSettingsRepository
            .nowPlayingProgressShowAudioSpecFlow
            .collectAsState(initial = true)
        if (shouldShowCard(0)) PersonalizationDetailCard {
            MiuixSettingsSectionIntro(
                title = stringResource(R.string.settings_personalization_start_section),
                description = stringResource(R.string.settings_personalization_start_section_desc)
            )
            AutoSettingsListItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.DEFAULT_START_DESTINATION),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = stringResource(R.string.settings_default_start_screen),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Text(
                        stringResource(
                            R.string.settings_default_start_screen_desc,
                            defaultStartDestinationLabel
                        )
                    )
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished,
                onClick = onOpenDefaultStartDestination
            )

            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.AUTO_SHOW_KEYBOARD),
                checked = autoShowKeyboard,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setAutoShowKeyboard(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
        }

        if (shouldShowCard(1)) PersonalizationDetailCard {
            MiuixSettingsSectionIntro(
                title = stringResource(R.string.settings_personalization_home_section),
                description = stringResource(R.string.settings_personalization_home_section_desc)
            )
            SettingsHomeCardSwitch(
                title = stringResource(R.string.player_continue),
                icon = Icons.Outlined.History,
                checked = showHomeContinueCard,
                onCheckedChange = onShowHomeContinueCardChange,
                targetId = "setting:home_card_continue",
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )

            SettingsHomeCardSwitch(
                title = stringResource(homeTrendingLabelRes),
                description = homeTrendingSupportingRes?.let { stringResource(it) },
                icon = Icons.Outlined.Bolt,
                checked = showHomeTrendingCard,
                onCheckedChange = onShowHomeTrendingCardChange,
                targetId = "setting:home_card_trending",
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )

            SettingsHomeCardSwitch(
                title = stringResource(homeRadarLabelRes),
                description = homeRadarSupportingRes?.let { stringResource(it) },
                icon = if (internationalEnabled) Icons.Outlined.Explore else Icons.Outlined.Radar,
                checked = showHomeRadarCard,
                onCheckedChange = onShowHomeRadarCardChange,
                targetId = "setting:home_card_radar",
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )

            SettingsHomeCardSwitch(
                title = stringResource(homeRecommendedLabelRes),
                description = homeRecommendedSupportingRes?.let { stringResource(it) },
                icon = Icons.Outlined.Star,
                checked = showHomeRecommendedCard,
                onCheckedChange = onShowHomeRecommendedCardChange,
                targetId = "setting:home_card_recommended",
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )

            ListItem(
                modifier = Modifier.settingsItemClickable(onClick = onNavigateToHomeSettings),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                headlineContent = {
                    Text(stringResource(R.string.settings_home_content_source_order))
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.settings_home_content_source_order_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                modifier = Modifier.settingsItemClickable(onClick = onNavigateToTabOrderSettings),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Tab,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                headlineContent = {
                    Text(stringResource(R.string.settings_tab_order))
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.settings_tab_order_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            LazyAnimatedVisibility(visible = !homeStartAvailable) {
                Text(
                    text = stringResource(R.string.settings_home_hidden_notice),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (shouldShowCard(2)) PersonalizationDetailCard {
            MiuixSettingsSectionIntro(
                title = stringResource(R.string.settings_personalization_playback_info_section),
                description = stringResource(R.string.settings_personalization_playback_info_section_desc)
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.SHOW_COVER_SOURCE_BADGE),
                checked = showCoverSourceBadge,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setShowCoverSourceBadge(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.NOWPLAYING_SHOW_TITLE),
                checked = nowPlayingShowTitle,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setNowPlayingShowTitle(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(
                    AutoSettingsKeys.NOW_PLAYING_SONG_TITLE_MARQUEE_ENABLED
                ),
                checked = nowPlayingSongTitleMarqueeEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        autoSettingsRepository.setNowPlayingSongTitleMarqueeEnabled(enabled)
                    }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.NOW_PLAYING_COVER_LYRICS_ENABLED),
                checked = nowPlayingCoverLyricsEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setNowPlayingCoverLyricsEnabled(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(
                    AutoSettingsKeys.NOWPLAYING_PROGRESS_SHOW_QUALITY_SWITCH
                ),
                checked = nowPlayingProgressShowQualitySwitch,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setNowPlayingProgressShowQualitySwitch(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.NOWPLAYING_PROGRESS_SHOW_AUDIO_CODEC),
                checked = nowPlayingProgressShowAudioCodec,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setNowPlayingProgressShowAudioCodec(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.NOWPLAYING_PROGRESS_SHOW_AUDIO_SPEC),
                checked = nowPlayingProgressShowAudioSpec,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setNowPlayingProgressShowAudioSpec(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
        }

        if (shouldShowCard(3)) PersonalizationDetailCard {
            MiuixSettingsSectionIntro(
                title = stringResource(R.string.settings_personalization_playback_controls_section),
                description = stringResource(R.string.settings_personalization_playback_controls_section_desc)
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.ALWAYS_USE_NEW_TAB_STYLE),
                checked = alwaysUseNewTabStyle,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setAlwaysUseNewTabStyle(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.NOWPLAYING_KEEP_SCREEN_ON),
                checked = nowPlayingKeepScreenOn,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setNowPlayingKeepScreenOn(enabled) }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            val nowPlayingControlsAtBottom =
                playbackControlLayoutPreferences.nowPlayingPlacement.placesControlsAtBottom
            PersonalizationSwitchItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.NOWPLAYING_TOOLBAR_DOCK_ENABLED),
                checked = nowPlayingToolbarDockEnabled && !nowPlayingControlsAtBottom,
                onCheckedChange = { enabled ->
                    scope.launch { autoSettingsRepository.setNowPlayingToolbarDockEnabled(enabled) }
                },
                enabled = !nowPlayingControlsAtBottom,
                supportingContent = if (nowPlayingControlsAtBottom) {
                    {
                        Text(
                            stringResource(
                                R.string.settings_nowplaying_toolbar_dock_disabled_by_control_position
                            )
                        )
                    }
                } else {
                    null
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            PlaybackControlLayoutSettings(
                preferences = playbackControlLayoutPreferences,
                onPreferencesChange = { preferences ->
                    scope.launch {
                        AppContainer.settingsRepo.setPlaybackControlLayoutPreferences(preferences)
                    }
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
        }

        if (shouldShowCard(4)) PersonalizationDetailCard {
            MiuixSettingsSectionIntro(
                title = stringResource(R.string.settings_personalization_background_section),
                description = stringResource(R.string.settings_personalization_background_section_desc)
            )
            AutoSettingsListItem(
                setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.BACKGROUND_IMAGE_URI),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Wallpaper,
                        contentDescription = stringResource(R.string.settings_custom_background),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Text(
                        if (backgroundImageUri != null) {
                            stringResource(R.string.settings_background_change)
                        } else {
                            stringResource(R.string.settings_background_select)
                        }
                    )
                },
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished,
                onClick = onPickBackgroundImage
            )

            LazyAnimatedVisibility(visible = backgroundImageUri != null) {
                Column {
                    MiuixSettingsTextButton(onClick = onClearBackgroundImage) {
                        Text(stringResource(R.string.background_clear))
                    }

                    AutoSettingsListItem(
                        setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.BACKGROUND_IMAGE_BLUR),
                        showDefaultIcon = false,
                        highlightTargetId = highlightTargetId,
                        highlightPulse = highlightPulse,
                        onHighlightFinished = onHighlightFinished,
                        supportingContent = {
                            MiuixSettingsSlider(
                                value = pendingBackgroundImageBlur,
                                onValueChange = onPendingBackgroundImageBlurChange,
                                onValueChangeFinished = onBackgroundImageBlurCommit,
                                valueRange = 0f..25f
                            )
                        }
                    )

                    AutoSettingsListItem(
                        setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.BACKGROUND_IMAGE_ALPHA),
                        showDefaultIcon = false,
                        highlightTargetId = highlightTargetId,
                        highlightPulse = highlightPulse,
                        onHighlightFinished = onHighlightFinished,
                        supportingContent = {
                            MiuixSettingsSlider(
                                value = pendingBackgroundImageAlpha,
                                onValueChange = onPendingBackgroundImageAlphaChange,
                                onValueChangeFinished = onBackgroundImageAlphaCommit,
                                valueRange = 0.1f..1.0f
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsLyricsAppearanceContent(
    autoSettingsRepository: AutoSettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    lyricFontScales: LyricFontScales,
    onLyricFontScaleChange: (LyricFontScaleTarget, Float) -> Unit,
    highlightTargetId: String?,
    highlightPulse: Int,
    onHighlightFinished: (() -> Unit)?
) {
    val showLyricTranslation by autoSettingsRepository.showLyricTranslationFlow.collectAsState(initial = true)
    val lyricTranslationUsePhonetic by autoSettingsRepository.lyricTranslationUsePhoneticFlow.collectAsState(
        initial = false
    )

    MiuixSettingsSectionIntro(
        title = stringResource(R.string.settings_lyrics_appearance_section),
        description = stringResource(R.string.settings_lyrics_appearance_section_desc)
    )
    PersonalizationSwitchItem(
        setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.SHOW_LYRIC_TRANSLATION),
        checked = showLyricTranslation,
        onCheckedChange = { enabled ->
            scope.launch { autoSettingsRepository.setShowLyricTranslation(enabled) }
        },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
    PersonalizationSwitchItem(
        setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.LYRIC_TRANSLATION_USE_PHONETIC),
        checked = lyricTranslationUsePhonetic,
        onCheckedChange = { enabled ->
            scope.launch { autoSettingsRepository.setLyricTranslationUsePhonetic(enabled) }
        },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
    MiuixSettingsSectionIntro(
        title = stringResource(R.string.settings_lyrics_cover_page_section),
        description = stringResource(R.string.settings_lyrics_cover_page_section_desc)
    )
    LyricFontScaleSettingsItem(
        setting = AutoSettingsMetadata.requireSetting(
            AutoSettingsKeys.NOWPLAYING_COVER_LYRIC_FONT_SCALE
        ),
        currentScale = lyricFontScales.coverLyric,
        onScaleCommit = { scale ->
            onLyricFontScaleChange(LyricFontScaleTarget.COVER_LYRIC, scale)
        },
        sampleText = stringResource(R.string.settings_lyrics_sample),
        sampleBaseSizeSp = 18f,
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
    LyricFontScaleSettingsItem(
        setting = AutoSettingsMetadata.requireSetting(
            AutoSettingsKeys.NOWPLAYING_COVER_TRANSLATION_FONT_SCALE
        ),
        currentScale = lyricFontScales.coverTranslation,
        onScaleCommit = { scale ->
            onLyricFontScaleChange(LyricFontScaleTarget.COVER_TRANSLATION, scale)
        },
        sampleText = stringResource(R.string.settings_lyrics_translation_sample),
        sampleBaseSizeSp = 14f,
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
    MiuixSettingsSectionIntro(
        title = stringResource(R.string.settings_lyrics_page_section),
        description = stringResource(R.string.settings_lyrics_page_section_desc)
    )
    LyricFontScaleSettingsItem(
        setting = AutoSettingsMetadata.requireSetting(AutoSettingsKeys.LYRICS_PAGE_LYRIC_FONT_SCALE),
        currentScale = lyricFontScales.lyricsPageLyric,
        onScaleCommit = { scale ->
            onLyricFontScaleChange(LyricFontScaleTarget.LYRICS_PAGE_LYRIC, scale)
        },
        sampleText = stringResource(R.string.settings_lyrics_sample),
        sampleBaseSizeSp = 20f,
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
    LyricFontScaleSettingsItem(
        setting = AutoSettingsMetadata.requireSetting(
            AutoSettingsKeys.LYRICS_PAGE_TRANSLATION_FONT_SCALE
        ),
        currentScale = lyricFontScales.lyricsPageTranslation,
        onScaleCommit = { scale ->
            onLyricFontScaleChange(LyricFontScaleTarget.LYRICS_PAGE_TRANSLATION, scale)
        },
        sampleText = stringResource(R.string.settings_lyrics_translation_sample),
        sampleBaseSizeSp = 16f,
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
}

@Composable
private fun PersonalizationDetailCard(
    content: @Composable () -> Unit
) {
    MiuixSettingsSectionCard(
        content = content
    )
}
```

### SettingsScreen.kt conflict 10

```kotlin
    val selectedSetting = remember {
        mutableStateOf<PlaybackControlLayoutSetting?>(null)
    }

    PlaybackControlLayoutListItem(
        targetId = "setting:nowplaying_control_placement",
        icon = Icons.Outlined.DashboardCustomize,
        title = stringResource(R.string.settings_nowplaying_control_placement),
        description = stringResource(R.string.settings_nowplaying_control_placement_desc),
        value = nowPlayingControlPlacementLabel(preferences.nowPlayingPlacement),
        onClick = {
            selectedSetting.value = PlaybackControlLayoutSetting.NOW_PLAYING_PLACEMENT
        },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
    PlaybackControlLayoutListItem(
        targetId = "setting:nowplaying_control_size",
        icon = Icons.Outlined.AspectRatio,
        title = stringResource(R.string.settings_nowplaying_control_size),
        description = stringResource(R.string.settings_nowplaying_control_size_desc),
        value = playbackControlSizeLabel(preferences.nowPlayingSize),
        onClick = {
            selectedSetting.value = PlaybackControlLayoutSetting.NOW_PLAYING_SIZE
        },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
    PlaybackControlLayoutListItem(
        targetId = "setting:lyrics_control_size",
        icon = Icons.Outlined.TextFields,
        title = stringResource(R.string.settings_lyrics_control_size),
        description = stringResource(R.string.settings_lyrics_control_size_desc),
        value = playbackControlSizeLabel(preferences.lyricsSize),
        onClick = {
            selectedSetting.value = PlaybackControlLayoutSetting.LYRICS_SIZE
        },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )

    val setting = selectedSetting.value ?: return
    val title = when (setting) {
        PlaybackControlLayoutSetting.NOW_PLAYING_PLACEMENT ->
            stringResource(R.string.settings_nowplaying_control_placement)
        PlaybackControlLayoutSetting.NOW_PLAYING_SIZE ->
            stringResource(R.string.settings_nowplaying_control_size)
        PlaybackControlLayoutSetting.LYRICS_SIZE ->
            stringResource(R.string.settings_lyrics_control_size)
    }
    MiuixSettingsDialog(
        onDismissRequest = { selectedSetting.value = null },
        title = { Text(title) },
        text = {
            Column {
                when (setting) {
                    PlaybackControlLayoutSetting.NOW_PLAYING_PLACEMENT -> {
                        NowPlayingControlPlacement.entries.forEach { placement ->
                            MiuixSettingsChoiceRow(
                                title = nowPlayingControlPlacementLabel(placement),
                                subtitle = if (placement.placesControlsAtBottom) {
                                    stringResource(
                                        R.string.settings_nowplaying_toolbar_dock_disabled_by_control_position
                                    )
                                } else {
                                    null
                                },
                                selected = placement == preferences.nowPlayingPlacement,
                                onClick = {
                                    onPreferencesChange(
                                        preferences.copy(nowPlayingPlacement = placement)
                                    )
                                    selectedSetting.value = null
                                }
                            )
                        }
                    }

                    PlaybackControlLayoutSetting.NOW_PLAYING_SIZE,
                    PlaybackControlLayoutSetting.LYRICS_SIZE -> {
                        PlaybackControlSize.entries.forEach { size ->
                            val selected = when (setting) {
                                PlaybackControlLayoutSetting.NOW_PLAYING_SIZE ->
                                    size == preferences.nowPlayingSize
                                PlaybackControlLayoutSetting.LYRICS_SIZE ->
                                    size == preferences.lyricsSize
                            }
                            MiuixSettingsChoiceRow(
                                title = playbackControlSizeLabel(size),
                                selected = selected,
                                onClick = {
                                    onPreferencesChange(
                                        when (setting) {
                                            PlaybackControlLayoutSetting.NOW_PLAYING_SIZE ->
                                                preferences.copy(nowPlayingSize = size)
                                            PlaybackControlLayoutSetting.LYRICS_SIZE ->
                                                preferences.copy(lyricsSize = size)
                                        }
                                    )
                                    selectedSetting.value = null
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            MiuixSettingsTextButton(
                onClick = { selectedSetting.value = null },
                text = { Text(stringResource(R.string.action_close)) }
            )
        }
    )
}

@Composable
private fun PlaybackControlLayoutListItem(
    targetId: String,
    icon: ImageVector,
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit,
    highlightTargetId: String?,
    highlightPulse: Int,
    onHighlightFinished: (() -> Unit)?
) {
    ListItem(
        modifier = Modifier
            .settingsHighlightTarget(
                targetId = targetId,
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            .settingsItemClickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun nowPlayingControlPlacementLabel(
    placement: NowPlayingControlPlacement
): String = when (placement) {
    NowPlayingControlPlacement.LOWER ->
        stringResource(R.string.settings_nowplaying_control_placement_lower)
    NowPlayingControlPlacement.BOTTOM ->
        stringResource(R.string.settings_nowplaying_control_placement_bottom)
    NowPlayingControlPlacement.BOTTOM_WITH_PROGRESS ->
        stringResource(R.string.settings_nowplaying_control_placement_bottom_with_progress)
}

@Composable
private fun playbackControlSizeLabel(size: PlaybackControlSize): String = when (size) {
    PlaybackControlSize.SMALL -> stringResource(R.string.settings_playback_control_size_small)
    PlaybackControlSize.MEDIUM -> stringResource(R.string.settings_playback_control_size_medium)
    PlaybackControlSize.LARGE -> stringResource(R.string.settings_playback_control_size_large)
}

@Composable
private fun PersonalizationSwitchItem(
    setting: AutoSettingInfo,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    highlightTargetId: String?,
    highlightPulse: Int,
    onHighlightFinished: (() -> Unit)?
) {
    AutoSettingsListItem(
        setting = setting,
        enabled = enabled,
        supportingContent = supportingContent,
        trailingContent = {
            MiuixSettingsSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        }
    )
}

@Composable
private fun LyricFontScaleSettingsItem(
    setting: AutoSettingInfo,
    currentScale: Float,
    onScaleCommit: (Float) -> Unit,
    sampleText: String,
    sampleBaseSizeSp: Float,
    highlightTargetId: String?,
    highlightPulse: Int,
    onHighlightFinished: (() -> Unit)?
) {
    var pendingScale by remember(setting.keyName) {
        mutableFloatStateOf(normalizeLyricFontScale(currentScale))
    }

    LaunchedEffect(currentScale) {
        val normalizedScale = normalizeLyricFontScale(currentScale)
        if ((pendingScale - normalizedScale).absoluteValue > 0.001f) {
            pendingScale = normalizedScale
        }
    }

    AutoSettingsListItem(
        setting = setting,
        showDefaultIcon = true,
        supportingContent = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        R.string.settings_lyrics_font_scale_value,
                        (pendingScale * 100).roundToInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MiuixSettingsSlider(
                    value = pendingScale,
                    onValueChange = { pendingScale = it },
                    onValueChangeFinished = {
                        onScaleCommit(normalizeLyricFontScale(pendingScale))
                    },
                    valueRange = MIN_LYRIC_FONT_SCALE..MAX_LYRIC_FONT_SCALE,
                    steps = 10
                )
                Text(
                    text = sampleText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    fontSize = scaledLyricFontSize(sampleBaseSizeSp, pendingScale).sp
                )
            }
        },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished
    )
}

@Composable
private fun SettingsHomeCardSwitch(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true,
    targetId: String,
    highlightTargetId: String?,
    highlightPulse: Int,
    onHighlightFinished: (() -> Unit)?
) {
    ListItem(
        modifier = Modifier
            .settingsHighlightTarget(
                targetId = targetId,
                highlightTargetId = highlightTargetId,
                highlightPulse = highlightPulse,
                onHighlightFinished = onHighlightFinished
            )
            .then(
                if (enabled) {
                    Modifier.settingsItemClickable {
                        onCheckedChange(!checked)
                    }
                } else {
                    Modifier.alpha(0.5f)
                }
            ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(title) },
        supportingContent = description?.let { text ->
            {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            MiuixSettingsSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ListenTogetherSettingsSection(
    modifier: Modifier = Modifier,
    isUsingDefaultServer: Boolean,
    isInRoom: Boolean,
    nickname: String,
    onOpenJoinRoomDialog: () -> Unit,
    onOpenServerDialog: () -> Unit,
    onResetIdentity: () -> Unit,
    onOpenNicknameDialog: () -> Unit
) {
    val joinRoomItemModifier = if (isInRoom) {
        Modifier.alpha(0.5f)
    } else {
        Modifier.settingsItemClickable(onClick = onOpenJoinRoomDialog)
    }
    val identityItemModifier = if (isInRoom) {
        Modifier.alpha(0.5f)
    } else {
        Modifier.settingsItemClickable(onClick = onResetIdentity)
    }
    val nicknameItemModifier = if (isInRoom) {
        Modifier.alpha(0.5f)
    } else {
        Modifier.settingsItemClickable(onClick = onOpenNicknameDialog)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ListItem(
            modifier = joinRoomItemModifier,
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.MeetingRoom,
                    contentDescription = stringResource(R.string.listen_together_join_room),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = { Text(stringResource(R.string.listen_together_join_room)) },
            supportingContent = {
                Text(
                    if (isInRoom) {
                        stringResource(R.string.settings_listen_together_join_room_disabled)
                    } else {
                        stringResource(R.string.settings_listen_together_join_room_desc)
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            modifier = Modifier.settingsItemClickable(onClick = onOpenServerDialog),
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = stringResource(R.string.settings_listen_together_server_title),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = { Text(stringResource(R.string.settings_listen_together_server_title)) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isUsingDefaultServer) {
                            stringResource(R.string.settings_listen_together_server_default_desc)
                        } else {
                            stringResource(R.string.settings_listen_together_server_custom_desc)
                        }
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            modifier = nicknameItemModifier,
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.FormatSize,
                    contentDescription = stringResource(
                        R.string.settings_listen_together_default_nickname_title
                    ),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = {
                Text(stringResource(R.string.settings_listen_together_default_nickname_title))
            },
            supportingContent = {
                Text(
                    if (isInRoom) {
                        stringResource(R.string.settings_listen_together_default_nickname_disabled)
                    } else nickname.ifBlank {
                        stringResource(R.string.settings_listen_together_default_nickname_unset)
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            modifier = identityItemModifier,
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = stringResource(R.string.listen_together_reset_uuid),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = { Text(stringResource(R.string.listen_together_reset_uuid)) },
            supportingContent = {
                Text(
                    if (isInRoom) {
                        stringResource(R.string.listen_together_reset_uuid_disabled)
                    } else {
                        stringResource(R.string.settings_listen_together_reset_identity_desc)
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun SettingsLoginExpandedContent(
    biliVm: BiliAuthViewModel,
    youtubeVm: YouTubeAuthViewModel,
    neteaseVm: NeteaseAuthViewModel,
    onOpenBiliSheet: (Int) -> Unit,
    onOpenBiliSavedCookieDialog: () -> Unit,
    onOpenYouTubeSavedCookieDialog: () -> Unit,
    onOpenNeteaseSavedCookieDialog: () -> Unit,
    onOpenYouTubeSheet: () -> Unit,
    onOpenNeteaseSheet: () -> Unit,
    onOpenKugouSheet: () -> Unit = {},
    onKugouLogout: () -> Unit = {},
    onOpenQqMusicSheet: () -> Unit = {},
    onQqMusicLogout: () -> Unit = {},
) {
    val biliAuthUiState by biliVm.uiState.collectAsStateWithLifecycleCompat()
    val youtubeAuthUiState by youtubeVm.uiState.collectAsStateWithLifecycleCompat()
    val neteaseAuthUiState by neteaseVm.uiState.collectAsStateWithLifecycleCompat()

    val kugouLoggedIn by AppContainer.kugouSession.loggedInFlow.collectAsState()
    val qqMusicLoggedIn by AppContainer.qqMusicSession.loggedInFlow.collectAsState()

    LaunchedEffect(biliVm, youtubeVm, neteaseVm) {
        biliVm.refreshAuthHealth()
        neteaseVm.refreshAuthHealth()
        youtubeVm.refreshAuthHealth()
    }

    val biliStatusText = when (biliAuthUiState.health.state) {
        SavedCookieAuthState.Valid -> {
            val relativeTime = biliAuthUiState.health.savedAt
                .takeIf { it > 0L }
                ?.let { formatSyncTime(it) }
                ?: stringResource(R.string.time_just_now)
            stringResource(R.string.settings_bili_status_valid, relativeTime)
        }
        SavedCookieAuthState.Checking -> {
            if (biliAuthUiState.hasSavedCookies) {
                stringResource(R.string.settings_bili_status_saved_invalid)
            } else {
                stringResource(R.string.settings_bili_status_missing)
            }
        }
        SavedCookieAuthState.Missing -> {
            if (biliAuthUiState.hasSavedCookies) {
                stringResource(R.string.settings_bili_status_saved_invalid)
            } else {
                stringResource(R.string.settings_bili_status_missing)
            }
        }
    }
    val neteaseStatusText = when (neteaseAuthUiState.health.state) {
        SavedCookieAuthState.Valid -> {
            val relativeTime = neteaseAuthUiState.health.savedAt
                .takeIf { it > 0L }
                ?.let { formatSyncTime(it) }
                ?: stringResource(R.string.time_just_now)
            stringResource(R.string.settings_netease_status_valid, relativeTime)
        }
        SavedCookieAuthState.Checking -> {
            if (neteaseAuthUiState.hasSavedCookies) {
                stringResource(R.string.settings_netease_status_saved_invalid)
            } else {
                stringResource(R.string.settings_netease_status_missing)
            }
        }
        SavedCookieAuthState.Missing -> {
            if (neteaseAuthUiState.hasSavedCookies) {
                stringResource(R.string.settings_netease_status_saved_invalid)
            } else {
                stringResource(R.string.settings_netease_status_missing)
            }
        }
    }
    val youtubeStatusText = when (youtubeAuthUiState.health.state) {
        YouTubeAuthState.Valid -> {
            val relativeTime = youtubeAuthUiState.health.savedAt
                .takeIf { it > 0L }
                ?.let { formatSyncTime(it) }
                ?: stringResource(R.string.time_just_now)
            stringResource(R.string.settings_youtube_status_valid, relativeTime)
        }
        YouTubeAuthState.Missing -> {
            if (youtubeAuthUiState.hasSavedAuth) {
                stringResource(R.string.settings_youtube_status_saved_invalid)
            } else {
                stringResource(R.string.settings_youtube_status_missing)
            }
        }
    }
    val kugouStatusText = if (kugouLoggedIn) {
        stringResource(R.string.settings_kugou_status_valid)
    } else {
        stringResource(R.string.settings_kugou_status_missing)
    }
    val qqMusicStatusText = if (qqMusicLoggedIn) {
        stringResource(R.string.settings_qqmusic_status_valid)
    } else {
        stringResource(R.string.settings_qqmusic_status_missing)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(start = 16.dp, end = 8.dp, bottom = 8.dp)
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bilibili),
                    contentDescription = stringResource(R.string.settings_bilibili),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = { Text(stringResource(R.string.platform_bilibili)) },
            supportingContent = { Text(biliStatusText) },
            modifier = Modifier.settingsItemClickable(
                onClick = {
                    if (biliAuthUiState.hasSavedCookies) {
                        onOpenBiliSavedCookieDialog()
                    } else {
                        onOpenBiliSheet(0)
                    }
                }
            ),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_youtube),
                    contentDescription = stringResource(R.string.common_youtube),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = { Text(stringResource(R.string.common_youtube)) },
            supportingContent = { Text(youtubeStatusText) },
            modifier = Modifier.settingsItemClickable(
                onClick = {
                    if (youtubeAuthUiState.hasSavedAuth) {
                        onOpenYouTubeSavedCookieDialog()
                    } else {
                        onOpenYouTubeSheet()
                    }
                }
            ),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_netease_cloud_music),
                    contentDescription = stringResource(R.string.settings_netease),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = { Text(stringResource(R.string.platform_netease)) },
            supportingContent = { Text(neteaseStatusText) },
            modifier = Modifier.settingsItemClickable(
                onClick = {
                    if (neteaseAuthUiState.hasSavedCookies) {
                        onOpenNeteaseSavedCookieDialog()
                    } else {
                        onOpenNeteaseSheet()
                    }
                }
            ),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_kugou),
                    contentDescription = stringResource(R.string.platform_kugou),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = { Text(stringResource(R.string.platform_kugou)) },
            supportingContent = { Text(kugouStatusText) },
            modifier = Modifier.settingsItemClickable(
                onClick = {
                    if (kugouLoggedIn) {
                        onKugouLogout()
                    } else {
                        onOpenKugouSheet()
                    }
                }
            ),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_qq_music),
                    contentDescription = stringResource(R.string.settings_qq_music),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = { Text(stringResource(R.string.settings_qq_music)) },
            supportingContent = { Text(qqMusicStatusText) },
            modifier = Modifier.settingsItemClickable(
                onClick = {
                    if (qqMusicLoggedIn) {
                        onQqMusicLogout()
                    } else {
                        onOpenQqMusicSheet()
                    }
                }
            ),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
```


## `app/src/main/java/moe/ouom/neriplayer/ui/viewmodel/tab/HomeViewModel.kt`
### HomeViewModel.kt conflict 1

```kotlin

```

### HomeViewModel.kt conflict 2

```kotlin
        val deadline = System.currentTimeMillis() + HOME_NETEASE_PROBE_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val state = _uiState.value
            val songSections = state.trendingSongSections + state.radarSongSections
            val playlistSections = state.playlistSections
            val settled =
                (songSections.isEmpty() || songSections.all { !it.section.loading }) &&
                    (playlistSections.isEmpty() || playlistSections.all { !it.section.loading }) &&
                    !state.radarPlaylists.loading
            if (settled) break
            delay(200)
        }
        return neteaseHomeHasData(_uiState.value)
    }

    /** 加载酷狗概念版默认内容（每日推荐/榜单/热门歌单），返回是否有可展示内容。 */
    private suspend fun probeKugouSource(): Boolean {
        _uiState.update { state ->
            state.copy(kugouSections = HomeKugouSectionState(loading = true))
        }
        return try {
            val content = withContext(Dispatchers.IO) {
                PlayerManager.kugouSession.loadKugouChannelContent()
            }
            val hasData = kugouContentHasData(content)
            NPLogger.d(
                TAG,
                "probeKugouSource: hasData=$hasData ranks=${content.ranks.size} " +
                    "playlists=${content.playlists.size} daily=${content.dailyRecommend.size}"
            )
            _uiState.update { state ->
                state.copy(
                    kugouSections = HomeKugouSectionState(
                        content = content.takeIf { hasData },
                        loading = false
                    )
                )
            }
            hasData
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            NPLogger.e(TAG, "probeKugouSource failed", e)
            _uiState.update { state ->
                state.copy(kugouSections = HomeKugouSectionState(error = buildHomeErrorMessage(e)))
            }
            false
        }
```

## 额外记录：整体回退到上游、需要成组重贴的功能单元

这些不是文本冲突，而是补丁套上去之后引用了上游重构中已不存在的结构，
因此整体回退为上游版本，避免留下半接线的代码。必须作为一整个单元重贴：

### 媒体库酷狗标签（LibraryTab.KUGOU）
已完成（`a91dd166a`）。`docs/upstream-sync-2026-10/fork-integration-patches/LibraryScreen.patch` 与
`LibraryHostScreen.patch` 属于同一单元，缺一不可：

- `LibraryTab.KUGOU(CoreCommonR.string.library_tab_kugou)` 枚举项
- `LibrarySelectedItem.KugouLibrary(kind)` / `KugouPlaylist(playlist)` 选中态
- `LibraryScrollSource.Kugou` 与 `kugouListState`（`rememberSaveable` + `LazyListState.Saver`）
- `LibraryScreen` 的两处 `when`：滚动折叠门（`shouldAllowCollapsingTopAppBar`）与分页内容
  （`KugouLibraryContent(listState, onOpenDetail, offlineMode)`）
- `LibraryHostScreen` 里 `KugouPlaylistDetailScreen` / `KugouUserPlaylistDetailScreen` /
  `KugouSongListDetailScreen` 的导航注册
- 当前 tab 被隐藏时回落到第一个可见 tab 的逻辑

只做其中一部分会得到一个能编译但点不开的标签，所以 `TabOrdering.kt` 和
`TabOrderSettingsScreen.kt` 里的 `LibraryTab.KUGOU` 分支也一并撤掉了；
单元完成后需要一起加回。

与设计文档的两点偏离：

- `LibrarySelectedItem` 的 KuGou / QQ 子类型改用上游的 `saveState()` 覆写 +
  `librarySelectedItemRestorers` 注册表；fork 原来把 save/restore 写成一个集中式
  `when`，上游已经把它拆成多态，照搬会留下第二套并存机制。
- 上游的 QQ 媒体库只是 `QqMusicPlaylistList` 占位（"敬请期待"），不属于"上游已有
  同样功能"，因此按规则换成 fork 的 `QQMusicLibraryContent`，并删除该占位。

### 探索页酷狗 / QQ 标签
已完成：`SearchSource.KUGOU` / `QQ_MUSIC` 枚举项、`ExploreUiState` 的三个字段
（`kugouContent` / `kugouChannelLoading` / `kugouChannelError`，QQ 同理）、
`loadKugouChannel()` / `loadQQMusicChannel()`，以及 `ExploreScreen` 分页里的
`KugouExploreContent` / `QQMusicExploreContent` 分支。

### 设置页与启动引导（部分完成）
`StartupOnboardingScreen.patch` 已重贴（`0d486b6ff`：QQ / 酷狗扫码登录步骤与未登录告警）。
`SettingsScreen.patch`（22 个 hunk）只剩账号卡片以外的分页集成；`SettingsHostScreen.patch`
仍整体回退到上游。回退原因相同：补丁引用了上游重构中已不存在的结构。

### 仍存在的编译错误全部来自缺失的酷狗 SDK 构件
`docs/歌词源体系接入KuGou与QQMusic设计.md` §10 未决问题 2 之外，还有一项前置：
`top.ghhccghk.multiplatform.kugouapi:kugouapi-kmp:1.0.5` 在本机只能解析到
KMP 元数据 jar（无 `.class`），导致 `core/api/kugou/**`、`core/api/qqmusic/**`
共 105 个编译错误。恢复 Maven Central 访问或放置该 AAR 后即可一并解决。

---

# Upstream sync 2026-10-25: 合并 4b2ad7bf4

merge 提交 `23ff67750`（双亲 `c32f9aa57` + `4b2ad7bf4`），带入 `294251101` 之后的
15 个上游提交（443 文件 / +38542 / −2756）。

## 冲突面

双方共改 31 个文件，但只有 **9 个真冲突**，全部是 `modules/common` 的字符串资源：
`strings_auth.xml`、`strings_settings_playback.xml`、`strings_storage.xml` × 默认 /
`values-en` / `values-zh`。原因是两边都只在文件尾部追加了互不相同的 key，
git 无法自动排布。所有 Kotlin 文件均自动合并。

解法取**并集**（两侧条目都保留）。校验方式：合并后每个文件的 key 集合必须等于
base ∪ ours ∪ theirs 且无重复 key（重复会让 AAPT 直接失败）。21 个 XML 全部通过。

## merge 打开的 fork 缺口（`152bc3908` 修复）

上游新增的代码写于 fork 定制消失之后，自动合并不会报错，只能由编译器暴露：

- `AuthLoginStorage.kt` 的 `when (platform)`（#554 新增）枚举了上游自己的三个平台，
  加上 fork 早先补的 `QqMusic` 分支，但**不含 `Kugou`** → 非穷尽，整个 lambda 类型崩掉。
  给 `Kugou` 返回与 `QqMusic` 相同的 `AuthLoginStorageStatus(false, false)`：
  两个平台都不走上游的 cookie repo，不存在兼容性存储告警。
- `HomeViewModel.probeKugouSource()` 仍从 `PlayerManager.kugouSession` 取会话，
  模块化后该属性属于 `AppContainer`；同文件 `buildHomeErrorMessage(e)` 也漏了
  后来新增的 `context` 形参。`KugouFmViewModel` 有同一处 `kugouSession` 引用。
  三处都按现有调用点（`ExploreViewModel` / `HomeViewModel` 其余分支）的写法对齐。

编译错误：基线 107 → merge 后 109 → 修复后 103。**相对基线新增 0 条**。
剩余 103 条全部可归因于 `kugouapi-kmp:1.0.5` 只有 KMP 元数据 jar（无 `.class`）：
`core/api/kugou/**` 93 条，其余 10 条是 SDK 类型缺失在 `KugouFmViewModel` /
`ExploreViewModel` 里的级联。

## 单元测试：本机不能作为门

全量模块单测在 merge 后共 1877 个失败（`:local` 1463/2467、`:sync` 219/1030），
但这是宿主环境而非同步造成的。对照组用 `git worktree` 固定在 merge 前的
`c32f9aa57`，同样只跑 `:playback:runtime`：

| | 测试数 | 失败数 |
|---|---|---|
| merge 前 | 1789 | 67 |
| merge 后 | 1953 | 79 |

按**失败方法名集合**做差集：merge 后新增的 12 个失败全部落在
`StorageAudioCacheClearTest`(11) 与 `RetainedImageCacheClearTest`(1)，
这两个类是上游 #571 新建的、merge 前根本不存在，异常链是
`Cache$CacheException → DatabaseIOException → SQLiteCantOpenDatabaseException`，
即 Robolectric 的原生 SQLite 在本机开不了 media3 的缓存库。
merge 前失败的 67 个方法在 merge 后仍然失败（差集为 0），
没有任何原本通过的测试转为失败。

`:common` / `:database` / `:download:logic` 的对照同样成立：
`FilePathGuardsTest > rejectsSymbolicLinksThatEscapeTheDirectory`
（本仓库 `core.symlinks=false`）、`ManagedDownloadReferenceIoTest`、
`NeriUserDataDatabaseVersionTest` 在 merge 前后都失败，且对照组失败更多
（`:download:logic` 16 vs 4、`:database` 5 vs 2）。

结论：本机能用的门是**编译错误差分**，不是单测红绿。

## 台账纠正

`fork-integration-patches/README.md` 里三条已过期记录按现状改写：
`ExploreViewModel.patch` 与 `PlayerManager.patch` 实为已完成（逐行核对缺的 14 行都
以改名后的形式存在），`NeriApp.patch` 判为不重贴（音质改走 route-state）。
`KugouModels.kt` / `QQMusicModels.kt` 里指向 `PlayerManager.isKugouTrack` /
`isQQMusicTrack` 的 KDoc 链接改为 `PlayerManager.KUGOU_SOURCE_TAG` /
`QQMUSIC_SOURCE_TAG`——那两个具名 helper 已被 `album` 前缀约定取代。
