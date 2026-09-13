package moe.ouom.neriplayer.ui.screen.playlist

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
import moe.ouom.neriplayer.core.api.qqmusic.fetchQQMusicPlaylistSongs
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.util.format.formatPlayCount
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest

private const val TAG = "QQMusicDetail"

/** QQ音乐歌单标识，作为 [moe.ouom.neriplayer.ui.screen.host.LibrarySelectedItem] 的载荷穿过导航。 */
@Parcelize
data class QQMusicPlaylistSelection(
    val playlistId: String,
    val name: String,
    val coverUrl: String? = null,
    val creator: String? = null,
    val songCount: Int = 0,
    val playCount: Long = 0L,
) : Parcelable

/** 歌曲列表加载结果：`songs == null` 表示加载中，[isError] 表示失败。 */
private data class QQMusicSongListSnapshot(
    val songs: List<SongItem>? = null,
    val isError: Boolean = false
)

@Composable
private fun QQMusicPlaylistCover(
    coverUrl: String?,
    displayName: String,
    size: Dp,
    cornerRadius: Dp,
    context: android.content.Context,
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

/**
 * QQ音乐歌单详情页：歌单头部 + 歌曲列表。
 */
@Composable
fun QQMusicPlaylistDetailScreen(
    playlist: QQMusicPlaylistSelection,
    onBack: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    offlineMode: Boolean = false
) {
    var retryToken by remember { mutableIntStateOf(0) }
    val snapshot by produceState<QQMusicSongListSnapshot?>(
        initialValue = null,
        playlist.playlistId,
        retryToken
    ) {
        value = null
        value = runCatching {
            AppContainer.qqMusicSession.fetchQQMusicPlaylistSongs(playlist.playlistId)
        }.fold(
            onSuccess = { QQMusicSongListSnapshot(songs = it) },
            onFailure = { error -> qqMusicFailureSnapshot(error) }
        )
    }

    QQMusicSongListScaffold(
        title = playlist.name,
        emptyText = stringResource(R.string.library_qqmusic_playlist_songs_empty),
        snapshot = snapshot,
        onBack = onBack,
        onRetry = { retryToken++ },
        onSongClick = onSongClick,
        offlineMode = offlineMode,
        header = { QQMusicPlaylistHeader(playlist = playlist, offlineMode = offlineMode) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QQMusicSongListScaffold(
    title: String,
    emptyText: String,
    snapshot: QQMusicSongListSnapshot?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit,
    offlineMode: Boolean,
    header: (@Composable () -> Unit)? = null
) {
    val songs = snapshot?.songs.orEmpty()
    QQMusicDetailScaffold(
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
                            text = stringResource(R.string.library_qqmusic_load_failed),
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
                        item(key = "qqmusic_detail_header") { header() }
                    }
                    itemsIndexed(songs) { index, song ->
                        QQMusicDetailSongRow(
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
private fun QQMusicDetailScaffold(
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
private fun QQMusicPlaylistHeader(
    playlist: QQMusicPlaylistSelection,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QQMusicPlaylistCover(
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
            Spacer(Modifier.height(6.dp))
            val metaParts = buildList {
                if (playlist.songCount > 0) {
                    add(stringResource(R.string.library_qqmusic_song_count, playlist.songCount))
                }
                if (playlist.playCount > 0L) add(formatPlayCount(context, playlist.playCount))
                playlist.creator?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" · "),
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
private fun QQMusicDetailSongRow(
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

private fun qqMusicFailureSnapshot(error: Throwable): QQMusicSongListSnapshot {
    if (error is CancellationException) throw error
    NPLogger.e(TAG, "load qq music detail songs failed", error)
    return QQMusicSongListSnapshot(isError = true)
}
