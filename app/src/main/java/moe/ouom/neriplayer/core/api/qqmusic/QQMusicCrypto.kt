package moe.ouom.neriplayer.core.api.qqmusic

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * QQ音乐登录链路所需的密码学与签名原语。
 * 对照 QQMusicapi：`utils/common.js#hash33` / `utils/qimei.js` 的 RSA+AES+MD5。
 */
internal object QQMusicCrypto {

    fun hash33(input: String, seed: Int = 0): Int {
        var h = seed
        for (ch in input) {
            h = (h shl 5) + h + ch.code
        }
        return h and 0x7FFFFFFF
    }

    fun md5Hex(vararg parts: String): String {
        val md = MessageDigest.getInstance("MD5")
        for (part in parts) {
            md.update(part.toByteArray(Charsets.UTF_8))
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** RSA-PKCS1 加密，输出 Base64（对应 Node `publicEncrypt` + PKCS1 padding）。 */
    fun rsaPkcs1Base64(plain: String, publicKeyBase64: String): String {
        val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(keyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    /**
     * AES-128-CBC：key 与 iv 同为 [keyBytes]（16 字节），PKCS5 填充，输出 Base64。
     * 与 QQMusicapi `aesEncrypt` 一致。
     */
    fun aesCbcBase64(keyBytes: ByteArray, content: String): String {
        require(keyBytes.size == 16) { "AES-128 key must be 16 bytes" }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            IvParameterSpec(keyBytes)
        )
        return Base64.getEncoder()
            .encodeToString(cipher.doFinal(content.toByteArray(Charsets.UTF_8)))
    }

    fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun randomHexChars(length: Int): String {
        val chars = "0123456789abcdef"
        val random = SecureRandom()
        return buildString(length) {
            repeat(length) { append(chars[random.nextInt(chars.length)]) }
        }
    }

    fun randomHexNonZero(length: Int): String {
        val chars = "123456789abcdef"
        val random = SecureRandom()
        return buildString(length) {
            repeat(length) { append(chars[random.nextInt(chars.length)]) }
        }
    }

    fun randomInt(min: Int, max: Int): Int {
        return SecureRandom().nextInt(max - min + 1) + min
    }

    /** 满足 Luhn 校验的 15 位随机 IMEI。 */
    fun randomImei(): String {
        val random = SecureRandom()
        val digits = IntArray(14) { random.nextInt(10) }
        var sum = 0
        for (i in 0 until 14) {
            var v = digits[i]
            if (i % 2 == 1) {
                v *= 2
                if (v > 9) v -= 9
            }
            sum += v
        }
        val check = (10 - (sum % 10)) % 10
        return digits.joinToString("") + check.toString()
    }

    /** 40 段 beaconId，格式对齐 QQMusicapi `randomBeaconId`。 */
    fun randomBeaconId(monthStart: String): String {
        val rand1 = randomInt(100_000, 999_999)
        val rand2 = randomInt(100_000_000, 999_999_999)
        val k1Set = setOf(1, 2, 13, 14, 17, 18, 21, 22, 25, 26, 29, 30, 33, 34, 37, 38)
        return buildString {
            for (i in 1..40) {
                when {
                    i in k1Set -> append("k$i:$monthStart$rand1.$rand2;")
                    i == 3 -> append("k3:0000000000000000;")
                    i == 4 -> append("k4:${randomHexNonZero(16)};")
                    else -> append("k$i:${randomInt(0, 9999)};")
                }
            }
        }
    }

    /** UTC 月初日期 `yyyy-MM-01`，beaconId 的 k1 段使用。 */
    fun utcMonthStart(epochMs: Long = System.currentTimeMillis()): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return String.format(
            Locale.ROOT,
            "%04d-%02d-01",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }
}
