package kr.jm.weddingledger.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kr.jm.weddingledger.core.config.BuildConfig

const val AUTH_DEEPLINK_SCHEME: String = "weddingledger"
const val AUTH_DEEPLINK_HOST: String = "auth"
const val AUTH_DEEPLINK_REDIRECT_URL: String = "$AUTH_DEEPLINK_SCHEME://$AUTH_DEEPLINK_HOST"

fun createSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = AUTH_DEEPLINK_SCHEME
            host = AUTH_DEEPLINK_HOST
        }
        install(Postgrest)
        install(Realtime)
    }
}
