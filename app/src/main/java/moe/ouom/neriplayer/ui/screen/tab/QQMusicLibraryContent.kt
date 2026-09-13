package moe.ouom.neriplayer.ui.screen.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.api.qqmusic.QQMusicUserPlaylistMeta
import moe.ouom.neriplayer.core.api.qqmusic.fetchQQMusicUserPlaylists
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.ui.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.screen.playlist.QQMusicPlaylistSelection
import moe.ouom.neriplayer.util.format.formatPlayCount
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest

private const val TAG = "QQMusicLibrary"

/**
 * 媒体库「QQ音乐」tab 内容页：登录后直接展示「我的歌单」，
 * 点击由 Host 层打开歌单详情；未登录时给出登录引导。
 */
@Composable
internal fun QQMusicLibraryContent(
    listState: LazyListState,
    onPlaylistClick: (QQMusicPlaylistSelection) -> Unit,
    offlineMode: Boolean = false
) {
    val qqMusicLoggedIn by AppContainer.qqMusicSession.loggedInFlow.collectAsState()
    var showLoginSheet by remember { mutableStateOf(false) }

    if (!qqMusicLoggedIn) {
        QQMusicLibraryLoginEmptyState(
            offlineMode = offlineMode,
            onLogin = { showLoginSheet = true }
        )
        if (showLoginSheet) {
            QQMusicQrLoginSheet(
                onDismiss = { showLoginSheet = false },
                onLoggedIn = { showLoginSheet = false }
            )
        }
        return
    }

    QQMusicUserPlaylistList(
        listState = listState,
        onPlaylistClick = onPlaylistClick,
        offlineMode = offlineMode
    )
}

@Composable
private fun QQMusicUserPlaylistList(
    listState: LazyListState,
    onPlaylistClick: (QQMusicPlaylistSelection) -> Unit,
    offlineMode: Boolean
) {
    var retryToken by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf(false) }
    val playlists by produceState<List<QQMusicUserPlaylistMeta>?>(
        initialValue = null,
        retryToken
    ) {
        value = null
        loadError = false
        value = runCatching { AppContainer.qqMusicSession.fetchQQMusicUserPlaylists() }
            .onFailure { error ->
                NPLogger.e(TAG, "load qq music user playlists failed", error)
                loadError = true
            }
            .getOrElse { emptyList() }
    }
    val resolved = playlists
    val miniPlayerHeight = LocalMiniPlayerHeight.current

    when {
        resolved == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        loadError -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.library_qqmusic_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { retryToken++ }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }

        resolved.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.library_qqmusic_playlists_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp + miniPlayerHeight
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(resolved, key = { it.playlistId }) { playlist ->
                    QQMusicPlaylistListRow(
                        playlist = playlist,
                        offlineMode = offlineMode,
                        onClick = {
                            onPlaylistClick(
                                QQMusicPlaylistSelection(
                                    playlistId = playlist.playlistId,
                                    name = playlist.name,
                                    coverUrl = playlist.coverUrl,
                                    creator = playlist.creator,
                                    songCount = playlist.songCount,
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

@Composable
private fun QQMusicPlaylistListRow(
    playlist: QQMusicUserPlaylistMeta,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(12.dp)
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(cardShape)
            .clickable(onClick = onClick)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = playlist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                val subtitle = buildList {
                    if (playlist.songCount > 0) {
                        add(stringResource(R.string.library_qqmusic_song_count, playlist.songCount))
                    }
                    playlist.creator?.takeIf { it.isNotBlank() }?.let { add(it) }
                    if (playlist.playCount > 0L) add(formatPlayCount(context, playlist.playCount))
                }.joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
                val coverUrl = playlist.coverUrl
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = offlineCachedImageRequest(
                            context = context,
                            data = coverUrl,
                            sizePx = 160,
                            allowHardware = false,
                            offlineMode = offlineMode
                        ),
                        contentDescription = playlist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun QQMusicLibraryLoginEmptyState(
    offlineMode: Boolean,
    onLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(
                    if (offlineMode) {
                        R.string.library_qqmusic_offline_hint
                    } else {
                        R.string.library_qqmusic_login_hint
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!offlineMode) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onLogin) {
                    Text(stringResource(R.string.library_qqmusic_login_action))
                }
            }
        }
    }
}
