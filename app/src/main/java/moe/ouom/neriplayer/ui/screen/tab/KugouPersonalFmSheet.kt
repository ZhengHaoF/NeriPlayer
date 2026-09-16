package moe.ouom.neriplayer.ui.screen.tab

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
 * File: moe.ouom.neriplayer.ui.screen.tab/KugouPersonalFmSheet
 */

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.api.kugou.KugouFmMode
import moe.ouom.neriplayer.core.api.kugou.KugouFmSongPool
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.viewmodel.tab.KugouFmViewModel

private val FmOrange = Color(0xFFF97316)
private val FmOrangeDark = Color(0xFFEA580C)

/**
 * 首页红心Radio入口卡片（横滑区最前）。
 * 点击卡片 → 直接开始播放；点击右上角切换图标 → 打开模式/曲库选择弹层。
 */
@Composable
internal fun KugouFmHomeCard(
    fmVm: KugouFmViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by fmVm.uiState.collectAsState()
    // 首次进入时预加载
    LaunchedEffect(ui.loggedIn) {
        if (ui.loggedIn && ui.buffer.isEmpty() && !ui.loading) {
            fmVm.preloadPreview()
        }
    }

    val track = ui.currentTrack ?: ui.buffer.firstOrNull()
    val gradient = remember {
        Brush.linearGradient(
            colors = listOf(FmOrange, FmOrangeDark)
        )
    }

    Box(
        modifier = modifier
            .width(200.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable { fmVm.startPlayback() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.kugou_fm_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                // 切换图标（右上角）
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = stringResource(R.string.kugou_fm_settings),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (ui.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (track != null) {
                Text(
                    text = "${track.name} - ${track.artist}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (!ui.loggedIn) {
                Text(
                    text = stringResource(R.string.kugou_fm_login_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            } else {
                Text(
                    text = stringResource(R.string.kugou_fm_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
        // 播放按钮（右下角）
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (ui.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = FmOrangeDark,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 私人FM 完整播放页（ModalBottomSheet）。
 * 模式切换 + 曲库切换 + 唱片动画 + 踩/播放/下一首。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KugouPersonalFmSheet(
    fmVm: KugouFmViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val ui by fmVm.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = FmOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.kugou_fm_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 模式切换
            Text(
                text = stringResource(R.string.kugou_fm_mode_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KugouFmMode.entries.forEach { mode ->
                    FilterChip(
                        selected = ui.mode == mode,
                        onClick = { fmVm.switchMode(mode) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FmOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 曲库切换
            Text(
                text = stringResource(R.string.kugou_fm_pool_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KugouFmSongPool.entries.forEach { pool ->
                    FilterChip(
                        selected = ui.songPool == pool,
                        onClick = { fmVm.switchSongPool(pool) },
                        label = { Text(pool.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FmOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 唱片区 + 歌曲信息
            val track = ui.currentTrack
            val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "vinylRotation"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (ui.loading && track == null) {
                    CircularProgressIndicator(color = FmOrange)
                } else if (track != null) {
                    // 唱片
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1e1e1e)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = track.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .rotate(if (ui.isPlaying) rotation else 0f)
                        )
                        // 中心圆孔
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }
                } else {
                    Text(
                        text = if (!ui.loggedIn) {
                            stringResource(R.string.kugou_fm_login_hint)
                        } else {
                            stringResource(R.string.kugou_fm_empty)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 歌曲信息
            if (track != null) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 踩
                IconButton(
                    onClick = { fmVm.dislikeCurrent() },
                    enabled = track != null && !ui.loading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.kugou_fm_dislike),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(24.dp))

                // 播放/暂停
                IconButton(
                    onClick = { fmVm.togglePlayPause() },
                    enabled = !ui.loading,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(FmOrange)
                ) {
                    Icon(
                        imageVector = if (ui.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.width(24.dp))

                // 下一首
                IconButton(
                    onClick = { fmVm.playNext() },
                    enabled = ui.buffer.size > 1 && !ui.loading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = stringResource(R.string.kugou_fm_next),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 待播预览
            if (ui.buffer.size > 1) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.kugou_fm_upcoming),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ui.buffer.drop(1).take(6)) { song ->
                        FmUpcomingSongCard(
                            song = song,
                            onClick = { fmVm.playTrack(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FmUpcomingSongCard(
    song: SongItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = song.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
