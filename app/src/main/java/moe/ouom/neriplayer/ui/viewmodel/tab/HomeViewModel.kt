package moe.ouom.neriplayer.ui.viewmodel.tab

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.viewmodel.tab/HomeViewModel
 * Created: 2025/8/10
 */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.api.kugou.KugouChannelContent
import moe.ouom.neriplayer.core.api.kugou.loadKugouChannelContent
import moe.ouom.neriplayer.core.api.netease.mergeNeteaseSessionCookies
import moe.ouom.neriplayer.core.api.youtube.YouTubeMusicHomeShelf
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.auth.youtube.YouTubeAuthBundle
import moe.ouom.neriplayer.data.auth.youtube.buildRefreshObserverFingerprint
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.platform.netease.neteaseRadarCacheContext
import moe.ouom.neriplayer.util.platform.LanguageManager
import moe.ouom.neriplayer.core.logging.NPLogger
import java.io.IOException

private const val TAG = "NERI-HomeVM"
private const val HOME_NETEASE_SONG_LIMIT = 30
private const val HOME_NETEASE_PLAYLIST_LIMIT = 30
private const val HOME_PRIVATE_FM_MAX_BATCHES = 10
private const val HOME_MAX_FAILURE_BEFORE_WARNING = 3
private const val HOME_YT_MUSIC_PLAYLIST_LIMIT = 24
private const val HOME_INITIAL_LOAD_DEFER_MS = 250L
private const val HOME_NETEASE_PROBE_TIMEOUT_MS = 30_000L
private const val HOME_SECTION_LOAD_PARALLELISM = 6
private const val HOME_SECTION_LOAD_PARALLELISM_PER_GROUP = 2

private fun shouldFallbackRecommend(code: Int): Boolean = code == 301 || code == 50000005

internal fun homeSongFetchAttemptCount(source: NeteaseHomeSongSource): Int {
    return if (source == NeteaseHomeSongSource.PRIVATE_FM) {
        1
    } else {
        HOME_MAX_FAILURE_BEFORE_WARNING
    }
}

internal fun shouldRefreshNeteaseHome(
    loginChanged: Boolean,
    recommendationsBootstrapped: Boolean,
    accountContextChanged: Boolean = false
): Boolean = loginChanged || accountContextChanged || !recommendationsBootstrapped

internal fun shouldAcceptNeteaseRadarPlaylistLoadResult(
    requestGeneration: Long,
    activeGeneration: Long,
    requestRadarCacheContext: String,
    activeRadarCacheContext: String
): Boolean {
    return requestGeneration == activeGeneration &&
        requestRadarCacheContext == activeRadarCacheContext
}

internal fun shouldAcceptYouTubeMusicHomeLoadResult(
    requestGeneration: Long,
    activeGeneration: Long,
    requestAuthFingerprint: String,
    activeAuthFingerprint: String,
    internationalizationEnabled: Boolean
): Boolean {
    return requestGeneration == activeGeneration &&
        requestAuthFingerprint == activeAuthFingerprint &&
        internationalizationEnabled
}

internal fun shouldScheduleYouTubeMusicHomeRefresh(
    refreshPending: Boolean,
    offlineMode: Boolean,
    requestGeneration: Long,
    activeGeneration: Long,
    requestAuthFingerprint: String,
    activeAuthFingerprint: String,
    internationalizationEnabled: Boolean
): Boolean {
    return refreshPending &&
        !offlineMode &&
        shouldAcceptYouTubeMusicHomeLoadResult(
            requestGeneration = requestGeneration,
            activeGeneration = activeGeneration,
            requestAuthFingerprint = requestAuthFingerprint,
            activeAuthFingerprint = activeAuthFingerprint,
            internationalizationEnabled = internationalizationEnabled
        )
}

internal fun shouldHandleInitialNeteaseHomeCookieEmission(
    isFirstEmission: Boolean,
    initialCookies: Map<String, String>,
    emittedCookies: Map<String, String>
): Boolean = !isFirstEmission || initialCookies != emittedCookies

/**
 * 网易云源是否有可展示内容：任一展示板块有真实数据即视为有内容。
 * 雷达歌单失败时的硬编码兜底不算「有数据」，否则永远不会降级到下一个源。
 */
internal fun neteaseHomeHasData(state: HomeUiState): Boolean =
    state.playlistSections.any { it.section.items.isNotEmpty() } ||
        state.trendingSongSections.any { it.section.items.isNotEmpty() } ||
        state.radarSongSections.any { it.section.items.isNotEmpty() } ||
        (state.radarPlaylists.items.isNotEmpty() && !state.radarPlaylistsFromFallback)

/** 酷狗源是否有可展示内容：每日推荐/榜单/热门歌单任一非空即视为有内容。 */
internal fun kugouContentHasData(content: KugouChannelContent): Boolean =
    content.ranks.isNotEmpty() || content.playlists.isNotEmpty() || content.dailyRecommend.isNotEmpty()

internal enum class HomeSectionLoadGroup {
    PLAYLISTS,
    TRENDING_SONGS,
    RADAR_SONGS
}

