package de.meply.meply.utils

import de.meply.meply.BuildConfig
import java.util.zip.CRC32

object AvatarUtils {
    private val AVATAR_BASE_URL = "${BuildConfig.WEB_BASE}/etc/images"

    /**
     * Calculate avatar index based on user ID using CRC32 hash
     * Mirrors the PHP implementation:
     * $hash = crc32($authorIdRaw);
     * $index = ($hash % 8) + 1;
     */
    fun calculateAvatarIndex(userId: String): Int {
        val crc = CRC32()
        crc.update(userId.toByteArray())
        val hash = crc.value
        return ((hash % 8) + 1).toInt()
    }

    /**
     * Get the default avatar URL for a user based on their ID
     * @param userId The user ID (documentId or userId field)
     * @return URL to the calculated avatar image
     */
    fun getDefaultAvatarUrl(userId: String): String {
        val index = calculateAvatarIndex(userId)
        return "$AVATAR_BASE_URL/avatar$index.png"
    }
}
