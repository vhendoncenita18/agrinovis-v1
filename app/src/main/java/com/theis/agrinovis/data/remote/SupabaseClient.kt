package com.theis.agrinovis.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    // IMPORTANT: Replace with your actual Supabase project credentials
    private const val SUPABASE_URL = "https://lbzzgxznwzdfauhneeql.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_hJftBJk5aXFrVwRtFW3Ysw_DSYHQx9L"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
    }
}