/** 让首页三类分区并行加载时保持公平的网络预算和完成顺序 */
internal class HomeSectionLoadCoordinator(
    maxConcurrentLoads: Int = HOME_SECTION_LOAD_PARALLELISM,
    maxConcurrentLoadsPerGroup: Int = HOME_SECTION_LOAD_PARALLELISM_PER_GROUP
) {
    private val totalSemaphore = Semaphore(maxConcurrentLoads)
    private val groupSemaphores = HomeSectionLoadGroup.values().associateWith { group ->
        Semaphore(maxConcurrentLoadsPerGroup)
    }

    /** 在来源完成时立即交给状态层，避免慢来源阻塞已完成分区 */
    suspend fun <S, T> load(
        group: HomeSectionLoadGroup,
        sources: List<S>,
        fetch: suspend (S) -> T,
        onResult: (T) -> Unit
    ) {
        coroutineScope {
            val pending = sources.map { source ->
                async {
                    groupSemaphores.getValue(group).withPermit {
                        totalSemaphore.withPermit {
                            fetch(source)
                        }
                    }
                }
            }.toMutableList()
            while (pending.isNotEmpty()) {
                select<Unit> {
                    pending.forEach { deferred ->
                        deferred.onAwait { result ->
                            pending.remove(deferred)
                            onResult(result)
                        }
                    }
                }
            }
        }
    }
}

data class HomeSectionState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class HomeNeteaseSongSectionState(
    val source: NeteaseHomeSongSource,
    val section: HomeSectionState<SongItem> = HomeSectionState()
)

data class HomeNeteasePlaylistSectionState(
    val source: NeteaseHomePlaylistSource,
    val section: HomeSectionState<PlaylistSummary> = HomeSectionState()
)

/** 酷狗概念版首页内容（每日推荐 + 排行榜 + 热门歌单）。 */
data class HomeKugouSectionState(
    val content: KugouChannelContent? = null,
    val loading: Boolean = false,
    val error: String? = null
)

