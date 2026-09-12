package moe.ouom.neriplayer.ui.screen.playlist

import android.content.Context
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.parcelize.Parcelize
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.api.kugou.KugouPlaylistMeta
import moe.ouom.neriplayer.core.api.kugou.fetchKugouCloudSongs
import moe.ouom.neriplayer.core.api.kugou.fetchKugouHistory
import moe.ouom.neriplayer.core.api.kugou.fetchKugouPlaylistSongs
import moe.ouom.neriplayer.core.api.kugou.fetchKugouUserPlaylists
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest

private const val TAG = "KugouDetail"

/**
 * 歌单封面：有封面走网络图，无封面用歌单图标占位。
 *
 * 酷狗概念版系统歌单（「默认收藏」「我喜欢」）服务端返回的 `pic` 为空串，
 * 且响应对象里没有 imgurl / img / cover，属服务端不提供封面，
 * 这里用 Material 歌单图标 + surfaceVariant 底色占位，避免列表留白。
 */
@Composable
private fun KugouPlaylistCover(
    coverUrl: String?,
    displayName: String,
    size: Dp,
    cornerRadius: Dp,
    context: Context,
    offlineMode: Boolean
) {
    val shape = RoundedCornerShape(cornerRadius)
    val resolvedCover = coverUrl?.takeIf { it.isNotBlank() }
    if (resolvedCover != null) {
        AsyncImage(
            model = offlineCachedImageRequest(
                context = context,
                data = resolvedCover,
                sizePx = (size.value * 3).toInt(),
                allowHardware = false,
                offlineMode = offlineMode
            ),
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                contentDescription = displayName,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/** 媒体库酷狗概念版 tab 的一级入口类型（云盘 / 听歌历史 / 我的歌单）。 */
enum class KugouLibraryEntryKind {
    CLOUD,
    HISTORY,
    USER_PLAYLISTS
}

/** 酷狗概念版歌单标识，作为 [moe.ouom.neriplayer.ui.screen.host.LibrarySelectedItem] 的载荷穿过导航。 */
@Parcelize
data class KugouPlaylistSelection(
    val globalCollectionId: String,
    val name: String,
    val coverUrl: String? = null,
    val creator: String? = null,
    val playCount: Long = 0L
) : Parcelable

/** 歌曲列表加载结果：`songs == null` 表示加载中，[isError] 表示失败。 */
private data class KugouSongListSnapshot(
    val songs: List<SongItem>? = null,
    val isError: Boolean = false
)

/**
 * 酷狗概念版云盘 / 听歌历史详情页：直接承载歌曲列表。
 */
@Composable
fun KugouSongListDetailScreen(
    kind: KugouLibraryEntryKind,
    onBack: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    offlineMode: Boolean = false
) {
    val titleResId = when (kind) {
        KugouLibraryEntryKind.HISTORY -> R.string.library_kugou_history
        else -> R.string.library_kugou_cloud
    }
    val emptyResId = when (kind) {
        KugouLibraryEntryKind.HISTORY -> R.string.library_kugou_history_empty
        else -> R.string.library_kugou_cloud_empty
    }
    var retryToken by remember { mutableIntStateOf(0) }
    val snapshot by produceState<KugouSongListSnapshot?>(
        initialValue = null,
        kind,
        retryToken
    ) {
        value = null
        value = runCatching { fetchKugouEntrySongs(kind) }.fold(
            onSuccess = { KugouSongListSnapshot(songs = it) },
            onFailure = { error -> kugouFailureSnapshot(error) }
        )
    }

    KugouSongListScaffold(
        title = stringResource(titleResId),
        emptyText = stringResource(emptyResId),
        snapshot = snapshot,
        onBack = onBack,
        onRetry = { retryToken++ },
        onSongClick = onSongClick,
        offlineMode = offlineMode
    )
}

/**
 * 酷狗概念版「我的歌单」详情页：列出账号下的收藏 / 创建歌单，点击进入歌单歌曲。
 */
@Composable
fun KugouUserPlaylistDetailScreen(
    onBack: () -> Unit,
    onPlaylistClick: (KugouPlaylistSelection) -> Unit = {},
    offlineMode: Boolean = false
) {
    var retryToken by remember { mutableIntStateOf(0) }
    val playlists by produceState<List<KugouPlaylistMeta>?>(
        initialValue = null,
        retryToken
    ) {
        value = null
        value = runCatching { AppContainer.kugouSession.fetchKugouUserPlaylists() }.fold(
            onSuccess = { it },
            onFailure = { error ->
                if (error is CancellationException) throw error
                NPLogger.e(TAG, "load kugou user playlists failed", error)
                emptyList()
            }
        )
    }

    val resolved = playlists
    KugouDetailScaffold(
        title = stringResource(R.string.library_kugou_playlists),
        onBack = onBack
    ) { padding ->
        when {
            resolved == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            resolved.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.library_kugou_playlists_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp + LocalMiniPlayerHeight.current
                    )
                ) {
                    items(resolved, key = { it.globalCollectionId }) { playlist ->
                        KugouPlaylistRow(
                            playlist = playlist,
                            offlineMode = offlineMode,
                            onClick = {
                                onPlaylistClick(
                                    KugouPlaylistSelection(
                                        globalCollectionId = playlist.globalCollectionId,
                                        name = playlist.name,
                                        coverUrl = playlist.coverUrl,
                                        creator = playlist.creator,
                                        playCount = playlist.playCount
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 酷狗概念版歌单详情页：歌单头部 + 歌曲列表。
 */
@Composable
fun KugouPlaylistDetailScreen(
    playlist: KugouPlaylistSelection,
    onBack: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    offlineMode: Boolean = false
) {
    var retryToken by remember { mutableIntStateOf(0) }
    val snapshot by produceState<KugouSongListSnapshot?>(
        initialValue = null,
        playlist.globalCollectionId,
        retryToken
    ) {
        value = null
        value = runCatching {
            AppContainer.kugouSession.fetchKugouPlaylistSongs(playlist.globalCollectionId)
        }.fold(
            onSuccess = { KugouSongListSnapshot(songs = it) },
            onFailure = { error -> kugouFailureSnapshot(error) }
        )
    }

    KugouSongListScaffold(
        title = playlist.name,
        emptyText = stringResource(R.string.library_kugou_playlist_songs_empty),
        snapshot = snapshot,
        onBack = onBack,
        onRetry = { retryToken++ },
        onSongClick = onSongClick,
        offlineMode = offlineMode,
        header = { KugouPlaylistHeader(playlist = playlist, offlineMode = offlineMode) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KugouSongListScaffold(
    title: String,
    emptyText: String,
    snapshot: KugouSongListSnapshot?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit,
    offlineMode: Boolean,
    header: (@Composable () -> Unit)? = null
) {
    val songs = snapshot?.songs.orEmpty()
    KugouDetailScaffold(
        title = title,
        onBack = onBack,
        playAllEnabled = songs.isNotEmpty(),
        onPlayAll = { if (songs.isNotEmpty()) onSongClick(songs, 0) }
    ) { padding ->
        when {
            snapshot == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            snapshot.isError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.library_kugou_load_failed),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }

            songs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp + LocalMiniPlayerHeight.current
                    )
                ) {
                    if (header != null) {
                        item(key = "kugou_detail_header") { header() }
                    }
                    itemsIndexed(songs) { index, song ->
                        KugouDetailSongRow(
                            index = index + 1,
                            song = song,
                            offlineMode = offlineMode,
                            onClick = { onSongClick(songs, index) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KugouDetailScaffold(
    title: String,
    onBack: () -> Unit,
    playAllEnabled: Boolean = false,
    onPlayAll: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    HapticIconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (playAllEnabled) {
                        HapticIconButton(onClick = onPlayAll) {
                            Icon(
                                Icons.AutoMirrored.Outlined.PlaylistPlay,
                                contentDescription = stringResource(R.string.cd_play_all)
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        content = content
    )
}

@Composable
private fun KugouPlaylistHeader(
    playlist: KugouPlaylistSelection,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KugouPlaylistCover(
            coverUrl = playlist.coverUrl,
            displayName = playlist.name,
            size = 96.dp,
            cornerRadius = 18.dp,
            context = context,
            offlineMode = offlineMode
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall
            )
            val creator = playlist.creator
            if (!creator.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = creator,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun KugouPlaylistRow(
    playlist: KugouPlaylistMeta,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KugouPlaylistCover(
            coverUrl = playlist.coverUrl,
            displayName = playlist.name,
            size = 56.dp,
            cornerRadius = 10.dp,
            context = context,
            offlineMode = offlineMode
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            val creator = playlist.creator
            if (!creator.isNullOrBlank()) {
                Text(
                    text = creator,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun KugouDetailSongRow(
    index: Int,
    song: SongItem,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }

        val coverUrl = song.coverUrl
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = offlineCachedImageRequest(
                    context = context,
                    data = coverUrl,
                    sizePx = 160,
                    allowHardware = false,
                    offlineMode = offlineMode
                ),
                contentDescription = song.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = song.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private suspend fun fetchKugouEntrySongs(kind: KugouLibraryEntryKind): List<SongItem> {
    return when (kind) {
        KugouLibraryEntryKind.CLOUD -> AppContainer.kugouSession.fetchKugouCloudSongs()
        KugouLibraryEntryKind.HISTORY -> AppContainer.kugouSession.fetchKugouHistory()
        KugouLibraryEntryKind.USER_PLAYLISTS -> emptyList()
    }
}

private fun kugouFailureSnapshot(error: Throwable): KugouSongListSnapshot {
    if (error is CancellationException) throw error
    NPLogger.e(TAG, "load kugou detail songs failed", error)
    return KugouSongListSnapshot(isError = true)
}
