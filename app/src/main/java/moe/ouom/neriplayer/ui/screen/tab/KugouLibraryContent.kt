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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.ui.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.screen.playlist.KugouLibraryEntryKind

/**
 * 媒体库「酷狗概念版」tab 内容页：登录后展示云盘 / 听歌历史 / 我的歌单三个入口，
 * 点击后由 Host 层打开对应详情页；未登录时给出登录引导。
 */
@Composable
internal fun KugouLibraryContent(
    listState: LazyListState,
    onOpenDetail: (KugouLibraryEntryKind) -> Unit,
    offlineMode: Boolean = false
) {
    val kugouLoggedIn by AppContainer.kugouSession.loggedInFlow.collectAsState()
    var showLoginSheet by remember { mutableStateOf(false) }
    val miniPlayerHeight = LocalMiniPlayerHeight.current

    if (!kugouLoggedIn) {
        KugouLibraryLoginEmptyState(
            offlineMode = offlineMode,
            onLogin = { showLoginSheet = true }
        )
        if (showLoginSheet) {
            KugouQrLoginSheet(
                onDismiss = { showLoginSheet = false },
                onLoggedIn = { showLoginSheet = false }
            )
        }
        return
    }

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
        item(key = "kugou_entry_cloud") {
            KugouLibraryEntryRow(
                icon = Icons.Outlined.Cloud,
                title = stringResource(R.string.library_kugou_cloud),
                onClick = { onOpenDetail(KugouLibraryEntryKind.CLOUD) }
            )
        }
        item(key = "kugou_entry_history") {
            KugouLibraryEntryRow(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.library_kugou_history),
                onClick = { onOpenDetail(KugouLibraryEntryKind.HISTORY) }
            )
        }
        item(key = "kugou_entry_playlists") {
            KugouLibraryEntryRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = stringResource(R.string.library_kugou_playlists),
                onClick = { onOpenDetail(KugouLibraryEntryKind.USER_PLAYLISTS) }
            )
        }
    }
}

@Composable
private fun KugouLibraryEntryRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
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
            headlineContent = { Text(title) },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            },
            trailingContent = {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun KugouLibraryLoginEmptyState(
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
                        R.string.library_kugou_offline_hint
                    } else {
                        R.string.library_kugou_login_hint
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!offlineMode) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onLogin) {
                    Text(stringResource(R.string.library_kugou_login_action))
                }
            }
        }
    }
}
