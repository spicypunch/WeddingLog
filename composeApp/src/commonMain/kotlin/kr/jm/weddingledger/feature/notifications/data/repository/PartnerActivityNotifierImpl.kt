package kr.jm.weddingledger.feature.notifications.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import kr.jm.weddingledger.core.config.BuildConfig
import kr.jm.weddingledger.core.network.WeddingIdResolver
import kr.jm.weddingledger.core.network.logSupabaseInfo
import kr.jm.weddingledger.core.network.logSupabaseRepositoryThrowable
import kr.jm.weddingledger.core.network.logSupabaseWarning
import kr.jm.weddingledger.feature.notifications.data.dto.DevicePushTokenSyncRequest
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
            val synced = manageCurrentDevicePushToken(
                action = ACTION_SYNC,
                installationId = installationId,
                platform = currentPushPlatform().dbValue,
                pushToken = pushToken,
            )
            if (!synced) return

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
            val deactivated = manageCurrentDevicePushToken(
                action = ACTION_DEACTIVATE,
                installationId = installationId,
            )
            if (!deactivated) return
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

    private suspend fun manageCurrentDevicePushToken(
        action: String,
        installationId: String,
        platform: String? = null,
        pushToken: String? = null,
    ): Boolean {
        val accessToken = supabaseClient.auth.currentAccessTokenOrNull()
            ?.trim()
            .takeUnless { it.isNullOrEmpty() }
            ?: run {
                logSupabaseWarning(
                    featureName = FEATURE_NAME,
                    message = "push_token_manage_skipped reason=no_access_token action=$action",
                )
                return false
            }

        val response = httpClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/manage-device-push-token") {
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header("Authorization", "Bearer $accessToken")
            setBody(
                DevicePushTokenSyncRequest(
                    action = action,
                    installationId = installationId,
                    platform = platform,
                    pushToken = pushToken,
                )
            )
        }

        if (response.status != HttpStatusCode.OK) {
            logSupabaseWarning(
                featureName = FEATURE_NAME,
                message = "push_token_manage_failed action=$action status=${response.status.value} body=${response.bodyAsText().take(200)}",
            )
            return false
        }

        return true
    }
}

private const val FEATURE_NAME: String = "PartnerActivity"
private const val ACTION_SYNC: String = "sync"
private const val ACTION_DEACTIVATE: String = "deactivate"