data class HomeUiState(
    val playlistSections: List<HomeNeteasePlaylistSectionState> = emptyList(),
    val trendingSongSections: List<HomeNeteaseSongSectionState> = emptyList(),
    val radarSongSections: List<HomeNeteaseSongSectionState> = emptyList(),
    val radarPlaylists: HomeSectionState<PlaylistSummary> = HomeSectionState(),
    val ytMusicPlaylists: HomeSectionState<YouTubeMusicPlaylist> = HomeSectionState(),
    val ytMusicHomeShelves: HomeSectionState<YouTubeMusicHomeShelf> = HomeSectionState(),
    val hasLogin: Boolean = false,
    val internationalizationEnabled: Boolean = false,
    val activeSource: HomeContentSource? = null,
    val kugouSections: HomeKugouSectionState = HomeKugouSectionState(),
    /** 雷达歌单当前是否为失败后的硬编码兜底（不计入源「有数据」）。 */
    val radarPlaylistsFromFallback: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppContainer.neteaseCookieRepo
    private val client = AppContainer.neteaseClient
    private val youtubeAuthRepo = AppContainer.youtubeAuthRepo

    private val initialRecommendCookies = repo.withCurrentCookies { cookies ->
        client.setPersistedCookies(cookies)
        cookies
    }
    private var hasRecommendLogin = !initialRecommendCookies["MUSIC_U"].isNullOrBlank()
    private val _uiState = MutableStateFlow(
        createHomeUiState(hasRecommendLogin, loading = true).copy(
            activeSource = DEFAULT_HOME_CONTENT_SOURCE_ORDER.first()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

    private var playlistJob: Job? = null
    private var hotSongsJob: Job? = null
    private var radarSongsJob: Job? = null
    private val homeSectionLoadCoordinator = HomeSectionLoadCoordinator()
    private var radarPlaylistsJob: Job? = null
    private var homeContentJob: Job? = null
    private var ytMusicHomeJob: Job? = null
    private var ytMusicHomeRefreshPending = false
    private var ytMusicHomeLoadGeneration: Long = 0L
    private var homeRecommendationsBootstrapped = false
    private var lastYouTubeAuthFingerprint: String? = null
    private var lastNeteaseRadarCacheContext = neteaseRadarCacheContext(repo.getCookiesOnce())
    private var radarPlaylistLoadGeneration: Long = 0L
    private var offlineMode = false

    private fun localizedAppContext() = LanguageManager.applyLanguage(getApplication())

    private fun createHomeUiState(hasLogin: Boolean, loading: Boolean): HomeUiState {
        return HomeUiState(
            playlistSections = createPlaylistSections(hasLogin, loading),
            trendingSongSections = createSongSections(
                sources = NeteaseHomeTrendingSongSources,
                hasLogin = hasLogin,
                loading = loading
            ),
            radarSongSections = createSongSections(
                sources = NeteaseHomeRadarSongSources,
                hasLogin = hasLogin,
                loading = loading
            ),
            radarPlaylists = HomeSectionState(loading = loading),
            hasLogin = hasLogin
        )
    }

    private fun createSongSections(
        sources: List<NeteaseHomeSongSource>,
        hasLogin: Boolean,
        loading: Boolean
    ): List<HomeNeteaseSongSectionState> {
        return availableNeteaseHomeSongSources(sources, hasLogin).map { source ->
            HomeNeteaseSongSectionState(source = source, section = HomeSectionState(loading = loading))
        }
    }

    private fun createPlaylistSections(
        hasLogin: Boolean,
        loading: Boolean
    ): List<HomeNeteasePlaylistSectionState> {
        return availableNeteaseHomePlaylistSources(NeteaseHomePlaylistSources, hasLogin).map { source ->
            HomeNeteasePlaylistSectionState(
                source = source,
                section = HomeSectionState(loading = loading)
            )
        }
    }

    private fun buildSongSectionsForRefresh(
        current: List<HomeNeteaseSongSectionState>,
        sources: List<NeteaseHomeSongSource>
    ): List<HomeNeteaseSongSectionState> {
        val previousBySource = current.associateBy { it.source }
        return availableNeteaseHomeSongSources(sources, hasRecommendLogin).map { source ->
            val previous = previousBySource[source]?.section ?: HomeSectionState()
            HomeNeteaseSongSectionState(
                source = source,
                section = previous.copy(loading = true, error = null)
            )
        }
    }

    private fun buildPlaylistSectionsForRefresh(
        current: List<HomeNeteasePlaylistSectionState>
    ): List<HomeNeteasePlaylistSectionState> {
        val previousBySource = current.associateBy { it.source }
        return availableNeteaseHomePlaylistSources(NeteaseHomePlaylistSources, hasRecommendLogin)
            .map { source ->
                val previous = previousBySource[source]?.section ?: HomeSectionState()
                HomeNeteasePlaylistSectionState(
                    source = source,
                    section = previous.copy(loading = true, error = null)
                )
            }
    }

    private fun clearSongSectionLoading(
        sections: List<HomeNeteaseSongSectionState>
    ): List<HomeNeteaseSongSectionState> {
        return sections.map { sectionState ->
            sectionState.copy(section = sectionState.section.copy(loading = false, error = null))
        }
    }

    private fun clearPlaylistSectionLoading(
        sections: List<HomeNeteasePlaylistSectionState>
    ): List<HomeNeteasePlaylistSectionState> {
        return sections.map { sectionState ->
            sectionState.copy(section = sectionState.section.copy(loading = false, error = null))
        }
    }

    private fun replaceSongSection(
        sections: List<HomeNeteaseSongSectionState>,
        updated: HomeNeteaseSongSectionState
    ): List<HomeNeteaseSongSectionState> {
        return sections.map { sectionState ->
            if (sectionState.source == updated.source) updated else sectionState
        }
    }

    private fun replacePlaylistSection(
        sections: List<HomeNeteasePlaylistSectionState>,
        updated: HomeNeteasePlaylistSectionState
    ): List<HomeNeteasePlaylistSectionState> {
        return sections.map { sectionState ->
            if (sectionState.source == updated.source) updated else sectionState
        }
    }

    init {
        lastYouTubeAuthFingerprint = buildYouTubeAuthFingerprint(youtubeAuthRepo.getAuthOnce())

        // 观察国际化设置变化, 切换推荐源
        viewModelScope.launch {
            combine(
                AppContainer.settingsRepo.internationalizationEnabledFlow,
                AppContainer.settingsRepo.youtubeEnabledFlow
            ) { internationalizationEnabled, youtubeEnabled ->
                internationalizationEnabled to youtubeEnabled
            }.collect { (internationalizationEnabled, youtubeEnabled) ->
                val useYouTubeHome = internationalizationEnabled && youtubeEnabled
                NPLogger.d(
                    TAG,
                    "home source updated: international=$internationalizationEnabled, youtube=$youtubeEnabled"
                )
                _uiState.value = _uiState.value.copy(
                    internationalizationEnabled = useYouTubeHome
                )
                if (useYouTubeHome) {
                    refreshYtMusicHome()
                } else {
                    cancelYouTubeHomeJobs()
                    _uiState.value = _uiState.value.copy(
                        ytMusicPlaylists = HomeSectionState(),
                        ytMusicHomeShelves = HomeSectionState()
                    )
                }
            }
        }

        viewModelScope.launch {
            AppContainer.youtubeAuthRepo.authFlow.drop(1).collect { bundle ->
                val nextFingerprint = buildYouTubeAuthFingerprint(bundle)
                if (nextFingerprint == lastYouTubeAuthFingerprint) {
                    return@collect
                }
                lastYouTubeAuthFingerprint = nextFingerprint
                NPLogger.d(
                    TAG,
                    "youtube auth changed: hasEffectiveAuth=${bundle.hasEffectiveAuth()}, hasCookieContext=${bundle.hasYouTubeMusicCookieContext()}, intl=${_uiState.value.internationalizationEnabled}"
                )
                if (!_uiState.value.internationalizationEnabled) {
                    return@collect
                }
                cancelYouTubeHomeJobs()
                if (!bundle.hasYouTubeMusicCookieContext()) {
                    NPLogger.d(TAG, "youtube auth cleared, reset home YouTube sections")
                    _uiState.value = _uiState.value.copy(
                        ytMusicPlaylists = HomeSectionState(),
                        ytMusicHomeShelves = HomeSectionState()
                    )
                    return@collect
                }
                refreshYtMusicHome()
            }
        }

        // 登录后自动刷新首页推荐歌单
        viewModelScope.launch {
            var isFirstCookieEmission = true
            repo.cookieFlow.collect { raw ->
                if (!repo.withCurrentCookiesIfMatches(raw) { currentCookies ->
                        client.setPersistedCookies(currentCookies)
                    }
                ) {
                    return@collect
                }
                val shouldHandleEmission = shouldHandleInitialNeteaseHomeCookieEmission(
                    isFirstEmission = isFirstCookieEmission,
                    initialCookies = initialRecommendCookies,
                    emittedCookies = raw
                )
                isFirstCookieEmission = false
                if (!shouldHandleEmission) return@collect
                NPLogger.d(TAG, "cookieFlow updated: keys=${raw.keys.joinToString()}")
                val nextHasLogin = !raw["MUSIC_U"].isNullOrBlank()
                val nextRadarCacheContext = neteaseRadarCacheContext(raw)
                val accountContextChanged =
                    lastNeteaseRadarCacheContext != nextRadarCacheContext
                lastNeteaseRadarCacheContext = nextRadarCacheContext
                val loginChanged = hasRecommendLogin != nextHasLogin
                hasRecommendLogin = nextHasLogin
                if (loginChanged) {
                    _uiState.value = _uiState.value.copy(hasLogin = nextHasLogin)
                }
                if (accountContextChanged) {
                    _uiState.update { state ->
                        state.copy(
                            radarPlaylists = HomeSectionState(),
                            radarPlaylistsFromFallback = false
                        )
                    }
                }
                if (
                    shouldRefreshNeteaseHome(
                        loginChanged = loginChanged,
                        recommendationsBootstrapped = homeRecommendationsBootstrapped,
                        accountContextChanged = accountContextChanged
                    )
                ) {
                    homeRecommendationsBootstrapped = true
                    refreshHomeContent()
                }
            }
        }
        viewModelScope.launch {
            delay(HOME_INITIAL_LOAD_DEFER_MS)
            if (!homeRecommendationsBootstrapped) {
                homeRecommendationsBootstrapped = true
                refreshHomeContent()
            }
        }
        // 首页内容源排序变化时按新顺序重新探测（主源恢复后自然回切）
        viewModelScope.launch {
            AppContainer.settingsRepo.homeContentSourceOrderFlow.drop(1).collect { order ->
                NPLogger.d(TAG, "home content source order changed: $order")
                refreshHomeContent()
            }
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        if (offlineMode == enabled) return

        NPLogger.d(TAG, "setOfflineMode: $enabled")
        offlineMode = enabled
        if (!enabled) return

        cancelHomeNetworkJobs()
        _uiState.update { state ->
            state.copy(
                playlistSections = clearPlaylistSectionLoading(state.playlistSections),
                trendingSongSections = clearSongSectionLoading(state.trendingSongSections),
                radarSongSections = clearSongSectionLoading(state.radarSongSections),
                radarPlaylists = state.radarPlaylists.copy(loading = false, error = null),
                kugouSections = state.kugouSections.copy(loading = false, error = null),
                ytMusicPlaylists = state.ytMusicPlaylists.copy(loading = false, error = null),
                ytMusicHomeShelves = state.ytMusicHomeShelves.copy(loading = false, error = null)
            )
        }
    }

    private fun cancelHomeNetworkJobs() {
        homeContentJob?.cancel()
        cancelNeteaseSectionJobs()
        cancelYouTubeHomeJobs()
    }

    private fun cancelNeteaseSectionJobs() {
        playlistJob?.cancel()
        hotSongsJob?.cancel()
        radarSongsJob?.cancel()
        radarPlaylistsJob?.cancel()
    }

    private fun cancelYouTubeHomeJobs() {
        ytMusicHomeLoadGeneration += 1L
        ytMusicHomeJob?.cancel()
        ytMusicHomeJob = null
        ytMusicHomeRefreshPending = false
    }

    fun refreshNeteaseHome() {
        refreshHomeContent()
    }

    /**
     * 首页内容源刷新入口：按「首页展示排序」设置依次探测内容源。
     * 当前源所有展示接口均无数据（失败或为空）时自动降级到下一个源；
     * 每次刷新都会重新按序探测，主源恢复数据后自然回切。
     * 探测期间 activeSource 指向当前源，便于 UI 立即展示对应 loading/error。
     */
    private fun refreshHomeContent() {
        if (offlineMode) return

        homeContentJob?.cancel()
        cancelNeteaseSectionJobs()
        homeContentJob = viewModelScope.launch {
            val orderedSources = parseHomeContentSourceOrder(
                AppContainer.settingsRepo.homeContentSourceOrderFlow.first()
            )
            NPLogger.d(TAG, "refreshHomeContent: sources=$orderedSources")
            for (source in orderedSources) {
                _uiState.update { state -> state.copy(activeSource = source) }
                val hasData = when (source) {
                    HomeContentSource.NETEASE -> probeNeteaseSource()
                    HomeContentSource.KUGOU -> probeKugouSource()
                }
                if (hasData) {
                    NPLogger.d(TAG, "refreshHomeContent: active source=$source")
                    return@launch
                }
                if (source == HomeContentSource.NETEASE) {
                    cancelNeteaseSectionJobs()
                    _uiState.update { state ->
                        state.copy(
                            playlistSections = emptyList(),
                            trendingSongSections = emptyList(),
                            radarSongSections = emptyList(),
                            radarPlaylists = HomeSectionState(),
                            radarPlaylistsFromFallback = false
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(kugouSections = HomeKugouSectionState())
                    }
                }
            }
            NPLogger.d(TAG, "refreshHomeContent: no source has data")
            _uiState.update { state -> state.copy(activeSource = null) }
        }
    }

    /** 加载网易云全部板块并等待完成，返回是否有可展示内容。 */
    private suspend fun probeNeteaseSource(): Boolean {
        refreshRecommend()
        loadHomeRecommendations(force = true)
        refreshRadarPlaylists()
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
    }

    /** 拉首页推荐歌单 */
    fun refreshRecommend() {
        if (offlineMode) return

        val sources = availableNeteaseHomePlaylistSources(
            candidates = NeteaseHomePlaylistSources,
            hasLogin = hasRecommendLogin
        )
        if (sources.isEmpty()) {
            _uiState.update { state -> state.copy(playlistSections = emptyList()) }
            return
        }
        NPLogger.d(TAG, "refreshRecommend start: sources=$sources, hasLogin=$hasRecommendLogin")
        playlistJob?.cancel()
        _uiState.update { state ->
            state.copy(
                playlistSections = buildPlaylistSectionsForRefresh(state.playlistSections),
                hasLogin = hasRecommendLogin
            )
        }
        playlistJob = viewModelScope.launch {
            homeSectionLoadCoordinator.load(
                group = HomeSectionLoadGroup.PLAYLISTS,
                sources = sources,
                fetch = ::fetchPlaylistSection
            ) { section ->
                _uiState.update { state ->
                    state.copy(
                        playlistSections = replacePlaylistSection(
                            state.playlistSections,
                            section
                        )
                    )
                }
            }
        }
    }

    fun loadHomeRecommendations(force: Boolean = false) {
        if (offlineMode) return

        val state = _uiState.value
        if (!force) {
            val songSections = state.radarSongSections + state.trendingSongSections
            val alreadyLoaded = songSections.isNotEmpty() &&
                songSections.all { it.section.items.isNotEmpty() || it.section.error != null }
            val loading = songSections.any { it.section.loading }
            if (alreadyLoaded || loading) return
        }

        refreshRadarSongs()
        refreshHotSongs()
    }

    /** 刷新首页热门歌曲板块，保留各来源独立的加载和错误状态 */
    private fun refreshHotSongs() {
        if (offlineMode) return

        val sources = availableNeteaseHomeSongSources(
            candidates = NeteaseHomeTrendingSongSources,
            hasLogin = hasRecommendLogin
        )
        if (sources.isEmpty()) {
            _uiState.update { state -> state.copy(trendingSongSections = emptyList()) }
            return
        }
        NPLogger.d(TAG, "refreshHotSongs start: sources=$sources")
        hotSongsJob?.cancel()
        _uiState.update { state ->
            state.copy(
                trendingSongSections = buildSongSectionsForRefresh(
                    current = state.trendingSongSections,
                    sources = NeteaseHomeTrendingSongSources
                ),
                hasLogin = hasRecommendLogin
            )
        }
        hotSongsJob = viewModelScope.launch {
            homeSectionLoadCoordinator.load(
                group = HomeSectionLoadGroup.TRENDING_SONGS,
                sources = sources,
                fetch = { source -> fetchSongSection("refreshHotSongs", source) }
            ) { section ->
                _uiState.update { state ->
                    state.copy(
                        trendingSongSections = replaceSongSection(
                            state.trendingSongSections,
                            section
                        )
                    )
                }
            }
        }
    }

    /** 刷新首页雷达歌曲板块，保留各来源独立的加载和错误状态 */
    private fun refreshRadarSongs() {
        if (offlineMode) return

        val sources = availableNeteaseHomeSongSources(
            candidates = NeteaseHomeRadarSongSources,
            hasLogin = hasRecommendLogin
        )
        if (sources.isEmpty()) {
            _uiState.update { state -> state.copy(radarSongSections = emptyList()) }
            return
        }
        NPLogger.d(TAG, "refreshRadarSongs start: sources=$sources")
        radarSongsJob?.cancel()
        _uiState.update { state ->
            state.copy(
                radarSongSections = buildSongSectionsForRefresh(
                    current = state.radarSongSections,
                    sources = NeteaseHomeRadarSongSources
                ),
                hasLogin = hasRecommendLogin
            )
        }
        radarSongsJob = viewModelScope.launch {
            homeSectionLoadCoordinator.load(
                group = HomeSectionLoadGroup.RADAR_SONGS,
                sources = sources,
                fetch = { source -> fetchSongSection("refreshRadarSongs", source) }
            ) { section ->
                _uiState.update { state ->
                    state.copy(
                        radarSongSections = replaceSongSection(
                            state.radarSongSections,
                            section
                        )
                    )
                }
            }
        }
    }

    private fun refreshRadarPlaylists() {
        if (offlineMode) return

        NPLogger.d(TAG, "refreshRadarPlaylists start")
        radarPlaylistsJob?.cancel()
        radarPlaylistLoadGeneration += 1L
        val loadGeneration = radarPlaylistLoadGeneration
        val requestRadarCacheContext = lastNeteaseRadarCacheContext
        val previous = _uiState.value.radarPlaylists
        _uiState.value = _uiState.value.copy(
            radarPlaylists = previous.copy(loading = true, error = null)
        )
        radarPlaylistsJob = viewModelScope.launch {
            when (val result = fetchWithRetry("refreshRadarPlaylists") {
                loadRadarPlaylistSummaries(requestRadarCacheContext)
            }) {
                is RetryLoadResult.Success -> {
                    if (!isCurrentRadarPlaylistLoad(loadGeneration, requestRadarCacheContext)) {
                        return@launch
                    }
                    NPLogger.d(TAG, "refreshRadarPlaylists success: count=${result.items.size}")
                    _uiState.value = _uiState.value.copy(
                        radarPlaylists = HomeSectionState(items = result.items),
                        radarPlaylistsFromFallback = false
                    )
                }
                is RetryLoadResult.Failure -> {
                    if (!isCurrentRadarPlaylistLoad(loadGeneration, requestRadarCacheContext)) {
                        return@launch
                    }
                    NPLogger.e(TAG, "refreshRadarPlaylists failed", result.throwable)
                    _uiState.value = _uiState.value.copy(
                        radarPlaylists = HomeSectionState(
                            items = NeteaseRadarPlaylistDefinitions.map { it.toPlaylistSummary() }
                        ),
                        radarPlaylistsFromFallback = true
                    )
                }
            }
        }
    }

    private fun isCurrentRadarPlaylistLoad(
        loadGeneration: Long,
        requestRadarCacheContext: String
    ): Boolean {
        return shouldAcceptNeteaseRadarPlaylistLoadResult(
            requestGeneration = loadGeneration,
            activeGeneration = radarPlaylistLoadGeneration,
            requestRadarCacheContext = requestRadarCacheContext,
            activeRadarCacheContext = neteaseRadarCacheContext(repo.getCookiesOnce())
        )
    }

    /** 拉取一次 YouTube Music 首页快照并同时更新歌单和推荐栏 */
    fun refreshYtMusicHome() {
        requestYtMusicHomeRefresh(queueIfLoading = true)
    }

    /** 兼容旧调用，复用正在加载的首页快照 */
    fun refreshYtMusicPlaylists() {
        requestYtMusicHomeRefresh(queueIfLoading = false)
    }

    /** 兼容旧调用，复用正在加载的首页快照 */
    fun refreshYtMusicHomeFeed() {
        requestYtMusicHomeRefresh(queueIfLoading = false)
    }

    private fun requestYtMusicHomeRefresh(queueIfLoading: Boolean) {
        if (offlineMode || !_uiState.value.internationalizationEnabled) return

        if (ytMusicHomeJob?.isActive == true) {
            if (queueIfLoading) {
                ytMusicHomeRefreshPending = true
                NPLogger.d(TAG, "refreshYtMusicHome coalesced while loading")
            } else {
                NPLogger.d(TAG, "refreshYtMusicHome reused while loading")
            }
            return
        }
        ytMusicHomeRefreshPending = false
        val requestGeneration = ++ytMusicHomeLoadGeneration
        val requestAuthFingerprint = buildYouTubeAuthFingerprint(youtubeAuthRepo.getAuthOnce())
        NPLogger.d(TAG, "refreshYtMusicHome start")
        _uiState.update { state ->
            state.copy(
                ytMusicPlaylists = state.ytMusicPlaylists.copy(loading = true, error = null),
                ytMusicHomeShelves = state.ytMusicHomeShelves.copy(loading = true, error = null)
            )
        }
        ytMusicHomeJob = viewModelScope.launch {
            try {
                when (val result = fetchWithRetry("refreshYtMusicHome") {
                    listOf(
                        withContext(Dispatchers.IO) {
                            loadYouTubeMusicHomeSnapshot(
                                playlistLimit = HOME_YT_MUSIC_PLAYLIST_LIMIT,
                                loadShelves = {
                                    AppContainer.youtubeMusicClient.getHomeFeed(
                                        fillShelfContinuations = false,
                                        requireLogin = true
                                    )
                                }
                            )
                        }
                    )
                }) {
                    is RetryLoadResult.Success -> {
                        if (!isCurrentYouTubeMusicHomeLoad(
                                requestGeneration = requestGeneration,
                                requestAuthFingerprint = requestAuthFingerprint
                            )
                        ) {
                            NPLogger.d(TAG, "refreshYtMusicHome discarded stale result")
                            return@launch
                        }
                        val snapshot = result.items.single()
                        NPLogger.d(
                            TAG,
                            "refreshYtMusicHome success: shelves=${snapshot.shelves.size}, playlists=${snapshot.playlists.size}"
                        )
                        _uiState.update { state ->
                            state.copy(
                                ytMusicPlaylists = HomeSectionState(items = snapshot.playlists),
                                ytMusicHomeShelves = HomeSectionState(items = snapshot.shelves)
                            )
                        }
                    }
                    is RetryLoadResult.Failure -> {
                        if (!isCurrentYouTubeMusicHomeLoad(
                                requestGeneration = requestGeneration,
                                requestAuthFingerprint = requestAuthFingerprint
                            )
                        ) {
                            NPLogger.d(TAG, "refreshYtMusicHome discarded stale failure")
                            return@launch
                        }
                        NPLogger.e(TAG, "refreshYtMusicHome failed", result.throwable)
                        val error = buildHomeErrorMessage(result.throwable)
                        _uiState.update { state ->
                            state.copy(
                                ytMusicPlaylists = state.ytMusicPlaylists.copy(
                                    loading = false,
                                    error = error
                                ),
                                ytMusicHomeShelves = state.ytMusicHomeShelves.copy(
                                    loading = false,
                                    error = error
                                )
                            )
                        }
                    }
                }
            } finally {
                val completedJob = coroutineContext[Job]
                if (ytMusicHomeJob === completedJob) {
                    ytMusicHomeJob = null
                    val shouldRefreshPendingLoad = shouldScheduleYouTubeMusicHomeRefresh(
                        refreshPending = ytMusicHomeRefreshPending,
                        offlineMode = offlineMode,
                        requestGeneration = requestGeneration,
                        activeGeneration = ytMusicHomeLoadGeneration,
                        requestAuthFingerprint = requestAuthFingerprint,
                        activeAuthFingerprint = buildYouTubeAuthFingerprint(
                            youtubeAuthRepo.getAuthOnce()
                        ),
                        internationalizationEnabled = _uiState.value.internationalizationEnabled
                    )
                    ytMusicHomeRefreshPending = false
                    if (shouldRefreshPendingLoad) {
                        refreshYtMusicHome()
                    }
                }
            }
        }
    }

    private fun isCurrentYouTubeMusicHomeLoad(
        requestGeneration: Long,
        requestAuthFingerprint: String
    ): Boolean {
        return shouldAcceptYouTubeMusicHomeLoadResult(
            requestGeneration = requestGeneration,
            activeGeneration = ytMusicHomeLoadGeneration,
            requestAuthFingerprint = requestAuthFingerprint,
            activeAuthFingerprint = buildYouTubeAuthFingerprint(youtubeAuthRepo.getAuthOnce()),
            internationalizationEnabled = _uiState.value.internationalizationEnabled
        )
    }

    private suspend fun <T> fetchWithRetry(
        name: String,
        maxAttempts: Int = HOME_MAX_FAILURE_BEFORE_WARNING,
        fetch: suspend () -> List<T>
    ): RetryLoadResult<T> {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            try {
                val items = fetch()
                if (attempt > 0) {
                    NPLogger.d(
                        TAG,
                        "$name recovered on attempt ${attempt + 1}: count=${items.size}"
                    )
                }
                return RetryLoadResult.Success(items)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                lastError = e
                NPLogger.w(
                    TAG,
                    "$name attempt ${attempt + 1}/$maxAttempts failed: ${e.message}"
                )
            }
        }
        return RetryLoadResult.Failure(lastError ?: IllegalStateException("Unknown error"))
    }

    private fun buildHomeErrorMessage(error: Throwable): String {
        val localizedContext = localizedAppContext()
        return when (error) {
            is IOException -> localizedContext.getString(
                R.string.home_error_network,
                error.message ?: error.javaClass.simpleName
            )
            is ApiCodeException -> {
                if (error.code == 50000005) {
                    localizedContext.getString(R.string.home_login_required)
                } else {
                    localizedContext.getString(R.string.error_api_code, error.code)
                }
            }
            else -> localizedContext.getString(
                R.string.home_error_unknown,
                error.message ?: error.javaClass.simpleName
            )
        }
    }

    private suspend fun parseRecommendOnWorker(raw: String): List<PlaylistSummary> =
        withContext(Dispatchers.Default) {
            parseNeteaseHomePlaylists(raw, limit = HOME_NETEASE_PLAYLIST_LIMIT)
        }

    private suspend fun parseSongsOnWorker(raw: String): List<SongItem> =
        withContext(Dispatchers.Default) {
            parseNeteaseHomeSongs(raw, limit = HOME_NETEASE_SONG_LIMIT)
        }

    private suspend fun fetchSongSection(
        name: String,
        source: NeteaseHomeSongSource
    ): HomeNeteaseSongSectionState {
        return when (
            val result = fetchWithRetry(
                name = "$name/$source",
                maxAttempts = homeSongFetchAttemptCount(source)
            ) {
                fetchSongSource(source)
            }
        ) {
            is RetryLoadResult.Success -> {
                NPLogger.d(
                    TAG,
                    "$name success: source=$source, count=${result.items.size}"
                )
                HomeNeteaseSongSectionState(
                    source = source,
                    section = HomeSectionState(items = result.items)
                )
            }
            is RetryLoadResult.Failure -> {
                NPLogger.e(TAG, "$name failed: source=$source", result.throwable)
                HomeNeteaseSongSectionState(
                    source = source,
                    section = HomeSectionState(error = buildHomeErrorMessage(result.throwable))
                )
            }
        }
    }

    private suspend fun fetchPlaylistSection(
        source: NeteaseHomePlaylistSource
    ): HomeNeteasePlaylistSectionState {
        return when (val result = fetchWithRetry("refreshRecommend/$source") {
            try {
                fetchPlaylistSource(source)
            } catch (e: ApiCodeException) {
                if (
                    source == NeteaseHomePlaylistSource.PERSONALIZED &&
                    hasRecommendLogin &&
                    shouldFallbackRecommend(e.code)
                ) {
                    NPLogger.w(
                        TAG,
                        "refreshRecommend fallback to anonymous due to api_code=${e.code}"
                    )
                    val fallbackRaw = withContext(Dispatchers.IO) {
                        client.getRecommendedPlaylists(
                            limit = HOME_NETEASE_PLAYLIST_LIMIT,
                            usePersistedCookies = false
                        )
                    }
                    parseRecommendOnWorker(fallbackRaw)
                } else {
                    throw e
                }
            }
        }) {
            is RetryLoadResult.Success -> {
                NPLogger.d(
                    TAG,
                    "refreshRecommend success: source=$source, count=${result.items.size}"
                )
                HomeNeteasePlaylistSectionState(
                    source = source,
                    section = HomeSectionState(items = result.items)
                )
            }
            is RetryLoadResult.Failure -> {
                NPLogger.e(TAG, "refreshRecommend failed: source=$source", result.throwable)
                HomeNeteasePlaylistSectionState(
                    source = source,
                    section = HomeSectionState(error = buildHomeErrorMessage(result.throwable))
                )
            }
        }
    }

    private suspend fun fetchSongSource(source: NeteaseHomeSongSource): List<SongItem> {
        if (source == NeteaseHomeSongSource.PRIVATE_FM) {
            return fetchPrivateFmSongs()
        }
        val raw = withContext(Dispatchers.IO) {
            fetchSongSourceRaw(source)
        }
        return parseSongsOnWorker(raw)
    }

    /** 将首页来源的请求参数集中在一起，避免登录态契约在调用点分散 */
    private fun fetchSongSourceRaw(source: NeteaseHomeSongSource): String {
        return when (source) {
            NeteaseHomeSongSource.TOP_SOARING -> client.getPlaylistDetail(
                playlistId = NETEASE_TOPLIST_SOARING_ID,
                n = HOME_NETEASE_SONG_LIMIT,
                s = 0
            )
            NeteaseHomeSongSource.PERSONAL_RADAR -> client.getPlaylistDetail(
                playlistId = NETEASE_PRIVATE_RADAR_PLAYLIST_ID,
                n = HOME_NETEASE_SONG_LIMIT,
                s = 0
            )
            NeteaseHomeSongSource.DAILY_RECOMMEND -> client.getDailyRecommendedSongs(
                afresh = true
            )
            NeteaseHomeSongSource.PRIVATE_FM -> client.getPersonalFmSongs()
            NeteaseHomeSongSource.PERSONALIZED_NEW_SONGS -> client.getPersonalizedNewSongs(
                limit = HOME_NETEASE_SONG_LIMIT,
                usePersistedCookies = hasRecommendLogin
            )
            NeteaseHomeSongSource.TOP_HOT -> client.getPlaylistDetail(
                playlistId = NETEASE_TOPLIST_HOT_ID,
                n = HOME_NETEASE_SONG_LIMIT,
                s = 0
            )
            NeteaseHomeSongSource.TOP_NEW -> client.getPlaylistDetail(
                playlistId = NETEASE_TOPLIST_NEW_ID,
                n = HOME_NETEASE_SONG_LIMIT,
                s = 0
            )
        }
    }

    private suspend fun fetchPrivateFmSongs(): List<SongItem> {
        var songs = emptyList<SongItem>()
        for (batchIndex in 0 until HOME_PRIVATE_FM_MAX_BATCHES) {
            val raw = withContext(Dispatchers.IO) {
                client.getPersonalFmSongs()
            }
            val batch = parseSongsOnWorker(raw)
            if (batch.isEmpty()) break

            val merged = appendUniqueNeteaseHomeSongs(
                current = songs,
                next = batch,
                limit = HOME_NETEASE_SONG_LIMIT
            )
            if (merged.size == songs.size) {
                NPLogger.d(TAG, "private FM returned no new songs at batch=$batchIndex")
                break
            }
            songs = merged
            if (songs.size >= HOME_NETEASE_SONG_LIMIT) break
        }
        return songs
    }

    private suspend fun fetchPlaylistSource(
        source: NeteaseHomePlaylistSource
    ): List<PlaylistSummary> {
        val raw = withContext(Dispatchers.IO) {
            when (source) {
                NeteaseHomePlaylistSource.PERSONALIZED -> client.getRecommendedPlaylists(
                    limit = HOME_NETEASE_PLAYLIST_LIMIT,
                    usePersistedCookies = hasRecommendLogin
                )
                NeteaseHomePlaylistSource.DAILY_RESOURCE -> client.getDailyRecommendedPlaylists()
                NeteaseHomePlaylistSource.HIGH_QUALITY -> client.getHighQualityPlaylists(
                    cat = "全部",
                    limit = HOME_NETEASE_PLAYLIST_LIMIT,
                    before = 0L
                )
                NeteaseHomePlaylistSource.HOT_PLAYLISTS -> client.getTopPlaylists(
                    cat = "全部",
                    order = "hot",
                    limit = HOME_NETEASE_PLAYLIST_LIMIT,
                    usePersistedCookies = hasRecommendLogin
                )
                NeteaseHomePlaylistSource.ACG_PLAYLISTS -> client.getTopPlaylists(
                    cat = "ACG",
                    order = "hot",
                    limit = HOME_NETEASE_PLAYLIST_LIMIT,
                    usePersistedCookies = hasRecommendLogin
                )
            }
        }
        return parseRecommendOnWorker(raw)
    }

    private suspend fun loadRadarPlaylistSummaries(
        expectedRadarCacheContext: String
    ): List<PlaylistSummary> {
        val hasLogin = hasRecommendLogin
        if (hasLogin) {
            prepareNeteaseRadarSession()
        }
        val summaries = loadNeteaseRadarPlaylistSummaries(
            definitions = NeteaseRadarPlaylistDefinitions,
            loadMetadata = { playlistId ->
                client.getRadarPlaylistMetadataCancellable(playlistId)
            },
            onLoadFailure = { definition, error ->
                NPLogger.w(TAG, "radar metadata failed: playlistId=${definition.id}, error=${error.message}")
            }
        )
        if (hasLogin) {
            persistNeteaseRadarSessionCookies(expectedRadarCacheContext)
        }
        return summaries
    }

    private suspend fun prepareNeteaseRadarSession() {
        try {
            withContext(Dispatchers.IO) {
                client.ensurePersonalizedSession()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            NPLogger.w(TAG, "radar session preheat failed: ${error.message}")
        }
    }

    private fun persistNeteaseRadarSessionCookies(expectedRadarCacheContext: String) {
        val persisted = repo.getCookiesOnce()
        if (neteaseRadarCacheContext(persisted) != expectedRadarCacheContext) return
        val updated = mergeNeteaseSessionCookies(
            persistedCookies = persisted,
            runtimeCookies = client.getNeteaseRequestCookies()
        )
        if (
            updated != persisted &&
                repo.saveCookiesIfCurrent(
                    expectedCookies = persisted,
                    cookies = updated
                )
        ) {
            NPLogger.d(TAG, "persisted NetEase radar session context")
        }
    }

    private fun buildYouTubeAuthFingerprint(bundle: YouTubeAuthBundle): String {
        return bundle.buildRefreshObserverFingerprint()
    }

    private fun YouTubeAuthBundle.hasYouTubeMusicCookieContext(): Boolean {
        return hasSavedAuthMaterial()
    }

    private sealed interface RetryLoadResult<out T> {
        data class Success<T>(val items: List<T>) : RetryLoadResult<T>
        data class Failure(val throwable: Throwable) : RetryLoadResult<Nothing>
    }
}
