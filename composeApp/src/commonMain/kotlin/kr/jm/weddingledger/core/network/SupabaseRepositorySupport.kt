package kr.jm.weddingledger.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class WeddingIdResolver(
    private val supabaseClient: SupabaseClient,
) {

    private var cachedUserId: String? = null
    private var cachedWeddingId: String? = null

    suspend fun resolve(): String {
        val user = supabaseClient.auth.currentUserOrNull()
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val userId = user.id
        if (cachedUserId == userId) {
            cachedWeddingId?.let { return it }
        }

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

        cachedUserId = userId
        cachedWeddingId = weddingId
        return weddingId
    }
}

fun logSupabaseRepositoryThrowable(
    featureName: String,
    t: Throwable,
) {
    // Avoid printing t.message (RestException may include URL + request headers).
    when (t) {
        is RestException -> println("Supabase[$featureName] error=${t.error} description=${t.description} status=${t.statusCode}")
        else -> println("$featureName error=${t::class.simpleName}")
    }
}

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
