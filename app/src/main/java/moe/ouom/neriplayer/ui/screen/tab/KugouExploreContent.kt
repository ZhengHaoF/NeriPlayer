package moe.ouom.neriplayer.ui.screen.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.api.kugou.KugouChannelContent
import moe.ouom.neriplayer.core.api.kugou.KugouRankMeta
import moe.ouom.neriplayer.core.api.kugou.fetchKugouPlaylistSongs
import moe.ouom.neriplayer.core.api.kugou.fetchKugouRankSongs
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.LocalMiniPlayerHeight
import moe.ouom.neriplayer.util.format.formatPlayCount

private const val TAG = "KugouExploreContent"

/**
 * 酷狗概念版 tab 默认内容页：每日推荐 + 排行榜 + 热门歌单。
 * 点击榜单/歌单弹出底部歌曲列表，点击歌曲走 [onSongClick]（带队列播放）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KugouExploreContent(
    content: KugouChannelContent?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlistLoginHint = stringResource(R.string.kugou_playlist_login_hint)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val kugouLoggedIn by AppContainer.kugouSession.loggedInFlow.collectAsState()

    var sheetOpen by remember { mutableStateOf(false) }
    var sheetTitle by remember { mutableStateOf("") }
    var sheetSongs by remember { mutableStateOf<List<SongItem>?>(null) }
    var sheetPlaceholder by remember { mutableStateOf<String?>(null) }

    fun openSheet(title: String, load: suspend () -> List<SongItem>) {
        sheetTitle = title
        sheetSongs = null
        sheetPlaceholder = null
        sheetOpen = true
        scope.launch {
            runCatching { load() }.onSuccess { list ->
                NPLogger.d(TAG, "sheet loaded: $title songs=${list.size}")
                sheetSongs = list
            }.onFailure { e ->
                if (e is CancellationException) throw e
                NPLogger.e(TAG, "sheet load failed: $title", e)
                sheetSongs = emptyList()
            }
        }
    }

    fun openPlaylistPlaceholder(title: String, message: String) {
        sheetTitle = title
        sheetSongs = emptyList()
        sheetPlaceholder = message
        sheetOpen = true
    }

    when {
        loading && content == null -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        error != null && content == null -> {
            Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.action_retry),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { onRetry() }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        else -> {
            val safeContent = content ?: KugouChannelContent()
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 8.dp + LocalMiniPlayerHeight.current
                )
            ) {
                if (safeContent.dailyRecommend.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.kugou_daily_recommend))
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(safeContent.dailyRecommend) { song ->
                                KugouVerticalSongCard(song = song) {
                                    onSongClick(safeContent.dailyRecommend, safeContent.dailyRecommend.indexOf(song))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }

                if (safeContent.ranks.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.kugou_rank))
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(safeContent.ranks) { rank ->
                                KugouCoverCard(
                                    title = rank.rankName,
                                    subtitle = if (rank.playCount > 0L) formatPlayCount(context, rank.playCount) else null,
                                    coverUrl = rank.coverUrl
                                ) {
                                    openSheet(rank.rankName) {
                                        AppContainer.kugouSession.fetchKugouRankSongs(rank.rankId)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }

                if (safeContent.playlists.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.kugou_playlist))
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(safeContent.playlists) { playlist ->
                                KugouCoverCard(
                                    title = playlist.name,
                                    subtitle = playlist.creator,
                                    coverUrl = playlist.coverUrl
                                ) {
                                    if (kugouLoggedIn) {
                                        openSheet(playlist.name) {
                                            AppContainer.kugouSession
                                                .fetchKugouPlaylistSongs(playlist.globalCollectionId)
                                        }
                                    } else {
                                        openPlaylistPlaceholder(
                                            title = playlist.name,
                                            message = playlistLoginHint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState
        ) {
            Column(
                Modifier
                    .heightIn(max = 520.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = sheetTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                when {
                    sheetPlaceholder != null -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = sheetPlaceholder!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    sheetSongs == null -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    sheetSongs!!.isEmpty() -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.search_no_result),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(Modifier.weight(1f)) {
                            items(sheetSongs!!) { song ->
                                KugouSongRow(song = song) {
                                    val index = sheetSongs!!.indexOf(song)
                                    sheetOpen = false
                                    onSongClick(sheetSongs!!, index)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun KugouCoverCard(
    title: String,
    subtitle: String?,
    coverUrl: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KugouVerticalSongCard(
    song: SongItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = song.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KugouSongRow(
    song: SongItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 首页嵌入用的酷狗内容板块（非 Lazy 版）：每日推荐 + 排行榜 + 热门歌单。
 * loading/error 由调用方（首页 grid）处理，本组件只负责渲染已有内容与弹层；
 * 点击榜单/歌单弹出底部歌曲列表，点击歌曲走 [onSongClick]（带队列播放）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KugouHomeSections(
    content: KugouChannelContent?,
    onRetry: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlistLoginHint = stringResource(R.string.kugou_playlist_login_hint)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val kugouLoggedIn by AppContainer.kugouSession.loggedInFlow.collectAsState()

    // 私人FM
    val fmVm: moe.ouom.neriplayer.ui.viewmodel.tab.KugouFmViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var fmSheetOpen by remember { mutableStateOf(false) }
    val fmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sheetOpen by remember { mutableStateOf(false) }
    var sheetTitle by remember { mutableStateOf("") }
    var sheetSongs by remember { mutableStateOf<List<SongItem>?>(null) }
    var sheetPlaceholder by remember { mutableStateOf<String?>(null) }

    // FM底部弹层
    if (fmSheetOpen) {
        KugouPersonalFmSheet(
            fmVm = fmVm,
            sheetState = fmSheetState,
            onDismiss = { fmSheetOpen = false }
        )
    }

    fun openSheet(title: String, load: suspend () -> List<SongItem>) {
        sheetTitle = title
        sheetSongs = null
        sheetPlaceholder = null
        sheetOpen = true
        scope.launch {
            runCatching { load() }.onSuccess { list ->
                NPLogger.d(TAG, "home sheet loaded: $title songs=${list.size}")
                sheetSongs = list
            }.onFailure { e ->
                if (e is CancellationException) throw e
                NPLogger.e(TAG, "home sheet load failed: $title", e)
                sheetSongs = emptyList()
            }
        }
    }

    fun openPlaylistPlaceholder(title: String, message: String) {
        sheetTitle = title
        sheetSongs = emptyList()
        sheetPlaceholder = message
        sheetOpen = true
    }

    val safeContent = content ?: KugouChannelContent()
    val isEmpty = safeContent.dailyRecommend.isEmpty() &&
        safeContent.ranks.isEmpty() &&
        safeContent.playlists.isEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        if (isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.kugou_home_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.action_retry),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        } else {
            // 红心Radio入口卡片（放在最顶部）
            KugouFmHomeCard(
                fmVm = fmVm,
                onOpenSettings = { fmSheetOpen = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(16.dp))

            if (safeContent.dailyRecommend.isNotEmpty()) {
                SectionHeader(stringResource(R.string.kugou_daily_recommend))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(safeContent.dailyRecommend) { song ->
                        KugouVerticalSongCard(song = song) {
                            onSongClick(
                                safeContent.dailyRecommend,
                                safeContent.dailyRecommend.indexOf(song)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (safeContent.ranks.isNotEmpty()) {
                SectionHeader(stringResource(R.string.kugou_rank))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(safeContent.ranks) { rank ->
                        KugouCoverCard(
                            title = rank.rankName,
                            subtitle = if (rank.playCount > 0L) {
                                formatPlayCount(context, rank.playCount)
                            } else {
                                null
                            },
                            coverUrl = rank.coverUrl
                        ) {
                            openSheet(rank.rankName) {
                                AppContainer.kugouSession.fetchKugouRankSongs(rank.rankId)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (safeContent.playlists.isNotEmpty()) {
                SectionHeader(stringResource(R.string.kugou_playlist))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(safeContent.playlists) { playlist ->
                        KugouCoverCard(
                            title = playlist.name,
                            subtitle = playlist.creator,
                            coverUrl = playlist.coverUrl
                        ) {
                            if (kugouLoggedIn) {
                                openSheet(playlist.name) {
                                    AppContainer.kugouSession
                                        .fetchKugouPlaylistSongs(playlist.globalCollectionId)
                                }
                            } else {
                                openPlaylistPlaceholder(
                                    title = playlist.name,
                                    message = playlistLoginHint
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState
        ) {
            Column(
                Modifier
                    .heightIn(max = 520.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = sheetTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                when {
                    sheetPlaceholder != null -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sheetPlaceholder!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    sheetSongs == null -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    sheetSongs!!.isEmpty() -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_no_result),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(Modifier.weight(1f)) {
                            items(sheetSongs!!) { song ->
                                KugouSongRow(song = song) {
                                    val index = sheetSongs!!.indexOf(song)
                                    sheetOpen = false
                                    onSongClick(sheetSongs!!, index)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
