package moe.ouom.neriplayer.ui.screen.tab

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.api.qqmusic.QQMusicQrLoginClient
import moe.ouom.neriplayer.core.api.qqmusic.QQMusicQrLoginStatus
import moe.ouom.neriplayer.core.api.qqmusic.QQMusicQrTicket
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger

private const val QR_POLL_INTERVAL_MS = 2_000L

/**
 * QQ音乐二维码登录弹窗：展示二维码 → 轮询扫码状态 → 成功后写入登录态并回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QQMusicQrLoginSheet(
    onDismiss: () -> Unit,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    var loading by remember { mutableStateOf(true) }
    var qrImage by remember { mutableStateOf<Bitmap?>(null) }
    var statusText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val session = remember { AppContainer.qqMusicSession }
    val loginClient = remember(session) { QQMusicQrLoginClient(session.client, session.device) }
    val titleText = stringResource(R.string.qqmusic_login_title)
    val qrContentDesc = stringResource(R.string.qqmusic_login_qr_content_desc)
    val cancelText = stringResource(R.string.action_cancel)
    val waitingText = stringResource(R.string.qqmusic_login_waiting)
    val scannedText = stringResource(R.string.qqmusic_login_scanned)
    val expiredText = stringResource(R.string.qqmusic_login_expired)
    val errorStatusText = stringResource(R.string.qqmusic_login_error)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))

            when {
                errorText != null -> {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }

                loading -> {
                    CircularProgressIndicator()
                }

                qrImage != null -> {
                    Image(
                        bitmap = qrImage!!.asImageBitmap(),
                        contentDescription = qrContentDesc,
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (statusText.isNotBlank()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss) {
                Text(text = cancelText)
            }
        }
    }

    LaunchedEffect(Unit) {
        errorText = null
        loading = true
        statusText = ""
        val ticket: QQMusicQrTicket = try {
            loginClient.createTicket()
        } catch (error: Exception) {
            NPLogger.e("QQMusicQrLogin", "create ticket failed", error)
            errorText = error.message ?: "二维码创建失败"
            loading = false
            return@LaunchedEffect
        }

        qrImage = runCatching {
            BitmapFactory.decodeByteArray(ticket.qrImage, 0, ticket.qrImage.size)
        }.getOrNull()
        loading = false

        while (true) {
            val (status, credential) = try {
                loginClient.poll(ticket)
            } catch (error: Exception) {
                NPLogger.e("QQMusicQrLogin", "poll failed", error)
                errorText = error.message ?: "状态获取失败"
                return@LaunchedEffect
            }
            when (status) {
                QQMusicQrLoginStatus.SUCCESS -> {
                    if (credential == null || !credential.isLoggedIn) {
                        statusText = "登录已确认，但未取到凭证，请重试"
                    } else {
                        session.applyCredential(credential)
                        NPLogger.d(
                            "QQMusicQrLogin",
                            "login success, musicid=${credential.musicid}"
                        )
                        onLoggedIn()
                    }
                    return@LaunchedEffect
                }

                QQMusicQrLoginStatus.SCANNED -> statusText = scannedText
                QQMusicQrLoginStatus.EXPIRED -> {
                    errorText = expiredText
                    return@LaunchedEffect
                }

                QQMusicQrLoginStatus.ERROR -> {
                    statusText = errorStatusText
                    return@LaunchedEffect
                }

                QQMusicQrLoginStatus.WAITING -> {
                    if (statusText.isBlank()) statusText = waitingText
                }
            }
            delay(QR_POLL_INTERVAL_MS)
        }
    }
}
