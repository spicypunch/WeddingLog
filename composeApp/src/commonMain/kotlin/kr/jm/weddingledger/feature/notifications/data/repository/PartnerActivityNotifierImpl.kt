package kr.jm.weddingledger.feature.notifications.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.jm.weddingledger.core.config.BuildConfig
import kr.jm.weddingledger.core.network.WeddingIdResolver
import kr.jm.weddingledger.core.network.logSupabaseInfo
import kr.jm.weddingledger.core.network.logSupabaseRepositoryThrowable
import kr.jm.weddingledger.core.network.logSupabaseWarning
import kr.jm.weddingledger.feature.notifications.data.dto.DevicePushTokenUpsertDto
import kr.jm.weddingledger.feature.notifications.data.dto.PartnerActivityDispatchRequest
import kr.jm.weddingledger.feature.notifications.data.local.PushInstallationStorage
import kr.jm.weddingledger.feature.notifications.data.platform.currentPushPlatform
import kr.jm.weddingledger.feature.notifications.data.platform.fetchRemotePushTokenOrNull
import kr.jm.weddingledger.feature.notifications.domain.repository.PartnerActivityNotifier

class PartnerActivityNotifierImpl(
    private val supabaseClient: SupabaseClient,
    private val httpClient: HttpClient,
) : PartnerActivityNotifier {

    private val weddingIdResolver = WeddingIdResolver(supabaseClient)

    override suspend fun syncCurrentDevicePushToken() {
        try {
            val user = supabaseClient.auth.currentUserOrNull() ?: return
            val pushToken = fetchRemotePushTokenOrNull()?.trim().takeUnless { it.isNullOrEmpty() }
                ?: run {
                    deactivateCurrentDevicePushToken()
                    return
                }
            val installationId = PushInstallationStorage.getOrCreateInstallationId()
            val now = Clock.System.now().toString()

            supabaseClient
                .from("device_push_tokens")
                .upsert(
                    DevicePushTokenUpsertDto(
                        userId = user.id,
                        installationId = installationId,
                        platform = currentPushPlatform().dbValue,
                        pushToken = pushToken,
                        isActive = true,
                        lastSeenAt = now,
                        updatedAt = now,
                    ),
                ) {
                    onConflict = "user_id,installation_id"
                }

            logSupabaseInfo(
                featureName = FEATURE_NAME,
                message = "push_token_synced platform=${currentPushPlatform().dbValue} user=${user.id}",
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
        }
    }

    override suspend fun deactivateCurrentDevicePushToken() {
        try {
            val user = supabaseClient.auth.currentUserOrNull() ?: return
            val installationId = PushInstallationStorage.getOrCreateInstallationId()
            val now = Clock.System.now().toString()
            supabaseClient
                .from("device_push_tokens")
                .update(
                    buildJsonObject {
                        put("is_active", JsonPrimitive(false))
                        put("updated_at", JsonPrimitive(now))
                    }
                ) {
                    filter {
                        eq("user_id", user.id)
                        eq("installation_id", installationId)
                    }
                }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
        }
    }

    override suspend fun notifyExpenseAdded(expenseId: String) {
        dispatchActivity(type = "expense_added", entityId = expenseId)
    }

    override suspend fun notifyScheduleAdded(eventId: String) {
        dispatchActivity(type = "schedule_added", entityId = eventId)
    }

    private suspend fun dispatchActivity(
        type: String,
        entityId: String,
    ) {
        val normalizedId = entityId.trim()
        if (normalizedId.isBlank()) return

        try {
            val accessToken = supabaseClient.auth.currentAccessTokenOrNull()
                ?: return logSupabaseWarning(FEATURE_NAME, "partner_activity_dispatch_skipped reason=no_access_token type=$type")
            val weddingId = runCatching { weddingIdResolver.resolve() }.getOrNull().orEmpty()
            val response = httpClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/send-partner-activity-push") {
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                bearerAuth(accessToken)
                setBody(
                    PartnerActivityDispatchRequest(
                        type = type,
                        entityId = normalizedId,
                    )
                )
            }

            if (response.status != HttpStatusCode.OK) {
                logSupabaseWarning(
                    featureName = FEATURE_NAME,
                    message = "partner_activity_dispatch_failed status=${response.status.value} type=$type weddingId=$weddingId body=${response.bodyAsText().take(200)}",
                )
                return
            }

            logSupabaseInfo(
                featureName = FEATURE_NAME,
                message = "partner_activity_dispatch_success type=$type weddingId=$weddingId entityId=$normalizedId",
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseWarning(
                featureName = FEATURE_NAME,
                message = "partner_activity_dispatch_exception type=$type error=${t::class.simpleName}",
            )
        }
    }
}

private const val FEATURE_NAME: String = "PartnerActivity"
