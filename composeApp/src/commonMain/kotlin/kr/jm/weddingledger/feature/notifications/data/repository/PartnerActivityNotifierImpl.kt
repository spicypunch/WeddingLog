package kr.jm.weddingledger.feature.notifications.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
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
            val user = supabaseClient.auth.currentUserOrNull()
                ?: return logSupabaseWarning(FEATURE_NAME, "push_token_sync_skipped reason=no_user")
            val pushToken = fetchRemotePushTokenOrNull()?.trim().takeUnless { it.isNullOrEmpty() }
                ?: run {
                    logSupabaseWarning(FEATURE_NAME, "push_token_sync_skipped reason=no_remote_token platform=${currentPushPlatform().dbValue}")
                    deactivateCurrentDevicePushToken()
                    return
                }
            val installationId = PushInstallationStorage.getOrCreateInstallationId()
            val now = Clock.System.now().toString()

            supabaseClient
                .from("device_push_tokens")
                .delete {
                    filter {
                        eq("user_id", user.id)
                        eq("installation_id", installationId)
                    }
                }

            supabaseClient
                .from("device_push_tokens")
                .delete {
                    filter { eq("push_token", pushToken) }
                }

            supabaseClient
                .from("device_push_tokens")
                .insert(
                    DevicePushTokenUpsertDto(
                        userId = user.id,
                        installationId = installationId,
                        platform = currentPushPlatform().dbValue,
                        pushToken = pushToken,
                        isActive = true,
                        lastSeenAt = now,
                        updatedAt = now,
                    ),
                )

            logSupabaseInfo(
                featureName = FEATURE_NAME,
                message = "push_token_synced platform=${currentPushPlatform().dbValue} user=${user.id} installationId=${installationId.take(8)} tokenSuffix=${pushToken.takeLast(8)}",
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
        }
    }

    override suspend fun deactivateCurrentDevicePushToken() {
        try {
            val user = supabaseClient.auth.currentUserOrNull()
                ?: return logSupabaseWarning(FEATURE_NAME, "push_token_deactivate_skipped reason=no_user")
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
            logSupabaseInfo(
                featureName = FEATURE_NAME,
                message = "push_token_deactivated user=${user.id} installationId=${installationId.take(8)}",
            )
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
            val weddingId = runCatching { weddingIdResolver.resolve() }.getOrNull().orEmpty()
            logSupabaseInfo(
                featureName = FEATURE_NAME,
                message = "partner_activity_dispatch_start type=$type weddingId=$weddingId entityId=$normalizedId",
            )
            val response = httpClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/send-partner-activity-push") {
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
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
