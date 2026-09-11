package moe.ouom.neriplayer.ui.screen.tab

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.ouom.neriplayer.core.api.kugou.KugouQrLoginClient
import moe.ouom.neriplayer.core.api.kugou.KugouQrLoginSession
import moe.ouom.neriplayer.core.api.kugou.KugouQrLoginStatus
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger

private const val QR_POLL_INTERVAL_MS = 2_000L

/**
 * 酷狗二维码登录弹窗：展示二维码 → 轮询扫码状态 → 成功后回调 [onLoggedIn]。
 * 登录成功由调用方将 login cookies 写入 [AppContainer.kugouSession] 并持久化。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KugouQrLoginSheet(
    onDismiss: () -> Unit,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    var loading by remember { mutableStateOf(true) }
    var qrImage by remember { mutableStateOf<Bitmap?>(null) }
    var statusText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val loginClient = remember { KugouQrLoginClient(AppContainer.kugouSession) }

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
                text = "扫一扫，登录酷狗",
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
                        contentDescription = "酷狗登录二维码",
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
                Text(text = "取消")
            }
        }
    }

    LaunchedEffect(Unit) {
        errorText = null
        loading = true
        val session: KugouQrLoginSession = try {
            loginClient.createSession()
        } catch (error: Exception) {
            NPLogger.e("KugouQrLogin", "create session failed", error)
            errorText = error.message ?: "二维码创建失败"
            loading = false
            return@LaunchedEffect
        }

        val bitmap = runCatching {
            val bytes = Base64.decode(
                session.qrImageBase64?.substringAfter(',') ?: session.qrImageBase64.orEmpty(),
                Base64.DEFAULT
            )
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
        qrImage = bitmap
        loading = false

        while (true) {
            val (status, cookies) = loginClient.check(session)
            when (status) {
                KugouQrLoginStatus.SUCCESS -> {
                    if (cookies["token"].isNullOrBlank()) {
                        statusText = "登录已确认，但未取到凭证，请重试"
                    } else {
                        AppContainer.kugouSession.applyCookies(cookies)
                        NPLogger.d("KugouQrLogin", "login success, cookieKeys=${cookies.keys}")
                        onLoggedIn()
                    }
                    return@LaunchedEffect
                }

                KugouQrLoginStatus.SCANNED -> statusText = "已扫码，请在手机上确认"
                KugouQrLoginStatus.EXPIRED -> {
                    errorText = "二维码已过期，请关闭后重试"
                    return@LaunchedEffect
                }

                KugouQrLoginStatus.ERROR -> {
                    statusText = "状态获取异常，请重试"
                    return@LaunchedEffect
                }

                KugouQrLoginStatus.WAITING -> {
                    if (statusText.isBlank()) statusText = "请使用酷狗 App 扫码"
                }
            }
            delay(QR_POLL_INTERVAL_MS)
        }
    }
}