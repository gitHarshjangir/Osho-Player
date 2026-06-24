package com.oshoplayer.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable

object Supabase {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "YOUR_SUPABASE_URL_HERE",
        supabaseKey = "YOUR_SUPABASE_PUBLISHABLE_KEY_HERE"
    ) {
        install(Postgrest)
    }
}

@Serializable
data class AppAnalytics(
    val osho_key: String,
    val device_id: String,
    val device_name: String
)

@Serializable
data class Subscription(
    val osho_key: String,
    val is_activated: Boolean,
    val current_session_token: String? = null,
    val utr_number: String? = null
)

@Serializable
data class SubscriptionTokenUpdate(
    val current_session_token: String
)
