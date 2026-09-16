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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab/KugouFmViewModel
 */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.core.api.kugou.KugouFmMode
import moe.ouom.neriplayer.core.api.kugou.KugouFmParams
import moe.ouom.neriplayer.core.api.kugou.KugouFmSongPool
import moe.ouom.neriplayer.core.api.kugou.fetchKugouPersonalFm
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.model.SongItem
import top.ghhccghk.multiplatform.kugouapi.model.FmAction

private const val TAG = "KugouFmVM"
private const val FM_BUFFER_REFILL_THRESHOLD = 4

/** 私人FM UI 状态。 */
data class KugouFmUiState(
    val loggedIn: Boolean = false,
    val mode: KugouFmMode = KugouFmMode.NORMAL,
    val songPool: KugouFmSongPool = KugouFmSongPool.TASTE,
    val buffer: List<SongItem> = emptyList(),
    val currentTrack: SongItem? = null,
    val isPlaying: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * 酷狗私人FM ViewModel：
 * 管理推荐 buffer、模式/曲库切换、播放与踩操作。
 * buffer ≤4 首时自动补充；踩时带当前歌 hash/songid/playtime 上报反馈。
 */
class KugouFmViewModel(application: Application) : AndroidViewModel(application) {

    private val kugouSession get() = PlayerManager.kugouSession

    private val _uiState = MutableStateFlow(KugouFmUiState())
    val uiState: StateFlow<KugouFmUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        // 监听登录态变化
        viewModelScope.launch {
            kugouSession.loggedInFlow.collect { loggedIn ->
                _uiState.update { it.copy(loggedIn = loggedIn) }
                if (loggedIn && _uiState.value.buffer.isEmpty() && !_uiState.value.loading) {
                    preloadPreview()
                }
            }
        }
    }

    /** 预加载一批推荐到 buffer（不播放）。 */
    fun preloadPreview() {
        val state = _uiState.value
        if (!state.loggedIn || state.loading) return
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val songs = withContext(Dispatchers.IO) {
                    kugouSession.fetchKugouPersonalFm(
                        KugouFmParams(
                            mode = state.mode,
                            songPoolId = state.songPool,
                            action = FmAction.PLAY,
                            remainSongCnt = 0
                        )
                    )
                }
                _uiState.update {
                    it.copy(
                        buffer = songs,
                        loading = false,
                        currentTrack = it.currentTrack ?: songs.firstOrNull()
                    )
                }
                NPLogger.d(TAG, "preloadPreview: got ${songs.size} songs")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                NPLogger.e(TAG, "preloadPreview failed", e)
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    /** 切换模式（红心/小众/速览）。 */
    fun switchMode(mode: KugouFmMode) {
        if (_uiState.value.mode == mode) return
        _uiState.update { it.copy(mode = mode) }
        // 切模式后清空 buffer 重新拉取
        _uiState.update { it.copy(buffer = emptyList(), currentTrack = null) }
        preloadPreview()
    }

    /** 切换曲库（口味/风格/探索）。 */
    fun switchSongPool(pool: KugouFmSongPool) {
        if (_uiState.value.songPool == pool) return
        _uiState.update { it.copy(songPool = pool) }
        _uiState.update { it.copy(buffer = emptyList(), currentTrack = null) }
        preloadPreview()
    }

    /** 开始播放：从 buffer 取第一首进入播放队列。 */
    fun startPlayback() {
        val state = _uiState.value
        if (!state.loggedIn) return
        if (state.buffer.isEmpty()) {
            // buffer 空则先拉再播
            fetchJob?.cancel()
            fetchJob = viewModelScope.launch {
                _uiState.update { it.copy(loading = true, error = null) }
                try {
                    val songs = withContext(Dispatchers.IO) {
                        kugouSession.fetchKugouPersonalFm(
                            KugouFmParams(
                                mode = state.mode,
                                songPoolId = state.songPool,
                                action = FmAction.PLAY,
                                remainSongCnt = 0
                            )
                        )
                    }
                    if (songs.isEmpty()) {
                        _uiState.update { it.copy(loading = false, error = "暂无推荐内容") }
                        return@launch
                    }
                    playTrack(songs.first(), songs)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    NPLogger.e(TAG, "startPlayback failed", e)
                    _uiState.update { it.copy(loading = false, error = e.message) }
                }
            }
            return
        }
        playTrack(state.buffer.first(), state.buffer)
    }

    /** 播放 buffer 中指定歌曲。 */
    fun playTrack(track: SongItem, queue: List<SongItem> = _uiState.value.buffer) {
        val state = _uiState.value
        // 把该歌移到 buffer 开头并移除重复
        val newBuffer = listOf(track) + queue.filter { it.id != track.id }
        _uiState.update {
            it.copy(
                buffer = newBuffer,
                currentTrack = track,
                isPlaying = true,
                loading = false,
                error = null
            )
        }
        // 实际播放交给 PlayerManager
        PlayerManager.playPlaylist(newBuffer, 0)
        NPLogger.d(TAG, "playTrack: ${track.name} - ${track.artist}")
        // 播放后检查是否需要补充
        maybeRefillBuffer(track, FmAction.PLAY, playTime = 0)
    }

    /** 踩当前歌曲：上报 feedback 并换下一首。 */
    fun dislikeCurrent() {
        val state = _uiState.value
        val current = state.currentTrack ?: return
        if (!state.loggedIn || state.loading) return
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                // 上报踩反馈，同时拉新歌
                val newSongs = withContext(Dispatchers.IO) {
                    kugouSession.fetchKugouPersonalFm(
                        KugouFmParams(
                            mode = state.mode,
                            songPoolId = state.songPool,
                            action = FmAction.GARBAGE,
                            hash = current.audioId,
                            songId = current.id,
                            playTime = 0,
                            remainSongCnt = state.buffer.size - 1
                        )
                    )
                }
                // 从 buffer 移除被踩的歌，合并新歌
                val remaining = state.buffer.filter { it.id != current.id }
                val merged = remaining + newSongs.filter { new -> remaining.none { it.id == new.id } }
                val next = merged.firstOrNull()
                _uiState.update {
                    it.copy(
                        buffer = merged,
                        currentTrack = next,
                        isPlaying = next != null,
                        loading = false
                    )
                }
                if (next != null) {
                    PlayerManager.playPlaylist(merged, 0)
                }
                NPLogger.d(TAG, "dislikeCurrent: removed ${current.name}, got ${newSongs.size} new")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                NPLogger.e(TAG, "dislikeCurrent failed", e)
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    /** 播放下一首（从 buffer 取第二首）。 */
    fun playNext() {
        val state = _uiState.value
        val next = state.buffer.getOrNull(1) ?: state.buffer.firstOrNull() ?: return
        playTrack(next, state.buffer)
    }

    /** 播放/暂停切换。 */
    fun togglePlayPause() {
        val state = _uiState.value
        if (state.currentTrack == null) {
            startPlayback()
            return
        }
        PlayerManager.togglePlayPause()
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    /** buffer 不足时自动补充。 */
    private fun maybeRefillBuffer(lastTrack: SongItem, action: FmAction, playTime: Int) {
        val state = _uiState.value
        if (state.buffer.size > FM_BUFFER_REFILL_THRESHOLD) return
        viewModelScope.launch {
            try {
                val newSongs = withContext(Dispatchers.IO) {
                    kugouSession.fetchKugouPersonalFm(
                        KugouFmParams(
                            mode = state.mode,
                            songPoolId = state.songPool,
                            action = action,
                            hash = lastTrack.audioId,
                            songId = lastTrack.id,
                            playTime = playTime,
                            remainSongCnt = state.buffer.size
                        )
                    )
                }
                if (newSongs.isEmpty()) return@launch
                _uiState.update { current ->
                    val merged = current.buffer + newSongs.filter { new ->
                        current.buffer.none { it.id == new.id }
                    }
                    current.copy(buffer = merged)
                }
                NPLogger.d(TAG, "refillBuffer: added ${newSongs.size} songs")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                NPLogger.w(TAG, "refillBuffer failed: ${e.message}")
            }
        }
    }
}
