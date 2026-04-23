package com.getakyra.app.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.gotrue.Auth

// DEVELOPMENT ONLY — hardcoded keys for pilot.
// Before any external deployment, move to local.properties + BuildConfig.
object SupabaseClientProvider {

    private const val SUPABASE_URL = "https://umantyerkqpjxllgdntf.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_ceQUmzK-xIZ2Gx8bxvFSug_wNYcQA57"

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Realtime)
            install(Auth)
        }
    }
}
