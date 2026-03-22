package kr.jm.weddingledger.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kr.jm.weddingledger.core.logging.LogLevel
import kr.jm.weddingledger.core.logging.platformLog

class WeddingIdResolver(
    private val supabaseClient: SupabaseClient,
) {

    private val cache = WeddingIdCache()

    suspend fun resolve(): String {
        val user = supabaseClient.auth.currentUserOrNull()
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val userId = user.id
        cache.get(userId)?.let { return it }

        val profile = try {
            supabaseClient
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                    limit(count = 1)
                }
                .decodeList<WeddingIdProfileRow>()
                .firstOrNull()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            null
        }

        val weddingId = profile?.weddingId ?: userId

        if (profile == null) {
            try {
                supabaseClient
                    .from("profiles")
                    .insert(
                        ProfileInsertRow(
                            id = userId,
                            email = user.email,
                            weddingId = weddingId,
                        )
                    )
            } catch (_: Throwable) {
                // Ignore: profile row may already be managed by trigger/RLS policy.
            }
        }

        cache.put(userId = userId, weddingId = weddingId)
        return weddingId
    }
}

internal class WeddingIdCache {
    private var cachedUserId: String? = null
    private var cachedWeddingId: String? = null

    fun get(userId: String): String? {
        return if (cachedUserId == userId) cachedWeddingId else null
    }

    fun put(
        userId: String,
        weddingId: String,
    ) {
        cachedUserId = userId
        cachedWeddingId = weddingId
    }
}

fun logSupabaseInfo(
    featureName: String,
    message: String,
) {
    printSupabaseLog(level = LogLevel.Info, featureName = featureName, message = message)
}

fun logSupabaseWarning(
    featureName: String,
    message: String,
) {
    printSupabaseLog(level = LogLevel.Warning, featureName = featureName, message = message)
}

fun logSupabaseError(
    featureName: String,
    message: String,
) {
    printSupabaseLog(level = LogLevel.Error, featureName = featureName, message = message)
}

fun logSupabaseRepositoryThrowable(
    featureName: String,
    t: Throwable,
) {
    // Avoid printing t.message (RestException may include URL + request headers).
    when (t) {
        is RestException -> {
            logSupabaseError(
                featureName = featureName,
                message = "error=${t.error} description=${t.description} status=${t.statusCode}",
            )
        }

        else -> {
            logSupabaseError(
                featureName = featureName,
                message = "type=${t::class.simpleName}",
            )
        }
    }
}

private fun printSupabaseLog(
    level: LogLevel,
    featureName: String,
    message: String,
) {
    platformLog(
        level = level,
        tag = SUPABASE_LOG_TAG,
        message = "[${level.name.uppercase()}][$featureName] $message",
    )
}

private const val SUPABASE_LOG_TAG: String = "WL_SUPABASE"

@Serializable
private data class WeddingIdProfileRow(
    @SerialName("wedding_id")
    val weddingId: String? = null,
)

@Serializable
private data class ProfileInsertRow(
    val id: String,
    val email: String?,
    @SerialName("wedding_id")
    val weddingId: String,
)
